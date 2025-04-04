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

import io.micronaut.http.HttpHeaders
import io.micronaut.http.netty.NettyHttpHeaders
import elide.http.Header
import elide.http.HeaderName
import elide.http.HeaderValue
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.MutableHeaders
import elide.http.asHeaderValue

// Implements HTTP headers via Micronaut.
@JvmInline internal value class MicronautHttpHeaders internal constructor (
  private val backing: HttpHeaders,
) : PlatformHttpHeaders<HttpHeaders> {
  override val headers: HttpHeaders get() = backing
  override fun contains(name: HttpHeaderName): Boolean = backing.contains(name)
  override fun contains(header: HeaderName): Boolean = backing.contains(header.nameNormalized)
  override val size: UInt get() = backing.values().sumOf { it.size }.toUInt()
  override val sizeDistinct: UInt get() = backing.distinctBy { it.key }.size.toUInt()
  override fun get(name: HttpHeaderName): HeaderValue? = backing.asHeaderValue(name)
  override fun get(header: HeaderName): HeaderValue? = backing.asHeaderValue(header.nameNormalized)
  override fun first(header: HeaderName): HttpHeaderValue? = backing.get(header.nameNormalized)
  override fun first(name: HttpHeaderName): HttpHeaderValue? = backing.get(name)
  override fun asOrdered(): Sequence<Header> = asSequence() // TODO(sgammon): guarantee order

  override fun asSequence(): Sequence<Header> = backing.asSequence().map {
    Header.of(it.key, when (it.value.size) {
      1 -> HeaderValue.single(it.value.first())
      else -> HeaderValue.multi(it.value)
    })
  }

  override fun asMap(): Map<HeaderName, HeaderValue> = asSequence().map { header ->
    header.name to header.value
  }.toMap()

  override fun asRawMap(): Map<HttpHeaderName, List<HttpHeaderValue>> = asSequence().map { header ->
    header.name.name to header.value.values.toList()
  }.toMap()

  override fun toMutable(): MutableHeaders = MicronautMutableHttpHeaders(NettyHttpHeaders().apply {
    backing.forEach { pair ->
      pair.value.forEach {
        add(pair.key, it)
      }
    }
  })

  companion object {
    // Empty headers instance.
    internal val EMPTY: MicronautHttpHeaders = MicronautHttpHeaders(NettyHttpHeaders())
  }
}
