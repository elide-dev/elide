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
import kotlin.collections.MutableMap.MutableEntry
import elide.struct.api.MutableSortedMap
import elide.struct.codec.RedBlackTreeMapCodec

/**
 * A [MutableSortedMap] implementation backed by a Red/Black Tree, with each map entry being represented as a node.
 * Search, insertion, and removal all run in O(log n) time thanks to the properties of the tree.
 *
 * The [keys] and [entries] use a specialized implementation which shares the tree used by the map. This allows
 * operations on the returned sets to be reflected on the map without any additional performance costs. Note
 * that neither [keys] nor [entries] support adding new values, following the convention set by key sets in JVM maps.
 *
 * Removing map entries while iterating is not allowed, and using [MutableIterator.remove] will throw an exception.
 */
@Serializable(with = RedBlackTreeMapCodec::class)
internal class RedBlackTreeMap<K : Comparable<K>, V> : MutableSortedMap<K, V>, RedBlackTree<K, V>() {
  /**
   * A specialized [MutableSet] sharing the tree of its parent map, to be used as a read/write view of its keys.
   * Operations performed on the set will be reflected on the map immediately at no additional cost.
   *
   * Adding entries is not allowed, similar to how the key sets on the JVM's maps forbid additions. As a restriction,
   * the [iterator] does not support removal.
   */
  private inner class KeySet : MutableSet<K> {
    override val size: Int get() = nodeCount
    override fun isEmpty(): Boolean = nodeCount == 0

    override fun iterator(): MutableIterator<K> {
      val inner = nodes().iterator()
      return object : MutableIterator<K> {
        override fun hasNext(): Boolean = inner.hasNext()
        override fun next(): K = inner.next().key
        override fun remove() = error("Removing values while traversing the set is not supported")
      }
    }

    override fun contains(element: K): Boolean = findNodeByKey(element) != null
    override fun containsAll(elements: Collection<K>): Boolean = elements.all(::contains)

    override fun add(element: K): Boolean = error("Adding elements to a key set is not supported")
    override fun addAll(elements: Collection<K>): Boolean = error("Adding elements to a key set is not supported")

    override fun remove(element: K): Boolean = removeNodeByKey(element) != null
    override fun removeAll(elements: Collection<K>): Boolean {
      return elements.fold(false) { modified, e -> remove(e) || modified }
    }

    override fun retainAll(elements: Collection<K>): Boolean {
      var modified = false
      for (node in nodes()) if (node.key !in elements) {
        removeNode(node)
        modified = true
      }
      return modified
    }

    override fun clear() = reset()
  }

  /**
   * A specialized [MutableSet] sharing the tree of its parent map, to be used as a read/write view of its entries.
   * Operations performed on the set will be reflected on the map immediately at no additional cost.
   *
   * Adding entries is not allowed, similar to how the key sets on the JVM's maps forbid additions. As a restriction,
   * the [iterator] does not support removal.
   */
  private inner class EntrySet : MutableSet<MutableEntry<K, V>> {
    override val size: Int get() = nodeCount
    override fun isEmpty(): Boolean = nodeCount == 0

    override fun iterator(): MutableIterator<MutableEntry<K, V>> {
      val inner = nodes().iterator()
      return object : MutableIterator<MutableEntry<K, V>> {
        override fun hasNext(): Boolean = inner.hasNext()
        override fun next(): MutableEntry<K, V> = inner.next()
        override fun remove() = error("Removing values while traversing the set is not supported")
      }
    }

    override fun contains(element: MutableEntry<K, V>): Boolean = findNodeByKey(element.key) != null
    override fun containsAll(elements: Collection<MutableEntry<K, V>>): Boolean = elements.all(::contains)

    override fun add(element: MutableEntry<K, V>): Boolean {
      error("Adding elements to an entry set is not supported")
    }

    override fun addAll(elements: Collection<MutableEntry<K, V>>): Boolean {
      error("Adding elements to an entry set is not supported")
    }

    override fun remove(element: MutableEntry<K, V>): Boolean = removeNodeByKey(element.key) != null
    override fun removeAll(elements: Collection<MutableEntry<K, V>>): Boolean {
      return elements.fold(false) { modified, e -> remove(e) || modified }
    }

    override fun retainAll(elements: Collection<MutableEntry<K, V>>): Boolean {
      var modified = false
      for (node in nodes()) if (node !in elements) {
        removeNode(node)
        modified = true
      }
      return modified
    }

    override fun clear() = reset()
  }

  override val keys: MutableSet<K> by lazy(::KeySet)
  override val entries: MutableSet<MutableEntry<K, V>> by lazy(::EntrySet)
  override val values: MutableCollection<V> get() = nodes().map { it.value }.toMutableList()

  override val size: Int get() = nodeCount
  override fun isEmpty(): Boolean = nodeCount == 0

  override operator fun get(key: K): V? = findNodeByKey(key)?.value

  override fun containsKey(key: K): Boolean = findNodeByKey(key) != null
  override fun containsValue(value: V): Boolean = nodes().any { it.value == value }

  override fun put(key: K, value: V): V? = addNode(key, value)
  override fun putAll(from: Map<out K, V>) = from.forEach { (key, value) -> addNode(key, value) }

  override fun remove(key: K): V? = removeNodeByKey(key)?.value
  override fun clear() = reset()
}
