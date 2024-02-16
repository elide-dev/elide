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
import elide.runtime.intrinsics.js.JsIterator
import elide.runtime.intrinsics.js.JsIterator.JsIteratorFactory
import elide.runtime.intrinsics.js.MapLike
import elide.vm.annotations.Polyglot

/** Base implementation of a regular (non-mutable) JS map which is backed by a Java map. */
public sealed class BaseJsMap<K: Any, V> (
  protected var backingMap: Map<K, V>,
  threadsafe: Boolean = false,
  multi: Boolean = false,
  mutable: Boolean = false,
  sorted: Boolean = false,
) : AbstractJsMap<K, V>(
  threadsafe = threadsafe,
  multi = multi,
  mutable = mutable,
  sorted = sorted,
), Map<K, V> {
  internal companion object {
    // Wrap the provided key and value in a `MapLike.Entry`.
    internal fun <K: Any, V> entry(k: K, v: V): MapLike.Entry<K, V> = object : MapLike.Entry<K, V> {
      override val key: K = k
      override val value: V = v
    }

    // Create a stream for the provided `target`, optionally in `parallel` mode (but only if `safe` to do so).
    internal fun <X> toStream(target: Stream<X>, parallel: Boolean, safe: Boolean): Stream<X> = target.let {
      // if parallel streaming is requested and the backing map is threadsafe, then shift the stream to parallel mode.
      if (safe && parallel) it.parallel()
      else if (!safe && parallel) error("Cannot request parallel stream with non-threadsafe map")
      else it
    }
  }

  /** @inheritDoc */
  override fun keysStream(parallel: Boolean): Stream<K> = toStream(
    backingMap.keys.stream(),
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun keysSequence(): Sequence<K> = backingMap.keys.asSequence()

  /** @inheritDoc */
  override fun valuesStream(parallel: Boolean): Stream<V> = toStream(
    backingMap.values.stream(),
    parallel,
    threadsafe,
  )

  /** @inheritDoc */
  override fun valuesSequence(): Sequence<V> = backingMap.values.asSequence()

  /** @inheritDoc */
  override fun containsKey(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  override fun containsValue(value: V): Boolean = backingMap.containsValue(value)

  /** @inheritDoc */
  override fun isEmpty(): Boolean = backingMap.isEmpty()

  /** @inheritDoc */
  @get:Polyglot override val size: Int get() = backingMap.size

  /** @inheritDoc */
  @Polyglot override fun get(key: K): V? = backingMap[key]

  /** @inheritDoc */
  override fun getOrDefault(key: K, defaultValue: V): V = backingMap.getOrDefault(key, defaultValue)

  /** @inheritDoc */
  @Polyglot override fun has(key: K): Boolean = backingMap.containsKey(key)

  /** @inheritDoc */
  @Polyglot override fun keys(): JsIterator<K> = JsIteratorFactory.forIterator(backingMap.keys.iterator())

  /** @inheritDoc */
  @Polyglot override fun values(): JsIterator<V> = JsIteratorFactory.forIterator(backingMap.values.iterator())

  /** @inheritDoc */
  @Polyglot override fun entries(): JsIterator<MapLike.Entry<K, V>> = JsIteratorFactory.forIterator(
    backingMap.entries.stream().map {
      entry(it.key, it.value)
    }.iterator()
  )

  /** @inheritDoc */
  @Polyglot override fun forEach(op: (MapLike.Entry<K, V>) -> Unit): Unit = backingMap.entries.stream().forEach {
    op.invoke(entry(it.key, it.value))
  }

  /**
   * TBD.
   */
  @Polyglot abstract override fun toString(): String
}
