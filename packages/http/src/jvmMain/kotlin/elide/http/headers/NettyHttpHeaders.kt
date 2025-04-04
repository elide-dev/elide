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

import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpHeaders
import elide.http.Header
import elide.http.HeaderName
import elide.http.HeaderValue
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.MutableHeaders
import elide.http.asHeaderValue

// Implements platform-specific HTTP headers through Netty.
@JvmInline internal value class NettyHttpHeaders(private val data: HttpHeaders) : PlatformHttpHeaders<HttpHeaders> {
  override val headers: HttpHeaders get() = data
  override val size: UInt get() = data.entries().size.toUInt()
  override val sizeDistinct: UInt get() = data.names().size.toUInt()
  override fun contains(header: HeaderName): Boolean = headers.contains(header.name)
  override fun contains(name: HttpHeaderName): Boolean = headers.contains(name)
  override fun get(header: HeaderName): HeaderValue? = headers.asHeaderValue(header.name)
  override fun get(name: HttpHeaderName): HeaderValue? = headers.asHeaderValue(name)
  override fun first(header: HeaderName): HttpHeaderValue? = headers.asHeaderValue(header.name)?.values?.firstOrNull()
  override fun first(name: HttpHeaderName): HttpHeaderValue? = headers.asHeaderValue(name)?.values?.firstOrNull()
  override fun toMutable(): MutableHeaders = NettyMutableHttpHeaders(data)

  // Convert a sequence of Netty header names to Header objects, preserving order.
  private fun Sequence<String>.mapToHeaders() = map {
    it to data.getAllAsString(it)
  }.mapNotNull { (key, values) ->
    when (values.size) {
      0 -> null
      1 -> HeaderValue.single(values.first())
      else -> HeaderValue.multi(values)
    }?.let {
      Header.of(HeaderName.of(key), it)
    }
  }

  override fun asSequence(): Sequence<Header> = data.names()
    .asSequence()
    .mapToHeaders()

  override fun asOrdered(): Sequence<Header> = data.iteratorAsString()
    .asSequence()
    .map { it.key }
    .distinct()
    .mapToHeaders()

  override fun asMap(): Map<HeaderName, HeaderValue> = asSequence().map {
    it.name to it.value
  }.toMap()

  override fun asRawMap(): Map<HttpHeaderName, List<HttpHeaderValue>> = asSequence().map {
    it.name.name to it.value.values.toList()
  }.toMap()

  companion object {
    /** Empty header container singleton. */
    @JvmStatic internal val EMPTY: NettyHttpHeaders = NettyHttpHeaders(DefaultHttpHeaders())
  }
}
