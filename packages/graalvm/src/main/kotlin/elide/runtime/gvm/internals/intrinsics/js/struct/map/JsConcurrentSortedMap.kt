package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.runtime.intrinsics.js.MapLike.Entry
import java.util.Comparator
import java.util.SortedMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap

/** TBD. */
@Suppress("unused")
internal class JsConcurrentSortedMap<K: Comparable<K>, V> constructor (backingMap: ConcurrentMap<K, V>) :
  BaseMutableJsMap<K, V>(backingMap, threadsafe = true),
  ConcurrentMap<K, V>,
  SortedMap<K, V> {
  /**
   * TBD.
   */
  constructor() : this(concurrentSortedMapImpl())

  /** Concurrent & sorted map factory. */
  @Suppress("unused") internal companion object Factory {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsConcurrentSortedMap<Comparable<Any>, Any?>(concurrentSortedMapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> concurrentSortedMapImpl(): ConcurrentMap<K, V> = ConcurrentSkipListMap()

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> of(map: ConcurrentMap<K, V>): JsConcurrentSortedMap<K, V> =
      JsConcurrentSortedMap(map)

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> copyOf(map: ConcurrentMap<K, V>): JsConcurrentSortedMap<K, V> {
      return JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        putAll(map)
      })
    }

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, created from the provided set of [pairs], each an
     * instance of [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromPairs(pairs: Collection<Pair<K, V>>): JsConcurrentSortedMap<K, V> {
      return JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        pairs.forEach {
          put(it.first, it.second)
        }
      })
    }

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, created from the provided sized collection of
     * [entries], each an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsConcurrentSortedMap<K, V>
      = JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, created from the provided sized collection of
     * [entries], each an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> from(entries: Collection<Entry<K, V>>): JsConcurrentSortedMap<K, V> {
      return JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, created from the provided set of [entries], each an
     * instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> unboundedEntries(
      entries: Iterable<Map.Entry<K, V>>
    ): JsConcurrentSortedMap<K, V> {
      return JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a sorted concurrent [JsConcurrentSortedMap] instance, created from the provided set of [entries], each an
     * instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> unbounded(entries: Iterable<Entry<K, V>>): JsConcurrentSortedMap<K, V> {
      return JsConcurrentSortedMap(concurrentSortedMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return an empty sorted JS map instance which can safely be used concurrently.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Comparable<K>, V> empty(): JsConcurrentSortedMap<K, V> = EMPTY_MAP as JsConcurrentSortedMap<K, V>
  }

  // Shortcut to cast the current backing-map as a sorted map, which it is supposed to be.
  private fun asSorted(): SortedMap<K, V> = backingMap as SortedMap<K, V>

  /** @inheritDoc */
  override fun comparator(): Comparator<in K> = asSorted().comparator()

  /** @inheritDoc */
  override fun subMap(fromKey: K, toKey: K): SortedMap<K, V> = asSorted().subMap(fromKey, toKey)

  /** @inheritDoc */
  override fun headMap(toKey: K): SortedMap<K, V> = asSorted().headMap(toKey)

  /** @inheritDoc */
  override fun tailMap(fromKey: K): SortedMap<K, V> = asSorted().tailMap(fromKey)

  /** @inheritDoc */
  override fun firstKey(): K = asSorted().firstKey()

  /** @inheritDoc */
  override fun lastKey(): K = asSorted().lastKey()

  /** @inheritDoc */
  override fun getOrDefault(key: K, defaultValue: V): V = backingMap.getOrDefault(key, defaultValue)

  /** @inheritDoc */
  override fun remove(key: K, value: V): Boolean = asSorted().remove(key, value)

  /** @inheritDoc */
  override fun putIfAbsent(key: K, value: V): V? = asSorted().putIfAbsent(key, value)

  /** @inheritDoc */
  override fun replace(key: K, oldValue: V, newValue: V): Boolean = asSorted().replace(key, oldValue, newValue)

  /** @inheritDoc */
  override fun replace(key: K, value: V): V? = asSorted().replace(key, value)

  /** @inheritDoc */
  override fun toString(): String = "Map(immutable, unsorted, concurrent, size=$size)"
}
