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

import elide.struct.api.MutableSortedSet
import elide.struct.api.SortedSet

private val EMPTY_SORTED_SET: SortedSet<Nothing> = TreeSet(emptyList())

/**
 *
 */
@Suppress("UNCHECKED_CAST")
public fun <V: Comparable<V>> sortedSetOf(): SortedSet<V> = EMPTY_SORTED_SET as SortedSet<V>

/**
 *
 */
public fun <V: Comparable<V>> sortedSetOf(vararg values: V): SortedSet<V> =
  TreeSet(values.toList())

/**
 *
 */
public fun <V: Comparable<V>> mutableSortedSetOf(vararg values: V): MutableSortedSet<V> =
  MutableTreeSet(values.toList())
