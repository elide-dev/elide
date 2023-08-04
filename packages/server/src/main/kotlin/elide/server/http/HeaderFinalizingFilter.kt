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

package elide.server.http

import com.google.common.annotations.VisibleForTesting
import io.micronaut.http.*
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.netty.NettyMutableHttpResponse
import org.reactivestreams.Publisher
import reactor.core.publisher.Mono
import java.util.*

/**
 * Provides an [HttpServerFilter] which cleans response headers by de-duplicating certain values, ensuring consistent
 * casing, and applying settings specified by the developer within static configuration.
 *
 * The header finalizing filter does not touch headers except ones which are registered on a local allow-list.
 */
@Filter("/**") public class HeaderFinalizingFilter : HttpServerFilter {
  private companion object {
    // Whether to enforce consistent header casing.
    private const val consistentCasing = true

    // Whether to force lowercase header names, even for HTTP/1.1.
    private const val forceLowercase = true

    // Canonical header names mapped to their lowercase equivalents.
    private val canonicalHeaders = sortedMapOf(
      "connection" to "Connection",
      "content-encoding" to "Content-Encoding",
      "content-length" to "Content-Length",
      "content-type" to "Content-Type",
      "date" to "Date",
    )

    // Header names which should be de-duplicated.
    private val deduped = sortedSetOf(
      HttpHeaders.DATE.lowercase(),
      HttpHeaders.EXPIRES.lowercase(),
    )
  }

  /**
   * Given a [response] which has concluded server-side processing, finalize the response headers by applying consistent
   * casing, de-duplication, and any other transformations stipulated by app configuration.
   *
   * @param baseResponse HTTP response which was produced by the server.
   * @return Original HTTP response, but with finalized headers.
   */
  @VisibleForTesting internal fun finalizeHeaders(baseResponse:  MutableHttpResponse<*>): MutableHttpResponse<*> {
    // sort current headers
    val response = baseResponse as? NettyMutableHttpResponse<*> ?: return baseResponse
    val sortedHeaders = TreeSet<String>()
    val notableValues = TreeMap<String, String>()

    response.headers.mapNotNull { entry ->
      val header = entry.key.lowercase().trim()
      if (header.isBlank()) return@mapNotNull null
      header to entry
    }.flatMap {
      val (header, entry) = it
      entry.value.map { headerValue ->
        header to headerValue
      }
    }.forEach {
      val (header, value) = it
      val shouldBeDeduped = deduped.contains(header)
      val alreadySeen = sortedHeaders.contains(header)

      if (shouldBeDeduped && alreadySeen) {
        // this header is eligible for de-duplication, and we've already seen a value.
        val previousValue = notableValues[header]
        if (previousValue == value) {
          // the value is duplicated as well as the header, so we can safely skip it.
          response.headers.remove(header)
        }
      } else if (shouldBeDeduped) {
        // this header is eligible for de-duplication, but we haven't seen it yet. we should register it with the set of
        // final headers, and also register it with the map of notable values.
        notableValues[header] = value
      }
      if (forceLowercase || (consistentCasing && response.nettyHttpVersion.majorVersion() > 1)) {
        val valueset = response.headers.getAll(header)
        response.headers.remove(header)
        valueset.forEach { headerValue ->
          response.headers.add(header.lowercase(), headerValue)
        }
      } else if (response.nettyHttpVersion.majorVersion() == 1 && response.nettyHttpVersion.minorVersion() == 1) {
        // HTTP 1.1 should use non-lowercase headers
        val normalized = canonicalHeaders[header]
        if (normalized != null) {
          val valueset = response.headers.getAll(header)
          response.headers.remove(header)
          valueset.forEach { headerValue ->
            response.headers.add(normalized, headerValue)
          }
        }
      }
    }
    return response
  }

  /** @inheritDoc */
  override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
    return Mono.from(chain.proceed(request)).doOnNext { mutableResponse ->
      finalizeHeaders(mutableResponse)
    }
  }
}
