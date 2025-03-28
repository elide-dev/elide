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
package elide.runtime.gvm.internals.intrinsics.js.struct.map

import java.util.stream.Stream
import kotlinx.collections.immutable.toImmutableList
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.MapLike
import elide.vm.annotations.Polyglot

/** Abstract implementation of a JS-compatible multi-map structure. */
internal abstract class BaseJsMultiMap<K: Any, V>(
  @Volatile protected var backingMap: Map<K, List<V>>,
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMultiMap<K, V>(sorted, mutable, threadsafe) {
  /** @inheritDoc */
  override fun keysStream(parallel: Boolean): Stream<K> = BaseJsMap.toStream(
    backingMap.keys.stream(),
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun keysSequence(): Sequence<K> = backingMap.keys.asSequence()

  /** @inheritDoc */
  override fun valuesStream(parallel: Boolean): Stream<V>  = BaseJsMap.toStream(
    backingMap.values.stream().flatMap {
      if (parallel) it.parallelStream()
      else it.stream()
    },
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun valuesSequence(): Sequence<V> = backingMap.values.flatten().asSequence()

  /** @inheritDoc */
  override fun containsKey(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  override fun containsValue(value: V): Boolean = backingMap.values.any { it.contains(value) }

  /** @inheritDoc */
  override fun isEmpty(): Boolean = backingMap.isEmpty()

  /** @inheritDoc */
  @get:Polyglot override val entries: Set<Map.Entry<K, V>> get() = backingMap.entries.flatMap {
    it.value.map { valueEntry ->
      object: Map.Entry<K, V> {
        override val key: K = it.key
        override val value: V = valueEntry
      }
    }
  }.toSet()

  @get:Polyglot override val keys: Set<K> get() = backingMap.keys

  @get:Polyglot override val values: Collection<V> get() = backingMap.values.flatten()

  @get:Polyglot override val size: Int get() = backingMap.size

  @Polyglot override fun get(key: K): V? = backingMap[key]?.firstOrNull()

  override fun getOrDefault(key: K, defaultValue: V): V = backingMap[key]?.firstOrNull() ?: defaultValue

  @Polyglot override fun getAll(key: K): List<V> = backingMap[key]?.toImmutableList() ?: emptyList()

  @Polyglot override fun has(key: K): Boolean = backingMap.containsKey(key)

  override fun keys(): JsIterator<K> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.keys.iterator()
  )

  override fun values(): JsIterator<V> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.values.flatten().iterator()
  )

  override fun entries(): JsIterator<MapLike.Entry<K, V>> = JsIterator.JsIteratorFactory.forIterator(
    backingMap.entries.flatMap {
      it.value.map { valueEntry ->
        object: MapLike.Entry<K, V> {
          override val key: K = it.key
          override val value: V = valueEntry
        }
      }
    }.iterator()
  )

  @Polyglot override fun forEach(op: (MapLike.Entry<K, V>) -> Unit) = entries.forEach {
    op.invoke(BaseJsMap.entry(
      it.key,
      it.value,
    ))
  }

  @Polyglot abstract override fun toString(): String
}
