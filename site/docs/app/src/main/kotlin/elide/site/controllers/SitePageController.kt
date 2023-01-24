package elide.site.controllers

import elide.core.encoding.hex.Hex
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.*
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import elide.site.AppServerProps
import elide.site.Assets
import elide.site.ElideSite
import elide.site.I18nPage
import elide.site.abstract.SitePage
import io.micronaut.cache.annotation.Cacheable
import io.micronaut.cache.interceptor.CacheKeyGenerator
import io.micronaut.core.annotation.AnnotationMetadata
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpResponse
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import reactor.core.publisher.Mono
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.Duration
import java.util.LinkedList
import java.util.Locale
import java.util.concurrent.ConcurrentSkipListMap

/** Extend a [PageController] with access to a [SitePage] configuration. */
@Suppress("unused")
abstract class SitePageController protected constructor(val page: SitePage) : PageWithProps<AppServerProps>(
  AppServerProps.serializer(),
  AppServerProps(page = page.name),
) {
  companion object {
    const val enableSSR = true
    const val enableStreaming = true
  }

  /** Generate a cache key for an HTTP request. */
  internal class CachedResponseKeyGenerator : CacheKeyGenerator {
    /** @inheritDoc */
    override fun generateKey(annotationMetadata: AnnotationMetadata, vararg params: Any): Any {
      val req = params.first() as? HttpRequest<*> ?: error(
        "Cannot generate cache key for non-HTTP request: ${params.first()}"
      )
      return req.uri.toString()
    }
  }

  /** Cached HTTP response. */
  internal data class CachedResponse(
    val uri: String,
    val status: HttpStatus,
    val encoding: Charset,
    val type: MediaType,
    val headers: Map<CharSequence, CharSequence>,
    val length: Long,
    val body: String?,
    val fingerprint: String,
  ) {
    companion object {
      /** Create a cached response from a raw origin response and [body]. */
      @JvmStatic fun from(request: HttpRequest<*>, response: HttpResponse<String>): CachedResponse {
        return CachedResponse(
          request.uri.toString(),
          response.status,
          response.characterEncoding,
          response.contentType.orElse(MediaType.TEXT_HTML_TYPE),
          response.headers.flatMap { it.value.map { value -> it.key to value } }.toMap(),
          response.contentLength,
          response.body(),
          fingerprint(request, response),
        )
      }

      // Acquire a hasher for the provided algorithm.
      @JvmStatic private fun hasher() = MessageDigest.getInstance("SHA-256")

      // Generate a fingerprint for the provided response.
      @JvmStatic private fun fingerprint(req: HttpRequest<*>, res: HttpResponse<String>): String = hasher().apply {
        require(res.status == HttpStatus.OK) { "Cannot fingerprint non-OK response: $res" }
        update(req.uri.toString().toByteArray())
        update(res.status.code.toString().toByteArray())
        update(res.characterEncoding.name().toByteArray())
        update(res.contentType.orElse(MediaType.TEXT_HTML_TYPE).toString().toByteArray())
        update(res.headers.flatMap { it.value.map { value -> it.key to value } }.toMap().toString().toByteArray())
        update(res.contentLength.toString().toByteArray())
        val body = res.body()
        if (body != null) update(body.toByteArray())
      }.let { digest ->
        // encode an E-tag for the provided primed digest
        Hex.encodeToString(digest.digest()).takeLast(8)
      }
    }

    /** @return HTTP response to emit for this cached response. */
    fun response(): MutableHttpResponse<String> = HttpResponse.status<String>(
      status
    ).headers(
      headers
    ).header(
      HttpHeaders.ETAG,
      "\"${fingerprint}\"",
    ).characterEncoding(
      encoding
    ).contentLength(
      length
    ).contentType(
      type
    ).apply {
      if (this@CachedResponse.body != null) {
        body(this@CachedResponse.body)
      }
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false

      other as CachedResponse

      if (status != other.status) return false
      if (uri != other.uri) return false
      if (encoding != other.encoding) return false
      if (type != other.type) return false

      return true
    }

    override fun hashCode(): Int {
      var result = uri.hashCode()
      result = 32 * result + status.hashCode()
      result = 31 * result + encoding.hashCode()
      result = 31 * result + type.hashCode()
      return result
    }

    override fun toString(): String {
      return "CachedResponse(status='$status', uri='$uri', encoding=$encoding, type=$type, headers=$headers)"
    }
  }

  // Logger for all controllers.
  private val logging: Logger = Logging.named("SitePageController")

  // Response cache.
  private val responseCache: ConcurrentSkipListMap<String, CachedResponse> = ConcurrentSkipListMap()

  // Site-level info resolved for the current locale.
  @Inject internal lateinit var siteInfo: ElideSite.SiteInfo

  // Copy a response body and all metadata to a new response object.
  private fun <V, X> copyResponse(
    response: MutableHttpResponse<V>,
    base: HttpResponse<X>,
    getter: (HttpResponse<X>) -> V,
  ): MutableHttpResponse<V> {
    return response.headers(
      base.headers.asMap().flatMap {
        it.value.map { value -> it.key to value }
      }.toMap()
    ).characterEncoding(
      base.characterEncoding
    ).cookies(
      try {
        base.cookies.all
      } catch (thr: Throwable) {
        logging.warn("Failed to copy cookies to response", thr)
        emptySet()
      }
    ).status(
      base.status
    ).contentLength(
      base.contentLength
    ).contentType(
      base.contentType.orElse(MediaType.TEXT_HTML_TYPE)
    ).body(
      getter.invoke(base)
    )
  }

  // Check the cache for `request`, and serve it from the cache if possible.
  @Cacheable("ssrContent", keyGenerator = CachedResponseKeyGenerator::class)
  internal open suspend fun ssrGenerateResponse(
    request: HttpRequest<*>,
    response: MutableHttpResponse<String>,
    block: suspend HTML.() -> Unit
  ): CachedResponse {
    val subResponse = withContext(Dispatchers.IO) {
      val responsePublisher = ssr(
        request.mutate().apply {
          // disable conditional responses when generating an origin response body
          headers.remove(HttpHeaders.IF_NONE_MATCH)
          headers.remove(HttpHeaders.IF_MODIFIED_SINCE)
        },
        block = block,
      )
      responsePublisher.block(
        Duration.ofMinutes(3)
      ) ?: error(
        "Failed to render response for page: '${request.uri}'"
      )
    }

    val dataPublisher = subResponse.body()
    val data = if (dataPublisher != null) {
      withContext(Dispatchers.IO) {
        Mono.from(dataPublisher).block(Duration.ofMinutes(3))
      } ?: error(
        "Failed to render data stream for page: '${request.uri}'"
      )
    } else {
      null
    }

    // consume data into a static buffer
    val body = data?.use {
      String(it.toByteArray(), subResponse.characterEncoding)
    }
    return CachedResponse.from(
      request,
      copyResponse(response, subResponse) {
        body ?: ""
      }
    )
  }

  // Generate a baseline response to fill with content.
  protected open fun baseResponse(): MutableHttpResponse<String> = HttpResponse.ok()

  /** Finalize an HTTP response. */
  protected open fun finalize(request: HttpRequest<*>, locale: Locale, response: MutableHttpResponse<String>) {
    // set `Content-Language` header
    response.header(
      HttpHeaders.CONTENT_LANGUAGE,
      if (!locale.country.isNullOrBlank()) {
        "${locale.language}-${locale.country}"
      } else {
        (locale.language?.ifBlank { "en-US" } ?: "en-US")
      }
    )

    // set `Vary` header
    response.headers[HttpHeaders.VARY] = sortedSetOf(
      HttpHeaders.ACCEPT_LANGUAGE,
      HttpHeaders.ACCEPT_ENCODING,
      HttpHeaders.ACCEPT,
    ).joinToString(", ")

    // add security headers
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers[HttpHeaders.ACCEPT_CH] = "DPR"
    response.headers["Cross-Origin-Embedder-Policy"] = "require-corp"

    response.headers["Permissions-Policy"] = listOf(
     "ch-dpr=(self)",
    ).joinToString(", ")

    response.headers["Content-Security-Policy"] = listOf(
      "default-src 'self'",
      "script-src 'self' https://www.googletagmanager.com",
      "style-src 'self' 'unsafe-inline'",
      "img-src 'self' data: https://www.googletagmanager.com https://www.google-analytics.com",
      "font-src https://fonts.gstatic.com",
      "connect-src 'self' https://www.googletagmanager.com https://www.google-analytics.com",
    ).joinToString("; ")

    // apex caching
    response.headers[HttpHeaders.CACHE_CONTROL] = listOf(
      "public",
      "max-age=60",
      "s-max-age=300",
      "proxy-revalidate",
      "stale-while-revalidate=7200",
    ).joinToString(", ")

    // add `Link` headers
    response.headers.apply {
      add(HttpHeaders.LINK, "<https://fonts.gstatic.com>; rel=preconnect")
      add(HttpHeaders.LINK, "</assets/base.min.css>; rel=preload; as=style")

      val ua = request.headers["sec-ch-ua"] ?: request.headers[HttpHeaders.USER_AGENT] ?: ""
      when {
        ua.contains("Google Chrome") || ua.contains("Chrome") || ua.contains("Chromium") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/s/jetbrainsmono/v13/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxTOlOVk6OThhvA.woff2>; rel=preload; as=font; crossorigin=anonymous; type=font/woff2")
        }
        ua.contains("Safari") -> {
          add(HttpHeaders.LINK, "<https://fonts.gstatic.com/s/jetbrainsmono/v13/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxTOlOTk6OThhvA.woff>; rel=preload; as=font; crossorigin=anonymous; type=font/woff")
        }
      }
    }
  }

  /** Finalize an HTTP response. */
  protected open fun finalizeAsset(locale: Locale, response: StreamedAssetResponse) {
    response.headers["Cross-Origin-Resource-Policy"] = "same-origin"
  }

  // Consult the cache, returning any found result, otherwise populate the cache with an origin execution.
  @Suppress("ReactiveStreamsUnusedPublisher")
  private suspend fun ssrCached(
    locale: Locale,
    request: HttpRequest<*>,
    response: MutableHttpResponse<String> = baseResponse(),
    block: suspend HTML.() -> Unit
  ): Mono<HttpResponse<String>> = mono {
    ssrGenerateResponse(
      request,
      response,
      block,
    ).response().apply {
      finalize(request, locale, this)
    }
  }

  /** @return Rendered page title. */
  protected open fun renderTitle(): String {
    return if (page.title == "Elide") {
      siteInfo.title
    } else {
      "${page.title} | ${siteInfo.title}"
    }
  }

  protected open fun pageHead(): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "icon"
      href = "/images/favicon.png"
      type = "image/png"
    }
    link {
      rel = "icon"
      href = "/images/favicon.svg"
      sizes = "any"
      type = "image/svg+xml"
    }
    meta {
      name = "viewport"
      content = "width=device-width, initial-scale=1.0"
    }
  }

  protected open fun headMetadata(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val canonical = page.canonical(locale)
        val keywords = page.keywords(locale)
        val description = page.description(locale)
        val twitterInfo = page.twitterInfo(locale)
        val ogInfo = page.openGraph(locale)

        link {
          rel = "canonical"
          href = canonical.toString()
        }
        if (description.isNotBlank()) meta {
          name = "description"
          content = description
        }
        if (keywords.isNotEmpty()) meta {
          name = "keywords"
          content = keywords.joinToString(",")
        }
        ogInfo.head(locale).invoke(this)
        twitterInfo.head(locale).invoke(this)
      }
    }
  }

  protected open fun pageBody(): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    if (enableSSR) {
      if (enableStreaming) streamSSR(this@SitePageController, it)
      else injectSSR(this@SitePageController, it)
    }
  }

  @Suppress("unused")
  protected open fun tailMetadata(locale: Locale): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val twitterInfo = page.twitterInfo(locale)
        val ogInfo = page.openGraph(locale)
         val builder = I18nPage.LinkedDataBuilder.create()
        ogInfo.linkedData(locale).invoke(builder)
        twitterInfo.linkedData(locale).invoke(builder)

        script {
          type = "application/json+ld"

          unsafe {
            +(builder.serializeJson())
          }
        }
      }
    }
  }

  protected open fun fonts(): List<String> = emptyList()

  protected open fun preStyles(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "preconnect dns-prefetch"
      href = "https://fonts.gstatic.com"
      attributes["crossorigin"] = "true"
    }
  }

  protected open fun pageStyles(locale: Locale): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    stylesheet(Assets.Styles.base)
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    head: suspend HEAD.(HttpRequest<*>) -> Unit,
    block: suspend BODY.(HttpRequest<*>) -> Unit,
  ): Mono<HttpResponse<String>> {
    // locale selected
    val locale = request.locale.orElse(I18nPage.Defaults.locale)
    return ssrCached(locale, request) {
      // set HTML lang
      lang = if (!locale.country.isNullOrBlank()) {
        "${locale.language}-${locale.country}"
      } else {
        locale.language
      }

      head {
        meta {
          charset = "utf-8"
        }

        preStyles(locale).invoke(this@head, request)
        pageStyles(locale).invoke(this@head, request)
        fonts().forEach {
          link {
            href = it
            rel = "stylesheet"
          }
        }

        title { +renderTitle() }

        script(Assets.Scripts.ui, defer = true)
        script(Assets.Scripts.analytics, async = true)
        head.invoke(this@head, request)
        headMetadata(locale).invoke(this@head, request)
      }
      body {
        block.invoke(this@body, request)
        tailMetadata(locale).invoke(this@body, request)
      }
    }
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    block: suspend BODY.(HttpRequest<*>) -> Unit = pageBody(),
  ) = page(
    request,
    pageHead(),
    block,
  )
}
