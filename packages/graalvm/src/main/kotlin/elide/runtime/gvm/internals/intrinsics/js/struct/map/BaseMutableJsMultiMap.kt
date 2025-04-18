/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.gvm.internals.intrinsics.js.struct.map

import java.util.*
import java.util.stream.Stream
import elide.runtime.gvm.js.JsError.jsErrors
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.MapLike
import elide.runtime.intrinsics.js.MutableMultiMapLike
import elide.runtime.intrinsics.js.err.TypeError
import elide.vm.annotations.Polyglot

/** Implements a JS-compatible map structure which is mutable and accepts multiple values per key. */
internal abstract class BaseMutableJsMultiMap<K: Any, V> (
  map: MutableMap<K, MutableList<V>>,
  threadsafe: Boolean,
  sorted: Boolean,
) : BaseJsMultiMap<K, V>(
  map,
  threadsafe = threadsafe,
  sorted = sorted,
  mutable = true,
), MutableMap<K, V>, MutableMultiMapLike<K, V> {
  // Shortcut to cast the backing map as mutable.
  private fun asMutable(): MutableMap<K, List<V>> = backingMap as MutableMap<K, List<V>>

  /** @inheritDoc */
  override fun keysStream(parallel: Boolean): Stream<K> = BaseJsMap.toStream(
    backingMap.keys.stream(),
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun keysSequence(): Sequence<K> = backingMap.keys.asSequence()

  /** @inheritDoc */
  override fun valuesStream(parallel: Boolean): Stream<V> = BaseJsMap.toStream(
    backingMap.values.stream().flatMap { it.stream() },
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun valuesSequence(): Sequence<V> = backingMap.values.asSequence().flatMap { it.asSequence() }

  /** @inheritDoc */
  override fun containsKey(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  override fun containsValue(value: V): Boolean = backingMap.values.any { it.contains(value) }

  /** @inheritDoc */
  override fun isEmpty(): Boolean = backingMap.isEmpty()

  /** @inheritDoc */
  override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = backingMap.entries.flatMap {
    it.value.map { inner ->
      object: MutableMap.MutableEntry<K, V> {
        override var key: K = it.key
        override var value: V = inner

        override fun setValue(newValue: V): V {
          val old = value
          value = newValue
          return old
        }
      }
    }
  }.toMutableSet()

  /** @inheritDoc */
  override val keys: MutableSet<K> get() = asMutable().keys

  /** @inheritDoc */
  override val values: MutableCollection<V> get() = backingMap.values.flatten().toMutableList()

  /** @inheritDoc */
  override fun getOrDefault(key: K, defaultValue: V): V = backingMap[key]?.firstOrNull() ?: defaultValue

  /** @inheritDoc */
  @Polyglot override fun get(key: K): V? = backingMap[key]?.firstOrNull()

  /** @inheritDoc */
  @Polyglot override fun has(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  @Polyglot override fun keys(): JsIterator<K> = JsIteratorFactory.forIterator(backingMap.keys.iterator())

  /** @inheritDoc */
  @Polyglot override fun values(): JsIterator<V> = JsIteratorFactory.forIterator(
    backingMap.values.stream().flatMap { inner ->
      inner.stream()
    }.iterator()
  )

  /** @inheritDoc */
  @Polyglot override fun entries(): JsIterator<MapLike.Entry<K, V>> = JsIteratorFactory.forIterator(
    backingMap.entries.stream().flatMap {
      it.value.stream().map { inner ->
        BaseJsMap.entry(it.key, inner)
      }
    }.iterator()
  )

  /** @inheritDoc */
  @Polyglot override fun forEach(op: (MapLike.Entry<K, V>) -> Unit) = entries().forEach { inner ->
    when (val entry = inner.value) {
      null -> {}
      else -> op.invoke(entry)
    }
  }

  /** @inheritDoc */
  override fun put(key: K, value: V): V? {
    val previousValue = get(key)
    set(key, value)
    return previousValue
  }

  /** @inheritDoc */
  override fun putIfAbsent(key: K, value: V): V? {
    return if (!backingMap.containsKey(key)) {
      asMutable()[key] = mutableListOf(value)
      null
    } else {
      backingMap[key]?.firstOrNull()
    }
  }

  /** @inheritDoc */
  override fun putAll(from: Map<out K, V>) = from.entries.forEach {
    append(it.key, it.value)
  }

  /** @inheritDoc */
  @Polyglot override fun set(key: K, value: V) {
    asMutable()[key] = mutableListOf(value)
  }

  /** @inheritDoc */
  @Polyglot override fun append(key: K, value: V) {
    (asMutable().getOrPut(key) { mutableListOf() } as MutableList<V>).add(value)
  }

  /** @inheritDoc */
  override fun remove(key: K): V? {
    val valueToDelete = get(key)
    if (valueToDelete != null) {
      asMutable().remove(key)
    }
    return valueToDelete
  }

  /** @inheritDoc */
  @Polyglot override fun clear() = asMutable().clear()

  /** @inheritDoc */
  @Polyglot override fun delete(key: K) {
    asMutable().remove(key)
  }

  /** @inheritDoc */
  @Throws(TypeError::class)
  @Polyglot override fun sort() = jsErrors {
    backingMap = TreeMap<K, List<V>>().apply {
      putAll(backingMap)
    }
  }

  /** @inheritDoc */
  abstract override fun toString(): String
}
