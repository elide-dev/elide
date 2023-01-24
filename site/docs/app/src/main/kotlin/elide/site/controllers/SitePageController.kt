package elide.site.controllers

import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.*
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import elide.site.AppServerProps
import elide.site.Assets
import elide.site.ElideSite
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
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import reactor.core.publisher.Mono
import java.nio.charset.Charset
import java.time.Duration
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
        )
      }
    }

    /** @return HTTP response to emit for this cached response. */
    fun response(): HttpResponse<String> = HttpResponse.status<String>(
      status
    ).headers(
      headers
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
  private fun baseResponse(): MutableHttpResponse<String> = HttpResponse.ok()

  // Consult the cache, returning any found result, otherwise populate the cache with an origin execution.
  private suspend fun ssrCached(
    request: HttpRequest<*>,
    response: MutableHttpResponse<String> = baseResponse(),
    block: suspend HTML.() -> Unit
  ): HttpResponse<String> = ssrGenerateResponse(
    request,
    response,
    block,
  ).response()

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
  }

  protected open fun pageBody(): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    if (enableSSR) {
      if (enableStreaming) streamSSR(this@SitePageController, it)
      else injectSSR(this@SitePageController, it)
    }
  }

  protected open fun fonts(): List<String> = listOf(
    "https://fonts.googleapis.com/css2?family=JetBrains+Mono&display=swap",
  )

  protected open fun preStyles(): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "preconnect"
      href = "https://fonts.googleapis.com"
    }
    link {
      rel = "preconnect"
      href = "https://fonts.gstatic.com"
      attributes["crossorigin"] = "true"
    }
  }

  protected open fun pageStyles(): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    preStyles().invoke(this, it)
    fonts().forEach {
      link {
        href = it
        rel = "stylesheet"
      }
    }
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    head: suspend HEAD.(HttpRequest<*>) -> Unit,
    block: suspend BODY.(HttpRequest<*>) -> Unit,
  ) = ssrCached(request) {
    head {
      title { +renderTitle() }
      preStyles().invoke(this@head, request)

      stylesheet(Assets.Styles.base)
      link {
        href = "/assets/home.min.css"
        rel = "stylesheet"
      }

      pageStyles().invoke(this@head, request)
      script(Assets.Scripts.ui, defer = true)
      script(Assets.Scripts.analytics, async = true)
      head.invoke(this@head, request)
    }
    body {
      block.invoke(this@body, request)
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
