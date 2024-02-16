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
import elide.http.api.HttpText
import elide.http.api.MutableHttpMapping
import elide.struct.MutableTreeMap
import elide.struct.api.MutableSortedMap
import elide.http.api.MutableHttpMapping as MutableHttpMappingAPI

/**
 *
 */
public class MutableHttpMapping<Key, Value> private constructor (
  /** */
  private val backing: MutableSortedMap<Key, Value> = MutableTreeMap.create(),
) : MutableHttpMappingAPI<Key, Value>, MutableSortedMap<Key, Value> by backing
        where Key: Comparable<Key>, Key: HttpText, Value: HttpText {
  //
  public companion object {
    @JvmStatic public fun <Key: HttpText, Value: HttpText> empty(): MutableHttpMapping<Key, Value> {
      TODO("not yet implemented")
    }
  }
}
