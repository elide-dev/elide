package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** TBD. */
@Suppress("unused") internal class JsConcurrentMap<K: Any, V> constructor (backingMap: ConcurrentMap<K, V>) :
  BaseMutableJsMap<K, V>(backingMap, threadsafe = true),
  ConcurrentMap<K, V> {
  /**
   * TBD.
   */
  constructor() : this(concurrentMapImpl())

  /**
   * TBD.
   */
  constructor(size: Int) : this(concurrentMapImpl(size))

  /** Concurrent map factory. */
  @Suppress("unused") internal companion object Factory {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsConcurrentMap<Any, Any?>(concurrentMapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> concurrentMapImpl(size: Int? = null): ConcurrentMap<K, V> = if (size != null) {
      ConcurrentHashMap(size)
    } else {
      ConcurrentHashMap()
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Any, V> of(map: ConcurrentMap<K, V>): JsConcurrentMap<K, V> = JsConcurrentMap(map)

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided set of [pairs], each an
     * instance of [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(concurrentMapImpl<K, V>(pairs.size).apply {
        pairs.forEach {
          put(it.first, it.second)
        }
      })
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided sized collection of [entries],
     * each an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(concurrentMapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided sized collection of [entries],
     * each an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> from(entries: Collection<MapLike.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(concurrentMapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided set of [entries], each an
     * instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred, so
     * that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(concurrentMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided set of [entries], each an
     * instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred, so
     * that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(concurrentMapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return an empty and threadsafe JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(): JsConcurrentMap<K, V> = EMPTY_MAP as JsConcurrentMap<K, V>
  }

  // Cast the backing map as mutable.
  private fun asConcurrent(): ConcurrentMap<K, V> = backingMap as ConcurrentMap<K, V>

  /** @inheritDoc */
  override fun getOrDefault(key: K, defaultValue: V): V = backingMap.getOrDefault(key, defaultValue)

  /** @inheritDoc */
  override fun remove(key: K, value: V): Boolean = asConcurrent().remove(key, value)

  /** @inheritDoc */
  override fun putIfAbsent(key: K, value: V): V? = asConcurrent().putIfAbsent(key, value)

  /** @inheritDoc */
  override fun replace(key: K, oldValue: V, newValue: V): Boolean = asConcurrent().replace(key, oldValue, newValue)

  /** @inheritDoc */
  override fun replace(key: K, value: V): V? = asConcurrent().replace(key, value)

  /** @inheritDoc */
  override fun toString(): String = "Map(immutable, unsorted, concurrent, size=$size)"
}
