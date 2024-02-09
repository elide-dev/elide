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

package elide.struct

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic
import elide.struct.api.MutableSortedMap
import elide.struct.codec.PresortedMutableMapCodec

/**
 *
 */
@Serializable(with = PresortedMutableMapCodec::class)
public class MutableTreeMap<Key, Value> internal constructor (
  pairs: Iterable<Pair<Key, Value>>,
  presorted: Boolean,
) : AbstractTreeMap<Key, Value>(pairs, presorted), MutableSortedMap<Key, Value> where Key : Comparable<Key> {
  public companion object {
    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> empty(): MutableTreeMap<Key, Value> = MutableTreeMap()

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> of(
      pairs: Iterable<Pair<Key, Value>>
    ): MutableTreeMap<Key, Value> = MutableTreeMap(pairs.toList())

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> of(vararg pairs: Pair<Key, Value>): MutableTreeMap<Key, Value> =
      MutableTreeMap(pairs.toList())

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> copyOf(map: Map<Key, Value>): MutableTreeMap<Key, Value> =
      MutableTreeMap(map.toList())
  }

  //
  public constructor () : this(emptyList())

  //
  public constructor (map: Map<Key, Value>) : this(map.entries.map { it.key to it.value })

  /**
   *
   */
  public constructor (pairs: Iterable<Pair<Key, Value>>) : this(
    pairs,
    false,
  )

  override fun clear(): Unit = evictRoot()
  override fun put(key: Key, value: Value): Value? = addNode(key, value)
  override fun putAll(from: Map<out Key, Value>): Unit = from.forEach { (k, v) -> put(k, v) }
  override fun remove(key: Key): Value? = removeNodeByKey(key)?.let { return it.value }

  public operator fun set(key: Key, value: Value): Value? = addNode(key, value)
}
