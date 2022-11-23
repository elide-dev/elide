package elide.tool.ssg

import elide.tool.ssg.cfg.ElideSSGCompiler.ELIDE_TOOL_VERSION
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import jakarta.inject.Singleton
import tools.elide.meta.Endpoint
import java.net.URI

/** Default request factory implementation. */
@Suppress("unused")
@Singleton public class DefaultRequestFactory : RequestFactory {
  private companion object {
    // Synthesized hostname.
    const val ssgSynthesizedHost: String = "elide-ssg.local"

    // Default user agent string to use.
    const val defaultUserAgent: String = (
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_0) " +
      "AppleWebKit/537.36 (KHTML, like Gecko) " +
      "Chrome/105.0.0.0 " +
      "Safari/535.00 " +
      "Elide/SSGCompiler/${ELIDE_TOOL_VERSION}"
    )

    // Default headers to include on requests.
    val defaultHeaders: List<Pair<String, String>> = listOf(
      HttpHeaders.ACCEPT to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*",
      HttpHeaders.ACCEPT_ENCODING to "identity",
      HttpHeaders.ACCEPT_LANGUAGE to "en-US,en;q=0.9",
      HttpHeaders.CACHE_CONTROL to "max-age=0",
      HttpHeaders.CONNECTION to "keep-alive",
      HttpHeaders.HOST to ssgSynthesizedHost,
      HttpHeaders.USER_AGENT to defaultUserAgent,
    )
  }

  /** @inheritDoc */
  override fun create(page: Endpoint, controller: Class<*>?): HttpRequest<*> {
    val urlBase = page.base
    val urlPath = page.tail
    val url = if (page.tail.startsWith("/")) {
      if (page.base == "/") {
        urlPath
      } else {
        "$urlBase$urlPath"
      }
    } else {
      "$urlBase/$urlPath"
    }
    val urlTarget = try {
      URI.create(url)
    } catch (err: Throwable) {
      throw RequestFactory.RequestGenerationFailed(
        page,
        err,
      )
    }

    return HttpRequest.GET<String>(urlTarget).headers {
      defaultHeaders.forEach { (key, value) ->
        it.add(key, value)
      }
    }
  }
}
