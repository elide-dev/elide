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
import elide.http.Headers
import elide.http.HttpHeaderName
import elide.http.HttpHeaderValue
import elide.http.asHeaderValue

// Implements platform-specific HTTP headers through Netty, with mutability.
@JvmInline internal value class NettyMutableHttpHeaders(private val backing: HttpHeaders)
  : PlatformMutableHttpHeaders<HttpHeaders> {
  override val headers: HttpHeaders get() = backing
  override val size: UInt get() = backing.entries().size.toUInt()
  override val sizeDistinct: UInt get() = backing.names().size.toUInt()
  override fun contains(header: HeaderName): Boolean = headers.contains(header.name)
  override fun contains(name: HttpHeaderName): Boolean = headers.contains(name)
  override fun get(header: HeaderName): HeaderValue? = headers.asHeaderValue(header.name)
  override fun get(name: HttpHeaderName): HeaderValue? = headers.asHeaderValue(name)
  override fun first(header: HeaderName): HttpHeaderValue? = headers.asHeaderValue(header.name)?.values?.firstOrNull()
  override fun first(name: HttpHeaderName): HttpHeaderValue? = headers.asHeaderValue(name)?.values?.firstOrNull()
  override fun build(): Headers = NettyHttpHeaders(backing)

  // Convert a sequence of Netty header names to Header objects, preserving order.
  private fun Sequence<String>.mapToHeaders() = map {
    it to backing.getAllAsString(it)
  }.mapNotNull { (key, values) ->
    when (values.size) {
      0 -> null
      1 -> HeaderValue.single(values.first())
      else -> HeaderValue.multi(values)
    }?.let {
      Header.of(HeaderName.of(key), it)
    }
  }

  override fun asSequence(): Sequence<Header> = backing.names()
    .asSequence()
    .mapToHeaders()

  override fun asOrdered(): Sequence<Header> = backing.iteratorAsString()
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

  override fun set(pair: Header) {
    val first = pair.value.values.first()
    headers.set(pair.name.name, first)
    pair.value.values.drop(1).forEach {
      headers.add(pair.name.name, it)
    }
  }

  override fun set(key: HeaderName, value: HeaderValue) {
    val first = value.values.first()
    headers.set(key.name, first)
    value.values.drop(1).forEach {
      headers.add(key.name, it)
    }
  }

  override fun set(key: HttpHeaderName, value: HttpHeaderValue) {
    headers.set(key, value)
  }

  override fun append(pair: Header) = pair.value.values.forEach {
    headers.add(pair.name.name, it)
  }

  override fun append(key: HttpHeaderName, value: HttpHeaderValue) {
    headers.add(key, value)
  }

  override fun remove(pair: Header) {
    remove(pair.name, pair.value)
  }

  override fun remove(header: HeaderName) {
    backing.remove(header.name)
    backing.remove(header.nameNormalized)
  }

  override fun remove(key: HeaderName, value: HeaderValue) {
    val match = value.values.toSortedSet()
    val all = backing.getAll(key.nameNormalized)
    backing.remove(key.nameNormalized)
    all.forEach { entry ->
      if (entry !in match) backing.add(key.nameNormalized, entry)
    }
  }

  override fun remove(name: HttpHeaderName) {
    backing.remove(name)
  }

  override fun remove(key: HttpHeaderName, value: HttpHeaderValue) {
    val all = backing.getAll(key)
    backing.remove(key)
    all.forEach { entry ->
      if (entry != value) backing.add(key, entry)
    }
  }

  /** Factories for obtaining [NettyMutableHttpHeaders]. */
  companion object {
    /** @return Empty suite of Netty mutable headers. */
    @JvmStatic fun empty(): NettyMutableHttpHeaders = NettyMutableHttpHeaders(DefaultHttpHeaders())
  }
}
