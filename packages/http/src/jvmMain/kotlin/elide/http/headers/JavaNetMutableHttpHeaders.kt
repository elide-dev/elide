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
import java.util.LinkedList
import java.util.concurrent.ConcurrentSkipListMap
import java.util.function.BiPredicate
import elide.http.Header
import elide.http.HeaderName
import elide.http.HeaderValue
import elide.http.Headers
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.alwaysTruePredicate
import elide.http.asHeaderValue

// Implements platform-specific HTTP headers via `java.net.http` with mutability.
internal class JavaNetMutableHttpHeaders (initial: HttpHeaders) : PlatformMutableHttpHeaders<HttpHeaders> {
  private val backing = ConcurrentSkipListMap<String, MutableList<String>>()

  init {
    initial.map().forEach { (key, values) ->
      backing[key] = LinkedList(values)
    }
  }

  override val headers: HttpHeaders get() = HttpHeaders.of(backing, BiPredicate { _, _ -> true })
  override val size: UInt get() = backing.values.sumOf { it.size }.toUInt()
  override val sizeDistinct: UInt get() = backing.keys.size.toUInt()
  override fun contains(header: HeaderName): Boolean = header.name in backing
  override fun contains(name: HttpHeaderName): Boolean = name in backing
  override fun get(header: HeaderName): HeaderValue? = backing.asHeaderValue(header.name)
  override fun get(name: HttpHeaderName): HeaderValue? = backing.asHeaderValue(name)
  override fun first(header: HeaderName): HttpHeaderValue? = backing[header.name]?.firstOrNull()
  override fun first(name: HttpHeaderName): HttpHeaderValue? = backing[name]?.firstOrNull()
  override fun asRawMap(): Map<HttpHeaderName, List<HttpHeaderValue>> = backing
  override fun asOrdered(): Sequence<Header> = asSequence()
  override fun build(): Headers = JavaNetHttpHeaders(headers)

  override fun asSequence(): Sequence<Header> = backing.asSequence().map {
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

  override fun set(pair: Header) {
    backing[pair.name.name] = pair.value.values.toMutableList()
  }

  override fun set(key: HeaderName, value: HeaderValue) {
    backing[key.name] = value.values.toMutableList()
  }

  override fun set(key: HttpHeaderName, value: HttpHeaderValue) {
    backing[key] = LinkedList<HttpHeaderValue>().also { it.add(value) }
  }

  override fun append(pair: Header) {
    val all = if (pair.name.name !in backing) {
      val it = LinkedList<String>()
      backing[pair.name.name] = it
      it
    } else {
      backing[pair.name.name]!!
    }
    all.addAll(pair.value.values)
  }

  override fun append(key: HttpHeaderName, value: HttpHeaderValue) {
    val all = if (key !in backing) {
      val it = LinkedList<String>()
      backing[key] = it
      it
    } else {
      backing[key]!!
    }
    all.add(value)
  }

  override fun remove(pair: Header) {
   val mutable = backing[pair.name.name]
    if (mutable != null) {
      mutable.removeAll(pair.value.values)
      if (mutable.isEmpty()) {
        backing.remove(pair.name.name)
      }
    }
  }

  override fun remove(header: HeaderName) {
    backing.remove(header.name)
  }

  override fun remove(key: HeaderName, value: HeaderValue) {
    val mutable = backing[key.name]
    if (mutable != null) {
      mutable.removeAll(value.values)
      if (mutable.isEmpty()) {
        backing.remove(key.name)
      }
    }
  }

  override fun remove(name: HttpHeaderName) {
    backing.remove(name)
  }

  override fun remove(key: HttpHeaderName, value: HttpHeaderValue) {
    val mutable = backing[key]
    if (mutable != null) {
      mutable.remove(value)
      if (mutable.isEmpty()) {
        backing.remove(key)
      }
    }
  }

  companion object {
    /** @return Empty [JavaNetMutableHttpHeaders] container. */
    @JvmStatic fun empty(): JavaNetMutableHttpHeaders = JavaNetMutableHttpHeaders(
      HttpHeaders.of(emptyMap(), alwaysTruePredicate)
    )
  }
}
