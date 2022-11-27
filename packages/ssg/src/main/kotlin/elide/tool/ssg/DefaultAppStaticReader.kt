package elide.tool.ssg

import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.ssg.StaticContentReader.ArtifactType
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import jakarta.inject.Singleton
import org.jsoup.Jsoup
import java.net.URL
import java.nio.ByteBuffer

/** Default content transformation and parsing logic. */
@Singleton internal class DefaultAppStaticReader : StaticContentReader {
  private companion object {
    // Link tag `rel` types to make eligible for downloading.
    private val linkTagRelationTypes = sortedSetOf(
      "stylesheet",
    )
  }

  // Private logger.
  private val logging: Logger = Logging.of(DefaultAppStaticReader::class)

  /** @inheritDoc */
  override fun consume(response: HttpResponse<ByteArray>): Pair<Boolean, ByteBuffer> {
    val (body, shouldParse) = if (
      response.status == HttpStatus.OK &&
      response.contentLength > 0 &&
      response.body.isPresent
    ) {
      // there must be body data present, and a content type of `text/html` in order for content to be eligible for
      // parsing to discover additional URLs.
      ByteBuffer.wrap(response.body()) to (response.contentType.orElse(null) == MediaType.TEXT_HTML_TYPE)
    } else {
      ByteBuffer.allocate(0) to false
    }
    if (logging.isEnabled(LogLevel.TRACE))
      logging.trace("Consumed raw binary HTTP response body of length '${response.contentLength}'")
    return shouldParse to body
  }

  /** @inheritDoc */
  override fun parse(
    request: HttpRequest<*>,
    response: HttpResponse<ByteArray>,
    content: ByteBuffer,
  ): List<DetectedArtifact> {
    require(response.status == HttpStatus.OK) {
      "Cannot parse non-OK response body"
    }
    require(response.contentLength > 0 && response.body.isPresent) {
      "Cannot parse content from an empty HTTP response body"
    }
    require(response.contentType.orElse(null) == MediaType.TEXT_HTML_TYPE) {
      "Cannot parse content from a non-HTML HTTP response body"
    }
    val bodyBytes = response.body.orElse(null) ?: error(
      "Failed to parse HTML HTTP response body as a string"
    )

    // parse string into HTML
    val parsed = bodyBytes.inputStream().buffered().use { buf ->
      Jsoup.parse(
        buf,
        null,  // parse `http-equiv` or fall back to UTF-8
        request.uri.toString(),
      )
    }
    val media = parsed.select("[src]")
    val imports = parsed.select("link[href]")
    return media.plus(imports).mapNotNull {
      val (src, _) = if (it.tagName() == "link") {
        val relation = it.attr("rel").lowercase().trim()
        if (relation == "" || linkTagRelationTypes.contains(relation)) {
          it.attr("href") to relation
        } else {
          if (logging.isEnabled(LogLevel.TRACE))
            logging.trace("Tag <link> skipped because rel '$relation' is not eligible for download.")
          return@mapNotNull null
        }
      } else {
        it.attr("src") to ""
      }
      val artifactType = when (it.tagName()) {
        "script" -> ArtifactType.SCRIPT
        "img" -> ArtifactType.IMAGE
        "link" -> ArtifactType.STYLE
        else -> null
      }
      if ((artifactType == null || src.isNullOrBlank()) && logging.isEnabled(LogLevel.TRACE)) logging.trace(
        "Skipping ineligible artifact '${it}'"
      )
      if (src.isNullOrBlank() || src == "/" || src == "." || src == "./" || src == request.uri.toString()) {
        // special case: don't allow detection of the root page as an artifact, which can happen with standards-breaking
        // HTML that is missing a `src` for a media element, in some cases.
        return@mapNotNull null
      }

      // detect relative URLs
      val resolvedUrl: URL = if (src.startsWith("http:") || src.startsWith("https:")) {
        URL(src)  // it's an absolute URL
      } else if (src.startsWith("://")) {
        // special case: it's a protocol-relative URL. these are rare and simply reference the origin URL protocol
        // instead of an explicit protocol.
        URL("${request.uri.scheme}://${src.drop("://".length)}")
      } else {
        // if it starts with `/`, it's a reference from the origin base, i.e. an origin-relative URL. we should
        // initialize the URL with the origin base but `src` as the path.
        if (src.startsWith("/")) {
          URL(
            request.uri.scheme,
            request.uri.host,
            request.uri.port,
            src,
          )
        } else {
          // otherwise, it's a completely relative link, so we need the original URL to calculate it.
          request.uri.resolve(src).toURL()
        }
      }

      // if we were able to resolve an artifact type, return it, mapped to the source URL.
      artifactType?.let { type ->
        type to resolvedUrl
      }
    }.map { (type, src) ->
      DetectedArtifact(
        type = type,
        request = request,
        url = src,
      )
    }
  }
}
