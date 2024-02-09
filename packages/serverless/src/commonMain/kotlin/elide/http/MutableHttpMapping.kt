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

import kotlin.collections.MutableMap.MutableEntry
import kotlin.jvm.JvmStatic
import elide.http.api.HttpString
import elide.http.api.MutableHttpMapping
import elide.http.api.MutableHttpMapping as MutableHttpMappingAPI

/**
 *
 */
public class MutableHttpMapping<Key, Value> : MutableHttpMappingAPI<Key, Value>
        where Key: HttpString, Value: HttpString {
  //
  public companion object {
    @JvmStatic public fun <Key: HttpString, Value: HttpString> empty(): MutableHttpMapping<Key, Value> {
      TODO("not yet implemented")
    }
  }

  override val size: Int
    get() = TODO("Not yet implemented")

  override fun containsKey(key: Key): Boolean {
    TODO("Not yet implemented")
  }

  override fun containsValue(value: Value): Boolean {
    TODO("Not yet implemented")
  }

  override fun get(key: Key): Value? {
    TODO("Not yet implemented")
  }

  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }

  override val entries: MutableSet<MutableEntry<Key, Value>>
    get() = TODO("Not yet implemented")
  override val keys: MutableSet<Key>
    get() = TODO("Not yet implemented")
  override val values: MutableCollection<Value>
    get() = TODO("Not yet implemented")

  override fun clear() {
    TODO("Not yet implemented")
  }

  override fun put(key: Key, value: Value): Value? {
    TODO("Not yet implemented")
  }

  override fun putAll(from: Map<out Key, Value>) {
    TODO("Not yet implemented")
  }

  override fun remove(key: Key): Value? {
    TODO("Not yet implemented")
  }
}
