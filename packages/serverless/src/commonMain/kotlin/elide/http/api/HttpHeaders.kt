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

package elide.http.api

import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic
import elide.http.api.HttpHeaders.HeaderName

/**
 *
 */
public interface HttpHeaders : HttpMapping<HeaderName, HttpString> {
  /**
   * ## HTTP: Header Name
   *
   * Normalized HTTP header name; always lower-cased and trimmed.
   */
  @JvmInline public value class HeaderName private constructor (
    public val name: HttpString,
  ) : CharSequence, Comparable<HeaderName> {
    public companion object {
      /**
       *
       */
      @JvmStatic public fun of(name: String): HeaderName = HeaderName(name.trim().lowercase())
    }

    override val length: Int get() = name.length
    override fun get(index: Int): Char = name[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = name.subSequence(startIndex, endIndex)

    override fun compareTo(other: HeaderName): Int {
      return name.hashCode().compareTo(other.name.hashCode())
    }
  }

  /**
   *
   */
  public override operator fun get(key: HeaderName): HttpString? {
    TODO("Not yet implemented")
  }

  /**
   *
   */
  public operator fun get(key: String): HttpString? {
    TODO("Not yet implemented")
  }
}
