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

import elide.struct.api.MutableSortedList

/**
 *
 */
public class MutablePresortedList<Value> internal constructor (
  values: Collection<Value>,
  presorted: Boolean,
  private val comparator: Comparator<Value>? = null,
) : MutableSortedList<Value> where Value: Comparable<Value> {
  //
  public constructor() : this(emptyList())

  //
  public constructor(values: Collection<Value>) : this(values, false, null)

  //
  public constructor(comparator: Comparator<Value>?) : this(emptyList(), false, comparator)

  //
  public constructor(values: Collection<Value>, comparator: Comparator<Value>?) :
    this(values, false, comparator)

  //
  private val sorted: MutableList<Value> = when {
    presorted -> values.toMutableList()
    comparator != null -> values.toMutableList().apply { sortWith(comparator) }
    else -> values.sorted().toMutableList()
  }

  override val size: Int get() = sorted.size

  // --- Read Methods

  override operator fun get(index: Int): Value = sorted[index]
  override fun contains(element: Value): Boolean = sorted.contains(element)
  override fun containsAll(elements: Collection<Value>): Boolean = sorted.containsAll(elements)
  override fun indexOf(element: Value): Int = sorted.indexOf(element)
  override fun isEmpty(): Boolean = sorted.isEmpty()
  override fun iterator(): MutableIterator<Value> = sorted.iterator()
  override fun lastIndexOf(element: Value): Int = sorted.lastIndexOf(element)
  override fun listIterator(): MutableListIterator<Value> = sorted.listIterator()
  override fun listIterator(index: Int): MutableListIterator<Value> = sorted.listIterator(index)
  override fun subList(fromIndex: Int, toIndex: Int): MutableList<Value> = sorted.subList(fromIndex, toIndex)

  // --- Write Methods

  override fun add(element: Value): Boolean {
    val index = when (val comparator = comparator) {
      null -> sorted.binarySearch(element)
      else -> sorted.binarySearch(element, comparator)
    }
    val insertIndex = if (index < 0) -index - 1 else index
    sorted.add(insertIndex, element)
    return true
  }

  override fun add(index: Int, element: Value): Unit =
    throw UnsupportedOperationException("PresortedList does not support adding elements at a specific index")

  override fun addAll(index: Int, elements: Collection<Value>): Boolean =
    throw UnsupportedOperationException("PresortedList does not support adding elements at a specific index")

  override fun set(index: Int, element: Value): Value =
    throw UnsupportedOperationException("PresortedList does not support setting elements at a specific index")

  override fun addAll(elements: Collection<Value>): Boolean = elements.all {
    add(it)
  }

  override fun clear(): Unit = sorted.clear()

  override fun remove(element: Value): Boolean = sorted.remove(element)

  override fun removeAll(elements: Collection<Value>): Boolean = elements.all {
    remove(it)
  }

  override fun removeAt(index: Int): Value = sorted.removeAt(index)

  override fun retainAll(elements: Collection<Value>): Boolean = sorted.retainAll(elements)
}
