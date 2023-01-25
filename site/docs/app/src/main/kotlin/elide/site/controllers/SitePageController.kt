package elide.site.controllers

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder as BrotliEncoder
import elide.core.encoding.base64.Base64
import elide.core.encoding.hex.Hex
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.*
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import elide.server.type.RequestState
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
import kotlin.text.StringBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant
import kotlinx.html.*
import kotlinx.html.tagext.body
import kotlinx.html.tagext.head
import reactor.core.publisher.Mono
import tools.elide.data.CompressionMode
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.SortedMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
import java.util.concurrent.atomic.AtomicReference

/** Extend a [PageController] with access to a [SitePage] configuration. */
@Suppress("unused")
abstract class SitePageController protected constructor(val page: SitePage) : PageWithProps<AppServerProps>(
  AppServerProps.serializer(),
  AppServerProps(page = page.name, nonce = currentNonce),
) {
  companion object {
    const val enableSSR = true
    const val enableStreaming = true
    const val securityReportOnly = true

    // Random data generator for generating nonces.
    private val nonceRandom = java.security.SecureRandom()

    // Length, in bytes, of the random nonce to append to each apex response.
    private const val nonceLength: Int = 8 * 2;

    // Current server-run nonce.
    private val currentNonce = generateNonce()

    /**
      * Generate a nonce to be used with Content Security Policy.
      *
      * @return Nonce for a given render cycle.
      */
    @JvmStatic private fun generateNonce(): String {
      val randomBytes = ByteArray(nonceLength)
      nonceRandom.nextBytes(randomBytes)
      return Base64.encode(randomBytes).string
        .replace("=", "")
        .replace("+", "")
    }
  }

  /**
   * Represents materialized render state for a single page-load.
   *
   * @param nonce CSP nonce for this page-load cycle.
   * @param props Server-side render state (React props) to share with the JS VM.
   * @param page Page which is due for render to the client.
   * @param request HTTP request submitted by the client.
   * @param locale Resolved locale for this request.
   */
  data class PageRenderState private constructor (
    private val nonce: AtomicReference<String>,
    val props: AppServerProps?,
    val page: SitePage,
    val request: HttpRequest<*>,
    val locale: Locale,
  ) {
    internal companion object {
      /**
       * Create a page render state from the provided inputs.
       *
       * @param props Server-side render state (React props) to share with the JS VM.
       * @param page Page which is due for render to the client.
       * @param request HTTP request submitted by the client.
       * @param locale Resolved locale for this request.
       * @param nonce On-hand nonce to use. Optional.
       * @return Page render state from the provided inputs.
       */
      @JvmStatic fun from(
        props: AppServerProps?,
        page: SitePage,
        request: HttpRequest<*>,
        locale: Locale,
        nonce: String? = null,
      ): PageRenderState = PageRenderState(
        props = props,
        page = page,
        request = request,
        locale = locale,
        nonce = AtomicReference(nonce ?: currentNonce),
      )
    }

    // Resolve a nonce string; either generate one if we do not have one on hand, or return the value we have on-hand.
    internal fun nonce(): String {
      return nonce.get()
    }

    // Generate a cache key for this page render flow.
    internal fun cacheKey(): String {
      return StringBuilder().apply {
        append(page.name)
        append(locale.language ?: "en")
        if (props != null) {
          append(props.hashCode().toString())
        }
      }.toString()
    }
  }

  /** Generate a cache key for an HTTP request. */
  internal class CachedResponseKeyGenerator : CacheKeyGenerator {
    /** @inheritDoc */
    override fun generateKey(annotationMetadata: AnnotationMetadata, vararg params: Any): Any {
      val req = params.first() as? HttpRequest<*> ?: error(
        "Cannot generate cache key for type (expected `HttpRequest`): ${params.first()}"
      )
      return StringBuilder().apply {
        append(req.uri.path)
        append(req.locale.orElse(I18nPage.Defaults.locale).language ?: "en")
      }.toString()
    }
  }

  /** Compressed response body variant. */
  internal class CompressedResponseBody (
    private val data: ByteArray,
    private val length: Int = data.size,
  ) {
    /** @return Copied bytes for this response. */
    internal fun emit(): ByteArray {
      val copy = ByteArray(length)
      System.arraycopy(data, 0, copy, 0, length)
      return copy
    }
  }

  /** Cached response body. */
  internal class CachedResponseBody private constructor (
    private val raw: ByteArray,
    private val length: Int,
    private val compressed: SortedMap<CompressionMode, CompressedResponseBody>,
  ) {
    companion object {
      // Enabled compression modes for caching.
      private val enabledModes = if (Brotli4jLoader.isAvailable()) {
        Brotli4jLoader.ensureAvailability()
        listOf(
          CompressionMode.BROTLI,
          CompressionMode.GZIP,
        )
      } else listOf(
        CompressionMode.GZIP
      )

      // Brotli configuration.
      private val brotliParams = BrotliEncoder.Parameters()
        .setMode(BrotliEncoder.Mode.TEXT)
        .setQuality(8)
        .setWindow(22)

      /** Perform compression with the provided [mode] on the provided [body]. */
      @JvmStatic private fun compress(mode: CompressionMode, data: ByteArray): CompressedResponseBody? {
        val outbin = ByteArrayOutputStream()
        when (mode) {
          CompressionMode.GZIP -> BestGzip(outbin)
          CompressionMode.BROTLI -> BrotliOutputStream(outbin, brotliParams)
          else -> return null
        }.use {
          it.write(data)
          it.flush()
        }
        return CompressedResponseBody(outbin.toByteArray())
      }

      /** Create a [CachedResponseBody] from the provided [raw] body content. */
      @JvmStatic suspend fun from(raw: ByteArray?): CachedResponseBody? {
        if (raw == null) return null
        val length = raw.size
        val cache = ConcurrentSkipListMap<CompressionMode, CompressedResponseBody>()

        val jobs = enabledModes.map { mode ->
          withContext(Dispatchers.IO) {
            async {
              compress(mode, raw).let {
                if (it != null) cache[mode] = it
              }
            }
          }
        }
        jobs.awaitAll()

        return CachedResponseBody(
          raw,
          length,
          cache,
        )
      }
    }

    /** @return Raw bytes to serve for this response if no clear encoding can be resolved. */
    internal fun raw(): ByteArray {
      val copy = ByteArray(length)
      System.arraycopy(raw, 0, copy, 0, length)
      return copy
    }

    /** @return Indication of whether a body is present for the provided [mode]. */
    internal fun has(mode: CompressionMode): Boolean = compressed.contains(mode)

    /** @return Compressed response body for [mode], or an error is thrown. */
    internal fun serve(mode: CompressionMode): CompressedResponseBody = compressed[mode] ?: error(
      "Failed to locate request body for compression mode '$mode'"
    )
  }

  /** Gzip output stream which uses the [Deflater.BEST_COMPRESSION] ratio possible. */
  private class BestGzip (outbin: OutputStream): GZIPOutputStream(outbin) {
    init {
      def.setLevel(Deflater.BEST_COMPRESSION)
    }
  }

  /** Cached HTTP response. */
  internal data class CachedResponse(
    val state: PageRenderState,
    val nonce: String,
    val uri: String,
    val status: HttpStatus,
    val encoding: Charset,
    val type: MediaType,
    val headers: Map<CharSequence, CharSequence>,
    val length: Long,
    val body: CachedResponseBody?,
    val fingerprint: String,
    val materialized: Instant = Clock.System.now(),
  ) {
    companion object {
      /** Create a cached response from a raw origin response and [body]. */
      @JvmStatic suspend fun from(state: PageRenderState, response: HttpResponse<ByteArray>): CachedResponse {
        return CachedResponse(
          state,
          state.nonce(),
          state.request.uri.toString(),
          response.status,
          response.characterEncoding,
          response.contentType.orElse(MediaType.TEXT_HTML_TYPE),
          response.headers.flatMap { it.value.map { value -> it.key to value } }.toMap(),
          response.contentLength,
          CachedResponseBody.from(response.body()),
          fingerprint(state.request, response),
        )
      }

      // Acquire a hasher for the provided algorithm.
      @JvmStatic private fun hasher() = MessageDigest.getInstance("SHA-256")

      // Generate a fingerprint for the provided response.
      @JvmStatic private fun fingerprint(req: HttpRequest<*>, res: HttpResponse<ByteArray>): String = hasher().apply {
        require(res.status == HttpStatus.OK) { "Cannot fingerprint non-OK response: $res" }
        update(req.uri.toString().toByteArray())
        update(res.status.code.toString().toByteArray())
        update(res.characterEncoding.name().toByteArray())
        update(res.contentType.orElse(MediaType.TEXT_HTML_TYPE).toString().toByteArray())
        update(res.headers.flatMap { it.value.map { value -> it.key to value } }.toMap().toString().toByteArray())
        update(res.contentLength.toString().toByteArray())
        val body = res.body()
        if (body != null) update(body)
      }.let { digest ->
        // encode an E-tag for the provided primed digest
        Hex.encode(digest.digest()).string.takeLast(8)
      }
    }

    // Decide whether a response can be short-circuited with statuses like 304; otherwise, check the cache and determine
    // what compressed response to use (if data is found); if no options succeed, return `null` to synthesize from raw.
    private fun negotiate(req: HttpRequest<*>): MutableHttpResponse<ByteArray>? {
      // negotiation requires a body
      val body = this.body ?: return null

      // check for `If-None-Match` header
      val ifNoneMatch = req.headers.findFirst(HttpHeaders.IF_NONE_MATCH)
      if (ifNoneMatch.isPresent) {
        val nominated = ifNoneMatch.get().replace("\"", "")
        if (nominated.isNotBlank()) {
          if (fingerprint == nominated) {
            return HttpResponse.notModified<ByteArray>().apply {
              header(HttpHeaders.ETAG, "\"$fingerprint\"")
            }
          }
        }
      }

      // check for `If-Modified-Since` header
      val date = req.headers.findDate(HttpHeaders.IF_MODIFIED_SINCE)
      if (date.isPresent) {
        val ref = date.get().toInstant().toKotlinInstant()
        if (ref >= materialized) {
          return HttpResponse.notModified<ByteArray>().apply {
            header(HttpHeaders.ETAG, "\"$fingerprint\"")
          }
        }
      }

      // okay, we can't short-circuit, so we should begin negotiating which kind of body to send.
      val acceptEncoding = req.headers.findFirst(HttpHeaders.ACCEPT_ENCODING).orElse(null)
        ?.split(",")
        ?.map { it.trim().lowercase() }

      return when {
        // if we do not have an `Accept-Encoding` header at all, we should fall back to default behavior.
        acceptEncoding?.isEmpty() != false -> null

        // if `Accept-Encoding` includes `br` and we have a Brotli payload on-hand, we can use a present Brotli variant.
        body.has(CompressionMode.BROTLI) && acceptEncoding.contains("br") -> {
          // use a brotli variant
          synthesize("br" to body.serve(CompressionMode.BROTLI))
        }

        // if `Accept-Encoding` includes `gz` and we have a Gzip payload on-hand, we can use a present Gzip variant.
        body.has(CompressionMode.GZIP) && acceptEncoding.contains("gzip") -> {
          // use a gzip variant
          synthesize("gzip" to body.serve(CompressionMode.GZIP))
        }

        // we do not have a special negotiated variant to serve, so we should just fall back to default behavior.
        else -> null
      }
    }

    // Synthesize a request for a response.
    private fun synthesize(
      compressedBody: Pair<String, CompressedResponseBody>? = null,
    ): MutableHttpResponse<ByteArray> = HttpResponse.status<ByteArray>(
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
      if (status.code != 201 && status.code != 304) {
        if (compressedBody != null) {
          val (encoding, body) = compressedBody
          header(HttpHeaders.CONTENT_ENCODING, encoding)
          body(body.emit())
        } else {
          val body = this@CachedResponse.body?.raw()
          if (body?.isNotEmpty() == true) body(body)
        }
      }
    }

    /** @return HTTP response to emit for this cached response. */
    fun response(req: HttpRequest<*>): MutableHttpResponse<ByteArray> = when (val early = negotiate(req)) {
      null -> synthesize()
      else -> early
    }

    /** @return HTTP response to emit for this cached response. */
    fun response(
      finalizer: MutableHttpResponse<ByteArray>.(PageRenderState) -> Unit
    ): MutableHttpResponse<ByteArray> {
      return response(state.request).apply {
        finalizer(this, state)
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
    getter: (HttpResponse<X>) -> V?,
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
        emptySet()
      }
    ).status(
      base.status
    ).contentLength(
      base.contentLength
    ).contentType(
      base.contentType.orElse(MediaType.TEXT_HTML_TYPE)
    ).apply {
      val body = getter(base)
      if (body != null) body(body)
    }
  }

  // Check the cache for `request`, and serve it from the cache if possible.
  @Cacheable("ssrContent", keyGenerator = CachedResponseKeyGenerator::class)
  internal open suspend fun ssrGenerateResponse(
    request: HttpRequest<*>,
    locale: Locale,
    response: MutableHttpResponse<ByteArray>,
    block: suspend HTML.(PageRenderState) -> Unit
  ): CachedResponse {
    // calculate render state
    val state = PageRenderState.from(
      props = props(RequestState(
        request = request,
        principal = null,
      )),
      request = request,
      locale = locale,
      page = page,
    )

    val subResponse = withContext(Dispatchers.IO) {
      val responsePublisher = ssr(
        request.mutate().apply {
          // disable conditional responses when generating an origin response body
          headers.remove(HttpHeaders.IF_NONE_MATCH)
          headers.remove(HttpHeaders.IF_MODIFIED_SINCE)
        },
      ) {
        block.invoke(this, state)
      }
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
      it.toByteArray()
    }
    return CachedResponse.from(
      state,
      copyResponse(response, subResponse) {
        body
      }
    )
  }

  // Generate a baseline response to fill with content.
  protected open fun baseResponse(): MutableHttpResponse<ByteArray> = HttpResponse.ok()

  // Retrieve the CSP header name that should be used.
  protected fun cspHeader(): String = if (securityReportOnly) {
    "Content-Security-Policy-Report-Only"
  } else {
    "Content-Security-Policy"
  }

  // Retrieve the CORP header name that should be used.
  protected fun corpHeader(): String = if (securityReportOnly) {
    "Cross-Origin-Opener-Policy-Report-Only"
  } else {
    "Cross-Origin-Opener-Policy"
  }

  // Retrieve the COEP header name that should be used.
  protected fun coepHeader(): String = if (securityReportOnly) {
    "Cross-Origin-Embedder-Policy-Report-Only"
  } else {
    "Cross-Origin-Embedder-Policy"
  }

  // Generate a baseline response to fill with content.
  protected open fun csp(state: PageRenderState): List<Pair<String, String>> = listOf(
      "default-src" to "'self'",
      "base-uri" to "'self'",
      "script-src" to "'self' 'nonce-NONCE' 'strict-dynamic' https://www.googletagmanager.com 'unsafe-inline'",
      "style-src" to "'self' 'unsafe-inline'",
      "object-src" to "'none'",
      "img-src" to "'self' data: https://www.googletagmanager.com https://www.google-analytics.com",
      "font-src" to "https://fonts.gstatic.com",
      "require-trusted-types-for" to "'script'",
      "connect-src" to "'self' https://www.googletagmanager.com https://www.google-analytics.com https://analytics.google.com https://stats.g.doubleclick.net",
      "report-uri" to "https://elidefw.report-uri.com/r/d/csp/enforce",
  )

  /** Finalize an HTTP response. */
  protected open fun finalize(state: PageRenderState, response: MutableHttpResponse<ByteArray>) {
    val request = state.request
    val locale = state.locale

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
    response.headers[coepHeader()] = "require-corp"
    response.headers[corpHeader()] = "same-origin"
    response.headers["Referrer-Policy"] = "no-referrer, strict-origin-when-cross-origin"

    response.headers["Permissions-Policy"] = listOf(
     "ch-dpr=(self)",
     "sync-xhr=()",
    ).joinToString(", ")

    response.headers[cspHeader()] = csp(state).map {
      "${it.first} ${it.second}"
    }.joinToString("; ").replace(
      "NONCE",
      state.nonce(),
    )

    // apex caching
    if (response.status.code == 200) response.headers[HttpHeaders.CACHE_CONTROL] = listOf(
      "public",
      "max-age=60",
      "s-max-age=300",
      "proxy-revalidate",
      "stale-while-revalidate=7200",
    ).joinToString(", ")

    // add `Link` headers
    if (response.status.code == 200) response.headers.apply {
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
    response.headers["X-Content-Type-Options"] = "nosniff"
    response.headers["X-Frame-Options"] = "DENY"
    response.headers["X-XSS-Protection"] = "1; mode=block"
    response.headers["Cross-Origin-Resource-Policy"] = "same-origin"
    response.headers["Cache-Control"] = (
      "public, max-age=900, s-max-age=3600, proxy-revalidate, stale-while-revalidate=7200"
    )
  }

  // Consult the cache, returning any found result, otherwise populate the cache with an origin execution.
  @Suppress("ReactiveStreamsUnusedPublisher")
  private suspend fun ssrCached(
    request: HttpRequest<*>,
    locale: Locale,
    response: MutableHttpResponse<ByteArray> = baseResponse(),
    block: suspend HTML.(PageRenderState) -> Unit
  ): Mono<HttpResponse<ByteArray>> = mono {
    ssrGenerateResponse(
      request,
      locale,
      response,
      block,
    ).response { state: PageRenderState ->
      finalize(state, this)
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
  }

  protected open fun headMetadata(state: PageRenderState): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val canonical = page.canonical(state.locale)
        val keywords = page.keywords(state.locale)
        val description = page.description(state.locale)
        val twitterInfo = page.twitterInfo(state.locale)
        val ogInfo = page.openGraph(state.locale)

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
        ogInfo.head(state.locale).invoke(this)
        twitterInfo.head(state.locale).invoke(this)
      }
    }
  }

  protected fun linkedRef(ref: String): Map<String, String> {
    return mapOf("@id" to ref)
  }

  protected open fun breadcrumb(state: PageRenderState): List<String> = if (page.parent != null) {
    listOf(
      page.parent!!.title,
      page.title,
    )
  } else {
    listOf(page.title)
  }

  protected open fun pageLinkedData(
    state: PageRenderState,
    builder: I18nPage.LinkedDataBuilder
  ) {
    // add `Organization` stanza
    builder.stanzaOf("Organization") {
      put("name", "Elide")
      put("identifier", "https://elide.dev")
      put("url", "https://elide.dev")
      put("logo", "https://elide.dev/images/favicon.png")
      put("sameAs", listOf(
        "https://github.com/elide-dev",
        "https://github.com/elide-dev/elide",
        "https://twitter.com/elide_dev",
      ))

      put("description", """
        Elide is a polyglot app framework and runtime which makes it easy to develop apps in your favorite languages,
        like JavaScript and Kotlin.
      """.trimIndent().replace("\n", " "))
    }

    // add `SoftwareApplication` stanza for the Elide runtime
    builder.stanzaOf("SoftwareApplication") {
      put("name", "Elide Runtime")
      put("identifier", "https://github.com/elide-dev/elide")
      put("url", "https://github.com/elide-dev/elide")
      put("license", "https://github.com/elide-dev/elide/blob/v3/LICENSE")
      put("isAccessibleForFree", true)
      put("headline", "It's like Node, but it does a lot more than JavaScript")
      put("applicationCategory", "Developer Tools")
      put("operatingSystem", "macOS, Linux")
      put("processorRequirements", "amd64, arm64")
      put("applicationSuite", "Elide")
      put("releaseNotes", "https://github.com/elide-dev/cli/releases")
      put("installUrl", "https://elide.dev/getting-started#install")
    }

    // add `SoftwareSourceCode` stanza for the Elide project (framework)
    builder.stanzaOf("SoftwareSourceCode") {
      put("name", "Elide Project")
      put("identifier", "https://github.com/elide-dev/elide")
      put("url", "https://github.com/elide-dev/elide")
      put("license", "https://github.com/elide-dev/elide/blob/v3/LICENSE")
      put("isAccessibleForFree", true)
      put("headline", "App framework and runtime designed for a polyglot future")
      put("discussionUrl", "https://github.com/orgs/elide-dev/discussions")
      put("codeSampleType", "code snippet")
      put("codeRepository", "https://github.com/elide-dev/elide")
      put("runtimePlatform", "Elide")
      put("programmingLanguage", listOf(
        mapOf(
          "@type" to "ComputerLanguage",
          "name" to "Kotlin",
          "url" to "https://kotlinlang.org",
        ),
        mapOf(
          "@type" to "ComputerLanguage",
          "name" to "JavaScript",
        ),
      ))
    }

    // add `WebSite` stanza for the whole site
    builder.stanzaOf("WebSite") {
      put("name", "Elide")
      put("url", "https://elide.dev")
    }

    val i18npage = page as? I18nPage

    // add `WebPage` stanza for the page itself
    builder.stanzaOf("WebPage") {
      put("name", "Elide")
      if (i18npage != null) {
        put("title", i18npage.pageTitle(state.locale))
        put("description", i18npage.description(state.locale))
        put("url", i18npage.canonical(state.locale))
      }
    }

    // add `BreadcrumbList` for page
    builder.stanzaOf("BreadcrumbList") {
      val portions = ArrayList<Map<String, Any>>(2)

      if (page.parent != null) {
        portions.add(mapOf(
          "@type" to "ListItem",
          "position" to 1,
        ))
        portions.add(mapOf(
          "@type" to "ListItem",
          "position" to 2,
        ))
      } else {
        portions.add(mapOf(
          "@type" to "ListItem",
          "position" to 1,
        ))
      }

      put("itemListElement", portions)
    }
  }

  protected open fun pageBody(): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    if (enableSSR) {
      if (enableStreaming) streamSSR(this@SitePageController, it)
      else injectSSR(this@SitePageController, it)
    }
  }

  @Suppress("unused")
  protected open fun tailMetadata(state: PageRenderState): suspend BODY.(request: HttpRequest<*>) -> Unit = {
    when (page) {
      // if the page is an `I18nPage` subsidiary, it can be interrogated for SEO information to embed in the page.
      is I18nPage -> {
        val twitterInfo = page.twitterInfo(state.locale)
        val ogInfo = page.openGraph(state.locale)
         val builder = I18nPage.LinkedDataBuilder.create()
        ogInfo.linkedData(state.locale).invoke(builder)
        twitterInfo.linkedData(state.locale).invoke(builder)
        pageLinkedData(state, builder)

        script {
          nonce = state.nonce()
          type = "application/ld+json"

          unsafe {
            +(builder.serializeJson())
          }
        }
      }
    }
  }

  protected open fun fonts(): List<String> = emptyList()

  protected open fun preStyles(state: PageRenderState): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    link {
      rel = "preconnect dns-prefetch"
      href = "https://fonts.gstatic.com"
      attributes["crossorigin"] = "true"
    }
  }

  protected open fun pageStyles(state: PageRenderState): suspend HEAD.(request: HttpRequest<*>) -> Unit = {
    stylesheet(Assets.Styles.base)
    stylesheet(Assets.Styles.home)
  }

  protected open suspend fun page(
    request: HttpRequest<*>,
    head: suspend HEAD.(HttpRequest<*>) -> Unit,
    block: suspend BODY.(HttpRequest<*>) -> Unit,
  ): Mono<HttpResponse<ByteArray>> {
    // locale selected
    val locale = request.locale.orElse(I18nPage.Defaults.locale)
    return ssrCached(request, locale) { state ->
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

        // pre-styles, page styles
        preStyles(state).invoke(this@head, request)
        pageStyles(state).invoke(this@head, request)

        // extra fonts, if any
        fonts().forEach {
          link {
            href = it
            rel = "stylesheet"
          }
        }

        // page title
        title { +renderTitle() }

        // UI and analytics scripts
        script {
          type = "text/javascript"
          src = Assets.Scripts.ui
          defer = true
          nonce = state.nonce()
        }
        script {
          type = "text/javascript"
          src = Assets.Scripts.analytics
          defer = true
          async = true
          nonce = state.nonce()
        }

        // extra head content
        head.invoke(this@head, request)

        // head SEO
        headMetadata(state).invoke(this@head, request)
      }

      body {
        // body content
        block.invoke(this@body, request)

        // body SEO
        tailMetadata(state).invoke(this@body, request)
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
