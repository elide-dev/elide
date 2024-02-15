/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.http

import kotlin.jvm.JvmStatic
import elide.http.api.HttpHeaders
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpHeaders.HeaderValue
import elide.http.api.HttpString
import elide.struct.MutableTreeMap
import elide.struct.api.MutableSortedMap
import elide.struct.mutableSortedMapOf
import elide.http.api.MutableHttpHeaders as HttpHeadersAPI

/**
 * # HTTP Headers (Mutable)
 *
 * Describes the API provided for mutable mapping structures specialized to store HTTP headers associated with values;
 * behaves as a sorted multi-map with multi-values behaving as single values unless addressed with iterators.
 *
 * [HeaderName] keys are normalized for case and trailing/leading whitespace; order of insertion is preserved for values
 * via [HeaderValue].
 *
 * &nbsp;
 *
 * ## Mutability
 *
 * This structure is the mutable form of [HttpHeaders]. Headers and values can be mutated with standard map-like methods
 * after creation in an arbitrary manner. The underlying structure should preserve insertion order for values associated
 * with a given key, and should produce sorted keys when iterated or exported.
 */
public class MutableHttpHeaders private constructor (
  private val headers: MutableSortedMap<HeaderName, HeaderValue> = mutableSortedMapOf(),
) : HttpHeadersAPI, MutableMap<HeaderName, HeaderValue> by headers {
  /** [HttpHeadersAPI.Factory] methods. */
  public companion object: HttpHeadersAPI.Factory {
    @JvmStatic override fun create(): MutableHttpHeaders = MutableHttpHeaders()
    @JvmStatic override fun of(vararg pairs: Pair<String, String>): MutableHttpHeaders = of(pairs.asSequence())
    @JvmStatic override fun of(collection: Collection<Pair<String, String>>): MutableHttpHeaders = of(
      collection.asSequence()
    )

    @JvmStatic override fun of(pairs: Sequence<Pair<String, String>>): MutableHttpHeaders {
      return MutableHttpHeaders().apply {
        // map each pair into a `HeaderName` and string value, then group by `HeaderName`
        pairs.map { HeaderName.of(it.first) to it.second }.groupingBy {
          it.first
        }.foldTo(MutableTreeMap.create<HeaderName, ArrayList<String>>(), ArrayList(2)) { acc, el ->
          // fold into a collection of string values for each `HeaderName`, preserving insertion order
          acc.apply {
            add(el.second)
          }
        }.entries.asSequence().map {
          // convert each `HeaderName` and `LinkedHashSet` of string values into a `HeaderName` and single `HeaderValue`
          it.key to HeaderValue.of(it.value)
        }.forEach {
          headers[it.first] = it.second
        }
      }
    }

    @JvmStatic override fun of(map: Map<String, String>): MutableHttpHeaders = MutableHttpHeaders().apply {
      map.forEach {
        headers[HeaderName.of(it.key)] = HeaderValue.single(it.value)
      }
    }
  }

  override operator fun get(key: HeaderName): HeaderValue? = headers[key]
  override fun getAll(key: HeaderName): Sequence<HttpString> = headers[key]?.allValues?.asSequence() ?: emptySequence()
  override operator fun contains(key: HeaderName): Boolean = headers.containsKey(key)

  override operator fun set(key: HeaderName, value: HttpString) {
    headers[key] = HeaderValue.single(value)
  }

  override fun add(key: HeaderName, value: HttpString) {
    when (val header = headers[key]) {
      null -> headers[key] = HeaderValue.single(value)
      else -> when (header) {
        is HeaderValue.SingleValue -> headers[key] = HeaderValue.multi(header.asString, value)
        is HeaderValue.MultiValue -> headers[key] = header.add(value)
      }
    }
  }

  override fun remove(key: HeaderName, value: HttpString) {
    when (val header = headers[key]) {
      null -> return
      else -> when (header) {
        is HeaderValue.SingleValue -> if (header.asString == value) headers.remove(key)
        is HeaderValue.MultiValue -> if (header.contains(value)) headers[key] = header.remove(value)
      }
    }
  }
}
