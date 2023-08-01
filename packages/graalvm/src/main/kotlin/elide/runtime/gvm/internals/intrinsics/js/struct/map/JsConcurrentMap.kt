package elide.runtime.gvm.internals.intrinsics.js.struct.map

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import elide.runtime.intrinsics.js.MapLike

/** Implements a thread-safe map for use with JavaScript; backed by a [ConcurrentMap]. */
@Suppress("unused")
internal class JsConcurrentMap<K : Any, V> constructor(backingMap: ConcurrentMap<K, V>) :
  BaseMutableJsMap<K, V>(backingMap, threadsafe = true),
  ConcurrentMap<K, V> {
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

  /** Concurrent map factory. */
  @Suppress("unused")
  internal companion object Factory : MapFactory<JsConcurrentMap<*, *>> {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsConcurrentMap<Any, Any>(mapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K : Any, V> mapImpl(size: Int? = null): ConcurrentMap<K, V> = if (size != null) {
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
    @JvmStatic override fun <K : Any, V> of(map: MutableMap<K, V>): JsConcurrentMap<K, V> = JsConcurrentMap(
      ConcurrentHashMap(map),
    )

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic override fun <K : Any, V> copyOf(map: Map<K, V>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>(map.size).apply {
        putAll(map)
      },
      )
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided set of [pairs], each an
     * instance of [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K : Any, V> fromPairs(pairs: Collection<Pair<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>(pairs.size).apply {
        pairs.forEach {
          put(it.first, it.second)
        }
      },
      )
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided sized collection of [entries],
     * each an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K : Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      },
      )
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided sized collection of [entries],
     * each an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K : Any, V> from(entries: Collection<MapLike.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      },
      )
    }

    /**
     * Return a generic threadsafe [JsConcurrentMap] instance, created from the provided set of [pairs], each an
     * instance of [Pair] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K : Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): JsConcurrentMap<K, V> =
      JsConcurrentMap(
        mapImpl<K, V>().apply {
        pairs.forEach { put(it.first, it.second) }
      },
      )

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
    @JvmStatic override fun <K : Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      },
      )
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
    @JvmStatic override fun <K : Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>): JsConcurrentMap<K, V> {
      return JsConcurrentMap(
        mapImpl<K, V>().apply {
        entries.forEach {
          put(it.key, it.value)
        }
      },
      )
    }

    /**
     * Return an empty and concurrent-safe JS map instance, pre-sized to the provided [size].
     *
     * @param size Known size.
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <K : Any, V> empty(size: Int): JsConcurrentMap<K, V> = JsConcurrentMap(size)

    /**
     * Return an empty and threadsafe JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    override fun <K : Any, V> empty(): JsConcurrentMap<K, V> = EMPTY_MAP as JsConcurrentMap<K, V>
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
