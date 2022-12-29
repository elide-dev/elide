package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.MapLike

/** TBD. */
@Suppress("unused") internal class JsMap<K: Any, V> constructor (backingMap: Map<K, V>) :
  BaseJsMap<K, V>(backingMap) {
  // Count of keys present in the map.
  private val keyCount: Int = backingMap.size

  /**
   * TBD.
   */
  constructor() : this(mapOf())

  /**
   * TBD.
   */
  constructor(size: Int) : this(immutableMapImpl(size))

  /** Immutable map factory. */
  @Suppress("unused") internal companion object Factory {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsMap<Any, Any?>(emptyMap())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> immutableMapImpl(size: Int? = null): MutableMap<K, V> = if (size != null) {
      HashMap(size)
    } else {
      HashMap()
    }

    /**
     * Return a generic immutable [JsMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Any, V> of(map: Map<K, V>): JsMap<K, V> = JsMap(map)

    /**
     * Return a generic immutable [JsMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic fun <K: Any, V> copyOf(map: Map<K, V>): JsMap<K, V> = JsMap(immutableMapImpl<K, V>(map.size).apply {
      putAll(map)
    })

    /**
     * Return a generic immutable [JsMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>) = JsMap(immutableMapImpl<K, V>().apply {
      pairs.forEach {
        put(it.first, it.second)
      }
    })

    /**
     * Return a generic immutable [JsMap] instance, created from the provided sized collection of [entries], each an
     * instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsMap<K, V> {
      return JsMap(immutableMapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a generic immutable [JsMap] instance, created from the provided sized collection of [entries], each an
     * instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> from(entries: Collection<MapLike.Entry<K, V>>): JsMap<K, V> {
      return JsMap(immutableMapImpl<K, V>(entries.size).apply {
        entries.forEach {
          put(it.key, it.value)
        }
      })
    }

    /**
     * Return a generic immutable [JsMap] instance, created from the provided set of [entries], each an instance of
     * a normal Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>) =
      JsMap(entries.associateBy { it.key })

    /**
     * Return a generic immutable [JsMap] instance, created from the provided set of [pairs], each an instance of [Pair]
     * of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): JsMap<K, V> {
      return JsMap(immutableMapImpl<K, V>().apply {
        pairs.forEach { put(it.first, it.second) }
      })
    }

    /**
     * Return a generic immutable [JsMap] instance, created from the provided set of [entries], each an instance of
     * a JS [MapLike.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>) = JsMap(entries.associateBy { it.key })

    /**
     * Return an empty and immutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(): JsMap<K, V> = EMPTY_MAP as JsMap<K, V>
  }

  /** @inheritDoc */
  override val keys: Set<K> get() = backingMap.keys

  /** @inheritDoc */
  override val values: Collection<V> get() = backingMap.values

  /** @inheritDoc */
  override val entries: Set<Map.Entry<K, V>> get() = backingMap.entries

  /** @inheritDoc */
  @get:Polyglot override val size: Int get() = keyCount

  /** @inheritDoc */
  @Polyglot override fun toString(): String = "Map(immutable, unsorted, size=$keyCount)"
}
