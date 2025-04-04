/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.http.headers

import java.net.http.HttpHeaders
import elide.http.Header
import elide.http.HeaderName
import elide.http.HeaderValue
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.MutableHeaders
import elide.http.alwaysTruePredicate
import elide.http.asHeaderValue

// Implements platform-specific HTTP headers via `java.net.http`.
@JvmInline internal value class JavaNetHttpHeaders(private val data: HttpHeaders) : PlatformHttpHeaders<HttpHeaders> {
  override val headers: HttpHeaders get() = data
  override val size: UInt get() = data.map().values.sumOf { it.size }.toUInt()
  override val sizeDistinct: UInt get() = data.map().keys.size.toUInt()
  override fun contains(header: HeaderName): Boolean = data.map().containsKey(header.name)
  override fun contains(name: HttpHeaderName): Boolean = data.map().containsKey(name)
  override fun get(header: HeaderName): HeaderValue? = data.asHeaderValue(header.name)
  override fun get(name: HttpHeaderName): HeaderValue? = data.asHeaderValue(name)
  override fun first(header: HeaderName): HttpHeaderValue? = data.firstValue(header.name).orElse(null)
  override fun first(name: HttpHeaderName): HttpHeaderValue? = data.firstValue(name).orElse(null)
  override fun asRawMap(): Map<HttpHeaderName, List<HttpHeaderValue>> = data.map()
  override fun asOrdered(): Sequence<Header> = asSequence()
  override fun toMutable(): MutableHeaders = JavaNetMutableHttpHeaders(
    HttpHeaders.of(data.map(), alwaysTruePredicate)
  )

  override fun asSequence(): Sequence<Header> = data.map().iterator().asSequence().map {
    when (it.value.size) {
      1 -> HeaderValue.single(it.value.first())
      else -> HeaderValue.multi(it.value)
    }.let { value ->
      Header.of(it.key, value)
    }
  }

  override fun asMap(): Map<HeaderName, HeaderValue> = asSequence().map {
    it.name to it.value
  }.toMap()

  /** Factories for creating or obtaining instances of [JavaNetHttpHeaders]. */
  companion object {
    /** Empty headers. */
    internal val EMPTY: JavaNetHttpHeaders = JavaNetHttpHeaders(
      HttpHeaders.of(emptyMap(), alwaysTruePredicate)
    )
  }
}
