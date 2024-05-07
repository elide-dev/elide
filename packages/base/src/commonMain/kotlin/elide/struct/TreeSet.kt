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

import kotlin.jvm.JvmStatic
import elide.struct.api.SortedSet

/**
 *
 */
public class TreeSet<Value> internal constructor(
  entries: Collection<Value>,
  presorted: Boolean = false,
) : SortedSet<Value> where Value : Comparable<Value> {
  public companion object {
    /**
     *
     */
    @JvmStatic public fun <Value : Comparable<Value>> of(pairs: Collection<Value>): TreeSet<Value> = TreeSet(pairs)

    /**
     *
     */
    @JvmStatic public fun <Value : Comparable<Value>> of(vararg values: Value): TreeSet<Value> =
      TreeSet(values.toList())
  }

  //
  public constructor () : this(emptyList())

  //
  private data object Marker

  //
  private val backing: RedBlackTreeMap<Value, Marker> = RedBlackTreeMap<Value, Marker>().apply {
    entries.forEach { put(it, Marker) }
  }

  override val size: Int get() = backing.size

  override fun iterator(): MutableIterator<Value> = backing.keys.iterator()
  override fun contains(element: Value): Boolean = backing.keys.contains(element)
  override fun containsAll(elements: Collection<Value>): Boolean = backing.keys.containsAll(elements)
  override fun isEmpty(): Boolean = backing.isEmpty()
}
