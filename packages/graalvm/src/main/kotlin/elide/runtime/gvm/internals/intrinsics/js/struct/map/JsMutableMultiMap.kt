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

import elide.runtime.intrinsics.js.MapLike
import elide.vm.annotations.Polyglot

/** Implements a JavaScript-compatible `Map` with mutable abilities, and a potential for multiple values per key. */
@Suppress("unused") internal class JsMutableMultiMap<K: Any, V> (
  backingMap: MutableMap<K, MutableList<V>>
) : BaseMutableJsMultiMap<K, V>(
  backingMap,
  threadsafe = false,
  sorted = false,
) {
  /**
   * Constructor: Empty.
   *
   * Internal-use-only constructor for an empty backed map.
   */
  constructor() : this(mapImpl())

  /**
   * Constructor: Sized.
   *
   * Internal-use-only constructor for pre-sized map structures, when a size is known at construction time.
   *
   * @param size Size of the desired map.
   */
  constructor(size: Int) : this(mapImpl(size))

  /** Mutable multi-map factory. */
  @Suppress("unused") internal companion object Factory : MapFactory<JsMutableMultiMap<*, *>> {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsMutableMultiMap<Any, Any?>(mapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> mapImpl(size: Int? = null): MutableMap<K, MutableList<V>> = if (size != null) {
      HashMap(size)
    } else {
      HashMap()
    }

    /**
     * Return a generic [JsMutableMultiMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic override fun <K: Any, V> of(map: MutableMap<K, V>): JsMutableMultiMap<K, V> = JsMutableMultiMap(
      mapImpl<K, V>().apply {
        map.forEach { (key, value) ->
          this[key] = (this[key] ?: mutableListOf()).apply {
            add(value)
          }
        }
      }
    )

    /**
     * Return a generic [JsMutableMultiMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic override fun <K: Any, V> copyOf(map: Map<K, V>) = JsMutableMultiMap(mapImpl<K, V>(map.size).apply {
      map.entries.forEach {
        this[it.key] = (this[it.key] ?: mutableListOf()).apply {
          add(it.value)
        }
      }
    })

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>) = JsMutableMultiMap(mapImpl<K, V>().apply {
      pairs.forEach {
        this[it.first] = (this[it.first] ?: mutableListOf()).apply {
          add(it.second)
        }
      }
    })

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided sized collection of [entries], each an
     * instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsMutableMultiMap<K, V> {
      return JsMutableMultiMap(mapImpl<K, V>(entries.size).apply {
        entries.forEach {
          this[it.key] = (this[it.key] ?: mutableListOf()).apply {
            add(it.value)
          }
        }
      })
    }

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided sized collection of [entries], each an
     * instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> from(entries: Collection<MapLike.Entry<K, V>>): JsMutableMultiMap<K, V> {
      return JsMutableMultiMap(mapImpl<K, V>(entries.size).apply {
        entries.forEach {
          this[it.key] = (this[it.key] ?: mutableListOf()).apply {
            add(it.value)
          }
        }
      })
    }

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided set of [entries], each an instance of a
     * normal Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>) = JsMutableMultiMap(
      mapImpl<K, V>().apply {
        entries.forEach {
          this[it.key] = (this[it.key] ?: mutableListOf()).apply {
            add(it.value)
          }
        }
      }
    )

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): JsMutableMultiMap<K, V> {
      return JsMutableMultiMap(mapImpl<K, V>().apply {
        pairs.forEach {
          this[it.first] = (this[it.first] ?: mutableListOf()).apply {
            add(it.second)
          }
        }
      })
    }

    /**
     * Return a generic [JsMutableMultiMap] instance, created from the provided set of [entries], each an instance of a
     * JS [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>) = JsMutableMultiMap(
      mapImpl<K, V>().apply {
        entries.forEach {
          this[it.key] = (this[it.key] ?: mutableListOf()).apply {
            add(it.value)
          }
        }
      }
    )

    /**
     * Return an empty JS map instance, pre-sized to the provided [size].
     *
     * @param size Known size.
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(size: Int): JsMutableMultiMap<K, V> = JsMutableMultiMap(mapImpl(size))

    /**
     * Return an empty and mutable JS multi-map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic override fun <K: Any, V> empty(): JsMutableMultiMap<K, V> = EMPTY_MAP as JsMutableMultiMap<K, V>
  }

  /** @inheritDoc */
  @Polyglot override fun toString(): String = "MultiMap(mutable, unsorted, size=$size)"
}
