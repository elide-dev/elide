/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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
import elide.runtime.intrinsics.js.MapLike

/**
 * # JS: Abstract Map
 *
 * Implements baseline `Map`-like functionality for JavaScript-compatible map structures. This is the top-most class for
 * all custom JS map implementations provided by Elide. Specialized sub-classes are available for maps with mutability,
 * sorted entries, multi-value entries, and threadsafe access. Each is backed by an equivalent Java map, with JavaScript
 * map methods mapped to equivalent Java map methods.
 *
 * ## Mutability
 *
 * By default, maps which adhere to this interface are non-mutable. [BaseMutableJsMap] implements the mutability methods
 * and provides an equivalent top-abstract-base.
 *
 * ## Sorting
 *
 * Since the structure of JS-provided map classes is like a giant Russian doll toy (with all map types extending this
 * one, and so on), it is entirely possible to represent sorted maps as regular [JsMap] or [AbstractJsMap] instances. By
 * keeping a type which adheres to [JsSortedMap], the developer knows that the map is sorted, and can rely on the
 * internal map state reflecting that.
 *
 * ## Concurrency
 *
 * Thread-safe maps are available where the underlying Java map is thread-safe. Concurrent maps should not usually be
 * necessary when interfacing with JS code, since JS execution is inherently single-threaded in almost all cases. If,
 * however, you are doing work in the background (in another thread) with a map that is exposed to JavaScript, a thread
 * safe map can make sure both views of the backing data are consistent.
 *
 * @param K Key type for the map. Keys cannot be `null`.
 * @param V Value type for the map. Values can be `null`.
 * @param sorted Whether the map implementation holds a sorted representation.
 * @param mutable Whether the map implementation is mutable.
 * @param multi Whether the map implementation allows multiple values per key.
 * @param threadsafe Whether the map implementation is thread-safe.
 */
internal sealed class AbstractJsMap<K: Any, V> constructor (
  internal val sorted: Boolean,
  internal val mutable: Boolean,
  internal val multi: Boolean,
  internal val threadsafe: Boolean,
) : MapLike<K, V> {
  /**
   * Keys: Stream.
   *
   * Produce a Java [Stream] of keys (of type [K]) which are resident within this map instance. If the backing map is
   * thread-safe, a [parallel] stream can be requested; requested a parallel stream with a non-threadsafe map throws an
   * error.
   *
   * @param parallel Whether to request a parallel stream.
   * @return A Java [Stream] of keys.
   * @throws IllegalStateException If a parallel stream is requested with a non-threadsafe map.
   */
  internal abstract fun keysStream(parallel: Boolean = false): Stream<K>

  /**
   * Keys: Sequence.
   *
   * Produce a Kotlin [Sequence] of keys (of type [K]) which are resident within this map instance.
   *
   * @return Sequence of keys present within this map.
   */
  internal abstract fun keysSequence(): Sequence<K>

  /**
   * Values: Stream.
   *
   * Produce a Java [Stream] of values (of type [V]) which are resident within this map instance. If the backing map is
   * thread-safe, a [parallel] stream can be requested; requested a parallel stream with a non-threadsafe map throws an
   * error.
   *
   * @param parallel Whether to request a parallel stream.
   * @return A Java [Stream] of values.
   * @throws IllegalStateException If a parallel stream is requested with a non-threadsafe map.
   */
  internal abstract fun valuesStream(parallel: Boolean = false): Stream<V>

  /**
   * Values: Sequence.
   *
   * Produce a Kotlin [Sequence] of values (of type [V]) which are resident within this map instance.
   *
   * @return Sequence of values present within this map.
   */
  internal abstract fun valuesSequence(): Sequence<V>

  /** Base map factory. */
  internal interface MapFactory<MapImpl> {
    /**
     * Return a JavaScript map instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    fun <K: Any, V> of(map: MutableMap<K, V>): MapImpl

    /**
     * Return a JavaScript map instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    fun <K: Any, V> copyOf(map: Map<K, V>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided set of [pairs], each an instance of [Pair] of type
     * [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided sized collection of [entries], each an instance of a
     * normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided sized collection of [entries], each an instance of a
     * JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> from(entries: Collection<MapLike.Entry<K, V>>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided set of [entries], each an instance of a normal Java
     * [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided set of [pairs], each an instance of [Pair] of type
     * [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): MapImpl

    /**
     * Return a JavaScript map instance, created from the provided set of [entries], each an instance of a JS
     * [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>): MapImpl

    /**
     * Return an empty and immutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    fun <K: Any, V> empty(): MapImpl
  }

  /** Sorted map factory. */
  internal interface SortedMapFactory<MapImpl> {
    /**
     * Return a sorted JavaScript map instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    fun <K: Comparable<K>, V> of(map: MutableMap<K, V>): MapImpl

    /**
     * Return a sorted JavaScript map instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    fun <K: Comparable<K>, V> copyOf(map: Map<K, V>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided set of [pairs], each an instance of [Pair] of
     * type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> fromPairs(pairs: Collection<Pair<K, V>>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided sized collection of [entries], each an
     * instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> fromEntries(entries: Collection<Map.Entry<K, V>>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided sized collection of [entries], each an
     * instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> from(entries: Collection<MapLike.Entry<K, V>>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided set of [entries], each an instance of a normal
     * Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided set of [pairs], each an instance of [Pair] of
     * type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): MapImpl

    /**
     * Return a sorted JavaScript map instance, created from the provided set of [entries], each an instance of a JS
     * [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    fun <K: Comparable<K>, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>): MapImpl

    /**
     * Return an empty, immutable, and "sorted" JS map instance.
     *
     * @return Empty JS map instance.
     */
    fun <K: Comparable<K>, V> empty(): MapImpl
  }
}
