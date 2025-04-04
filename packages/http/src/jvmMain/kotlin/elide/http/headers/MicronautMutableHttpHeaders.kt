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

import io.micronaut.http.MutableHttpHeaders
import io.micronaut.http.netty.NettyHttpHeaders
import elide.http.Header
import elide.http.HeaderName
import elide.http.HeaderValue
import elide.http.Headers
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.asHeaderValue

private fun String.normalized(): String = trim().lowercase()

// Implements HTTP headers via Micronaut.
@JvmInline internal value class MicronautMutableHttpHeaders(private val backing: MutableHttpHeaders, )
  : PlatformMutableHttpHeaders<MutableHttpHeaders> {
  override val headers: MutableHttpHeaders get() = backing
  override fun contains(name: HttpHeaderName): Boolean = backing.contains(name) || backing.contains(name.normalized())
  override fun contains(header: HeaderName): Boolean = backing.contains(header.nameNormalized)
  override val size: UInt get() = backing.values().sumOf { it.size }.toUInt()
  override val sizeDistinct: UInt get() = backing.distinctBy { it.key }.size.toUInt()
  override fun get(name: HttpHeaderName): HeaderValue? = backing.asHeaderValue(name)
  override fun get(header: HeaderName): HeaderValue? = backing.asHeaderValue(header.nameNormalized)
  override fun first(header: HeaderName): HttpHeaderValue? = backing.get(header.nameNormalized)
  override fun first(name: HttpHeaderName): HttpHeaderValue? = backing.get(name)
  override fun build(): Headers = MicronautHttpHeaders(backing)
  override fun asOrdered(): Sequence<Header> = asSequence() // TODO(sgammon): guarantee order
  override fun remove(pair: Header): Unit = remove(pair.name, pair.value)

  override fun set(pair: Header) {
    when (val value = pair.value) {
      is HeaderValue.SingleHeaderValue -> backing.set(pair.name.nameNormalized, value.single)
      else -> {
        backing.set(pair.name.nameNormalized, value.values.first())
        value.values.forEachIndexed { index, it ->
          if (index == 0) return@forEachIndexed
          backing.add(pair.name.nameNormalized, it)
        }
      }
    }
  }

  override fun set(key: HeaderName, value: HeaderValue) {
    when (val headerValue = value) {
      is HeaderValue.SingleHeaderValue -> backing.set(key.nameNormalized, headerValue.single)
      else -> {
        backing.remove(key.nameNormalized)
        headerValue.values.forEach {
          backing.add(key.nameNormalized, it)
        }
      }
    }
  }

  override fun set(key: HttpHeaderName, value: HttpHeaderValue) {
    backing.set(key, value)
  }

  override fun append(key: HttpHeaderName, value: HttpHeaderValue) {
    backing.add(key, value)
  }

  override fun append(pair: Header) {
    pair.value.values.forEach {
      backing.add(pair.name.nameNormalized, it)
    }
  }

  override fun remove(header: HeaderName) {
    backing.remove(header.nameNormalized)
  }

  override fun remove(name: HttpHeaderName) {
    backing.remove(name)
  }

  override fun remove(key: HeaderName, value: HeaderValue) {
    val all = backing.getAll(key.nameNormalized)
    backing.remove(key.nameNormalized)
    val match = value.values.toSortedSet()
    all.forEach {
      if (it !in match) backing.add(key.nameNormalized, it)
    }
  }

  override fun remove(key: HttpHeaderName, value: HttpHeaderValue) {
    val all = backing.getAll(key)
    backing.remove(key)
    all.forEach {
      if (it != value) backing.add(key, it)
    }
  }

  override fun asSequence(): Sequence<Header> = backing.asSequence().map {
    Header.of(it.key, when (it.value.size) {
      1 -> HeaderValue.single(it.value.first())
      else -> HeaderValue.multi(it.value)
    })
  }

  override fun asMap(): MutableMap<HeaderName, HeaderValue> = asSequence().map { header ->
    header.name to header.value
  }.toMap().toMutableMap()

  override fun asRawMap(): MutableMap<HttpHeaderName, List<HttpHeaderValue>> = asSequence().map { header ->
    header.name.name to header.value.values.toList()
  }.toMap().toMutableMap()

  /** Factories for obtaining [MicronautMutableHttpHeaders]. */
  companion object {
    /** @return Empty headers instance. */
    @JvmStatic fun empty() = MicronautMutableHttpHeaders(NettyHttpHeaders())
  }
}
