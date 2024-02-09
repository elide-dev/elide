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

import elide.struct.api.MutableSortedMap
import elide.struct.api.SortedMap

private val EMPTY_SORTED_MAP: SortedMap<Nothing, Nothing> = TreeMap(emptyList())

/**
 *
 */
@Suppress("UNCHECKED_CAST")
public fun <K: Comparable<K>, V> sortedMapOf(): SortedMap<K, V> = EMPTY_SORTED_MAP as SortedMap<K, V>

/**
 *
 */
public fun <K: Comparable<K>, V> sortedMapOf(pairs: Collection<Pair<K, V>>): SortedMap<K, V> =
  TreeMap(pairs)

/**
 *
 */
public fun <K: Comparable<K>, V> sortedMapOf(vararg pairs: Pair<K, V>): SortedMap<K, V> =
  TreeMap(pairs.toList())

/**
 *
 */
public fun <K: Comparable<K>, V> mutableSortedMapOf(vararg pairs: Pair<K, V>): MutableSortedMap<K, V> =
  MutableTreeMap(pairs.toList())
