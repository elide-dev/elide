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

package elide.struct

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmStatic
import elide.struct.api.SortedMap
import elide.struct.codec.PresortedMapCodec

/**
 *
 */
@Serializable(with = PresortedMapCodec::class)
public class TreeMap<Key, Value> internal constructor (
  pairs: Iterable<Pair<Key, Value>>,
  presorted: Boolean,
) : AbstractTreeMap<Key, Value>(pairs, presorted), SortedMap<Key, Value> where Key : Comparable<Key> {
  /** */
  public companion object {
    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> empty(): TreeMap<Key, Value> = TreeMap()

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> of(pairs: Iterable<Pair<Key, Value>>): TreeMap<Key, Value> =
      TreeMap(pairs)

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> of(vararg pairs: Pair<Key, Value>): TreeMap<Key, Value> =
      TreeMap(pairs.toList())

    /**
     *
     */
    @JvmStatic public fun <Key: Comparable<Key>, Value> copyOf(map: Map<Key, Value>): TreeMap<Key, Value> =
      TreeMap(map.toList())
  }

  /**
   *
   */
  public constructor () : this(emptyList<Pair<Key, Value>>())

  /**
   *
   */
  public constructor (map: Map<Key, Value>) : this(map.entries.map { it.key to it.value })

  /**
   *
   */
  public constructor (pairs: Iterable<Pair<Key, Value>>) : this(
    pairs,
    false,
  )

  /**
   *
   */
  public constructor (map: SortedMap<Key, Value>) : this(
    map.entries.map { it.key to it.value },
    true,
  )
}
