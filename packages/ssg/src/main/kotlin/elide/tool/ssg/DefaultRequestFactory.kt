/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.tool.ssg

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import tools.elide.meta.Endpoint
import java.net.URI
import java.net.URL
import jakarta.inject.Singleton
import elide.tool.ssg.cfg.ElideSSGCompiler.ELIDE_TOOL_VERSION

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

  private fun affixHeaders(req: MutableHttpRequest<*>): MutableHttpRequest<*> {
    return req.headers {
      defaultHeaders.forEach { (key, value) ->
        it.add(key, value)
      }
    }
  }

  /** @inheritDoc */
  override fun create(page: Endpoint, controller: Class<*>?): HttpRequest<*> {
    val urlBase = page.base
    val urlPath = page.tail

    // special case: if the URL evaluates to "/" or empty, then it is considered a request for the root page.
    val resolvedUrl = if (
      (urlBase == "" && urlPath == "/") ||
      (urlBase == "" && urlPath == "") ||
      (urlBase == "/" && urlPath == "/") ||
      (urlBase == "/" && urlPath == "")
    ) {
      "/"
    } else {
      if (page.tail.startsWith("/")) {
        if (page.base == "/") {
          urlPath
        } else {
          "$urlBase$urlPath"
        }
      } else {
        "$urlBase/$urlPath"
      }
    }

    return affixHeaders(HttpRequest.GET<String>(resolvedUrl))
  }

  /** @inheritDoc */
  override fun create(spec: StaticFragmentSpec, artifact: DetectedArtifact): HttpRequest<*> {
    return affixHeaders(HttpRequest.GET<String>(
      artifact.url.toURI()
    ))
  }

  override fun create(base: URL, path: String): HttpRequest<*> {
    return affixHeaders(HttpRequest.GET<String>(URI(
      base.protocol,
      null,
      base.host,
      base.port,
      path,
      null,
      null,
    )))
  }
}
