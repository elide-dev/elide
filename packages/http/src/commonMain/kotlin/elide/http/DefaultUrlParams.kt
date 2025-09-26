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

package elide.http

// Decode a URL segment.
internal expect fun String.decodeUrlSegment(): String

// Default implementation of a URL parameters container.
@JvmRecord internal data class DefaultUrlParams (private val pairs: Sequence<Pair<String, String>>): Params {
  override val size: UInt get() = pairs.count().toUInt()
  override val sizeDistinct: UInt get() = pairs.groupBy { it.first }.count().toUInt()
  override val keys: Sequence<String> get() = pairs.map { it.first }
  override fun contains(key: ParamName): Boolean = pairs.firstOrNull { it.first == key.name } != null
  override fun contains(key: String): Boolean = pairs.firstOrNull { it.first == key } != null
  override fun get(key: ParamName): ParamValue? = ParamValue.ofNullable(
    pairs
      .filter { it.first == key.name }
      .map { it.second }
  )

  override fun get(key: String): ParamValue? = ParamValue.ofNullable(
    pairs
      .filter { it.first == key }
      .map { it.second }
  )

  companion object {
    @JvmStatic fun parse(url: String): DefaultUrlParams = DefaultUrlParams(
      url.removePrefix("?")
        .split("&")
        .asSequence()
         .filter { it.isNotEmpty() }
          .map { it.split("=") }
          .filter {
            it.firstOrNull { inner -> inner.isNotEmpty() } != null
          }
          .map {
            it.first() to (it.getOrNull(1)?.decodeUrlSegment() ?: "")
          }
    )
  }
}
