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

import kotlin.collections.Map.Entry
import elide.struct.api.MutableSortedMap
import elide.struct.api.SortedMap

private object EmptySortedMap : SortedMap<Nothing, Nothing> {
  override fun get(key: Nothing): Nothing? = null

  override val entries: Set<Entry<Nothing, Nothing>> get() = emptySet()
  override val keys: Set<Nothing> get() = emptySet()
  override val size: Int get() = 0
  override val values: Collection<Nothing> get() = emptyList()

  override fun isEmpty(): Boolean = true
  override fun containsValue(value: Nothing): Boolean = false
  override fun containsKey(key: Nothing): Boolean = false
}

// TODO(@darvld): test this
@Suppress("UNCHECKED_CAST")
public fun <K : Comparable<K>, V> emptySortedMap(): SortedMap<K, V> {
  return EmptySortedMap as SortedMap<K, V>
}

/**
 *
 */
public fun <K : Comparable<K>, V> sortedMapOf(pairs: Collection<Pair<K, V>>): SortedMap<K, V> {
  return RedBlackTreeMap<K, V>().apply { pairs.forEach { put(it.first, it.second) } }
}

/**
 *
 */
public fun <K : Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>): SortedMap<K, V> {
  return RedBlackTreeMap<K, V>().apply { pairs.forEach { put(it.first, it.second) } }
}

/**
 *
 */
public fun <K : Comparable<K>, V> mutableSortedMapOf(vararg pairs: Pair<K, V>): MutableSortedMap<K, V> {
  return RedBlackTreeMap<K, V>().apply { pairs.forEach { put(it.first, it.second) } }
}
