package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import elide.vm.annotations.Polyglot

/** Implements a JavaScript-compatible `Map` with no mutable abilities. */
@Suppress("unused") internal class JsMap<K: Any, V> constructor (backingMap: Map<K, V>) :
  BaseJsMap<K, V>(backingMap) {
  // Count of keys present in the map.
  private val keyCount: Int = backingMap.size

  /**
   * Constructor: Empty.
   *
   * Internal-use-only constructor for an empty backed map.
   */
  constructor() : this(mapOf())

  /**
   * Constructor: Sized.
   *
   * Internal-use-only constructor for pre-sized map structures, when a size is known at construction time.
   *
   * @param size Size of the desired map.
   */
  constructor(size: Int) : this(mapImpl(size))

  /** Immutable map factory. */
  @Suppress("unused") internal companion object Factory : MapFactory<JsMap<*, *>> {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsMap<Any, Any?>(emptyMap())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> mapImpl(size: Int? = null): MutableMap<K, V> = if (size != null) {
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
    @JvmStatic override fun <K: Any, V> of(map: MutableMap<K, V>): JsMap<K, V> = JsMap(map)

    /**
     * Return a generic immutable [JsMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic override fun <K: Any, V> copyOf(map: Map<K, V>) = JsMap(mapImpl<K, V>(map.size).apply {
      putAll(map)
    })

    /**
     * Return a generic immutable [JsMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>) = JsMap(mapImpl<K, V>().apply {
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
    @JvmStatic override fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>): JsMap<K, V> {
      return JsMap(mapImpl<K, V>(entries.size).apply {
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
    @JvmStatic override fun <K: Any, V> from(entries: Collection<MapLike.Entry<K, V>>): JsMap<K, V> {
      return JsMap(mapImpl<K, V>(entries.size).apply {
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
    @JvmStatic override fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>) =
      JsMap(entries.associate { it.key to it.value })

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
    @JvmStatic override fun <K: Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): JsMap<K, V> {
      return JsMap(mapImpl<K, V>().apply {
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
    @JvmStatic override fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>) = JsMap(entries.associate {
      it.key to it.value
    })

    /**
     * Return an empty JS map instance, pre-sized to the provided [size].
     *
     * @param size Known size.
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(size: Int): JsMap<K, V> = JsMap(mapImpl(size))

    /**
     * Return an empty and immutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic override fun <K: Any, V> empty(): JsMap<K, V> = EMPTY_MAP as JsMap<K, V>
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
