/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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
import elide.http.api.HttpHeaders.HeaderName
import elide.http.api.HttpString
import elide.struct.api.MutableSortedMap
import elide.struct.mutableSortedMapOf
import elide.http.api.MutableHttpHeaders as HttpHeadersAPI

/**
 * # HTTP Headers
 *
 * Keeps track of HTTP headers in a given HTTP message payload.
 */
public class MutableHttpHeaders private constructor (
  private val headers: MutableSortedMap<HeaderName, HttpString> = mutableSortedMapOf(),
) : HttpHeadersAPI, MutableMap<HeaderName, HttpString> by headers {
  public companion object {
    /**
     *
     */
    @JvmStatic public fun create(): MutableHttpHeaders = MutableHttpHeaders()

    /**
     *
     */
    @JvmStatic public fun of(vararg pairs: Pair<String, String>): MutableHttpHeaders = MutableHttpHeaders().apply {
      for ((key, value) in pairs) {
        headers[HeaderName.of(key)] = value
      }
    }

    /**
     *
     */
    @JvmStatic public fun copyFrom(map: Map<String, String>): MutableHttpHeaders = MutableHttpHeaders().apply {
      map.forEach {
        headers[HeaderName.of(it.key)] = it.value
      }
    }
  }

  override operator fun get(key: String): HttpString? = headers[HeaderName.of(key)]

  override operator fun get(key: HeaderName): HttpString? = headers[key]

  override operator fun set(key: String, value: HttpString) {
    headers[HeaderName.of(key)] = value
  }

  override operator fun set(key: HeaderName, value: HttpString) {
    headers[key] = value
  }
}
