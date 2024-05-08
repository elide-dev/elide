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
import elide.struct.api.MutableSortedSet
import elide.struct.codec.TreeSetCodec

/**
 * A [MutableSortedSet] implementation backed by a Red/Black Tree, with each map entry being represented as a node.
 * Search, insertion, and removal all run in O(log n) time thanks to the properties of the tree.
 *
 * Removing map entries while iterating is not allowed, and using [MutableIterator.remove] will throw an exception.
 */
@Serializable(with = TreeSetCodec::class)
public class TreeSet<V : Comparable<V>> : MutableSortedSet<V>, RedBlackTree<V, Unit>() {
  override val size: Int get() = nodeCount
  override fun isEmpty(): Boolean = nodeCount == 0

  override fun iterator(): MutableIterator<V> {
    val inner = nodes().iterator()
    return object : MutableIterator<V> {
      override fun hasNext(): Boolean = inner.hasNext()
      override fun next(): V = inner.next().key
      override fun remove() = error("Removing values while traversing the set is not supported")
    }
  }

  override fun contains(element: V): Boolean = findNodeByKey(element) != null
  override fun containsAll(elements: Collection<V>): Boolean = elements.all(::contains)

  override fun add(element: V): Boolean = addNode(element, Unit) == null
  override fun addAll(elements: Collection<V>): Boolean = elements.fold(false) { modified, e -> add(e) || modified }

  override fun remove(element: V): Boolean = removeNodeByKey(element) != null
  override fun removeAll(elements: Collection<V>): Boolean {
    return elements.fold(false) { modified, e -> remove(e) || modified }
  }

  override fun retainAll(elements: Collection<V>): Boolean {
    var modified = false
    for (node in nodes()) if (node.key !in elements) {
      removeNode(node)
      modified = true
    }
    return modified
  }

  override fun clear(): Unit = reset()
}
