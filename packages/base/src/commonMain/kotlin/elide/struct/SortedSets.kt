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

import elide.struct.api.MutableSortedSet
import elide.struct.api.SortedSet

/** A stubbed [SortedSet] implementation, meant to be used as an optimized return value for [emptySortedSet]. */
private object EmptySortedSet : SortedSet<Nothing> {
  override val size: Int = 0
  override fun isEmpty(): Boolean = true
  override fun iterator(): Iterator<Nothing> = object : Iterator<Nothing> {
    override fun hasNext(): Boolean = false
    override fun next(): Nothing = error("Iterator is empty")
  }

  override fun containsAll(elements: Collection<Nothing>): Boolean = false
  override fun contains(element: Nothing): Boolean = false
}

/** Returns an empty, read-only sorted map. */
@Suppress("UNCHECKED_CAST")
public fun <V : Comparable<V>> emptySortedSet(): SortedSet<V> {
  return EmptySortedSet as SortedSet<V>
}

/**
 * Returns a new read-only sorted set with the given elements. Elements of the set are iterated in according to their
 * natural order.
 */
public fun <V : Comparable<V>> sortedSetOf(vararg values: V): SortedSet<V> {
  return TreeSet<V>().apply { addAll(values) }
}

/**
 * Returns a new mutable sorted set with the given elements. Elements of the set are iterated in according to their
 * natural order.
 */
public fun <V : Comparable<V>> mutableSortedSetOf(vararg values: V): MutableSortedSet<V> {
  return TreeSet<V>().apply { addAll(values) }
}
