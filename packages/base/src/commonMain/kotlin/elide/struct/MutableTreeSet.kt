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

/**
 *
 */
public class MutableTreeSet<Value> internal constructor (
  pairs: Collection<Value>,
  comparator: Comparator<Value>? = null
) : MutableSortedSet<Value> where Value : Comparable<Value> {
  //
  public constructor() : this(emptyList())

  //
  public constructor(comparator: Comparator<Value>) : this(emptyList(), comparator)

  override fun add(element: Value): Boolean {
    TODO("Not yet implemented")
  }

  override fun addAll(elements: Collection<Value>): Boolean {
    TODO("Not yet implemented")
  }

  override fun clear() {
    TODO("Not yet implemented")
  }

  override fun iterator(): MutableIterator<Value> {
    TODO("Not yet implemented")
  }

  override fun remove(element: Value): Boolean {
    TODO("Not yet implemented")
  }

  override fun removeAll(elements: Collection<Value>): Boolean {
    TODO("Not yet implemented")
  }

  override fun retainAll(elements: Collection<Value>): Boolean {
    TODO("Not yet implemented")
  }

  override val size: Int
    get() = TODO("Not yet implemented")

  override fun contains(element: Value): Boolean {
    TODO("Not yet implemented")
  }

  override fun containsAll(elements: Collection<Value>): Boolean {
    TODO("Not yet implemented")
  }

  override fun isEmpty(): Boolean {
    TODO("Not yet implemented")
  }
}
