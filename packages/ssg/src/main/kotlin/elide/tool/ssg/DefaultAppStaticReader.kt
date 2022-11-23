package elide.tool.ssg

import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tool.ssg.StaticContentReader.ArtifactType
import elide.tool.ssg.StaticContentReader.DetectedArtifact
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import jakarta.inject.Singleton
import org.jsoup.Jsoup
import java.net.URL
import java.nio.ByteBuffer

/** Default content transformation and parsing logic. */
@Singleton internal class DefaultAppStaticReader : StaticContentReader {
  // Private logger.
  private val logging: Logger = Logging.of(DefaultAppStaticReader::class)

  /** @inheritDoc */
  override fun consume(response: HttpResponse<ByteArray>): Pair<Boolean, ByteBuffer> {
    val (body, shouldParse) = if (response.contentLength > 0) {
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
  override fun parse(response: HttpResponse<ByteArray>, content: ByteBuffer): List<DetectedArtifact> {
    require(response.contentLength > 0 && response.body.isPresent) {
      "Cannot parse content from an empty HTTP response body"
    }
    require(response.contentType.orElse(null) == MediaType.TEXT_HTML_TYPE) {
      "Cannot parse content from a non-HTML HTTP response body"
    }
    val body = response.getBody(String::class.java).orElse(null) ?: error(
      "Failed to parse HTML HTTP response body as a string"
    )
    val parsed = try {
      Jsoup.parse(body)
    } catch (err: Throwable) {
      logging.warn("Failed to parse HTML body from response", err)
      return emptyList()
    }

    val media = parsed.select("[src]")
    val imports = parsed.select("link[href]")
    return media.plus(imports).mapNotNull {
      val src = if (it.tagName() == "link") {
        it.attr("abs:href")
      } else {
        it.attr("abs:link")
      }

      val artifactType = when (it.tagName()) {
        "script" -> ArtifactType.SCRIPT
        "img" -> ArtifactType.IMAGE
        "link" -> when (it.attr("rel")) {
          "stylesheet" -> ArtifactType.STYLE
          else -> null
        }
        else -> null
      }
      if (artifactType == null && logging.isEnabled(LogLevel.TRACE)) logging.trace(
        "Skipping ineligible artifact '${it}'"
      )

      // if we were able to resolve an artifact type, return it, mapped to the source URL.
      artifactType?.let { type ->
        type to src
      }
    }.mapNotNull { (type, src) ->
      val parsedUrl = try {
        URL(src)
      } catch (err: Throwable) {
        logging.warn("Failed to parse URL from artifact source '$src'", err)
        return@mapNotNull null
      }
      DetectedArtifact(
        type = type,
        url = parsedUrl,
      )
    }
  }
}
