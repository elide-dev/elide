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
import elide.struct.api.SortedList
import elide.struct.codec.PresortedListCodec

/**
 *
 */
@Serializable(with = PresortedListCodec::class)
public class PresortedList<Value> internal constructor (
  values: Collection<Value>,
  presorted: Boolean,
  private val comparator: Comparator<Value>? = null,
) : SortedList<Value> where Value: Comparable<Value> {
  //
  public constructor () : this(emptyList())

  //
  public constructor (values: Collection<Value>) : this(values, false, null)

  //
  public constructor (values: SortedList<Value>) : this(values, true, null)

  //
  public constructor (values: Collection<Value>, comparator: Comparator<Value>?) :
    this(values, false, comparator)

  //
  private val sorted: List<Value> = when {
    presorted -> values.toList()
    comparator != null -> values.sortedWith(comparator)
    else -> values.sorted()
  }

  //
  private val count: UInt = sorted.size.toUInt()

  override val size: Int get() = count.toInt()

  // --- Read Methods

  override operator fun get(index: Int): Value = sorted[index]
  override fun contains(element: Value): Boolean = sorted.contains(element)
  override fun containsAll(elements: Collection<Value>): Boolean = sorted.containsAll(elements)
  override fun indexOf(element: Value): Int = sorted.indexOf(element)
  override fun isEmpty(): Boolean = sorted.isEmpty()
  override fun iterator(): Iterator<Value> = sorted.iterator()
  override fun lastIndexOf(element: Value): Int = sorted.lastIndexOf(element)
  override fun listIterator(): ListIterator<Value> = sorted.listIterator()
  override fun listIterator(index: Int): ListIterator<Value> = sorted.listIterator(index)
  override fun subList(fromIndex: Int, toIndex: Int): List<Value> = sorted.subList(fromIndex, toIndex)
  public fun toMutableList(): MutablePresortedList<Value> = MutablePresortedList(
    sorted,
    presorted = true,
    comparator,
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is PresortedList<*>) return false
    if (sorted != other.sorted) return false
    return true
  }

  override fun hashCode(): Int {
    return sorted.hashCode()
  }

  override fun toString(): String {
    return sorted.toString()
  }
}
