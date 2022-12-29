package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.MapLike

/** TBD. */
@Suppress("unused")
internal class JsMutableMap<K: Any, V> constructor (backingMap: MutableMap<K, V>) :
  BaseMutableJsMap<K, V>(backingMap) {
  /**
   * TBD.
   */
  constructor() : this(mutableMapImpl())

  /**
   * TBD.
   */
  constructor(size: Int) : this(mutableMapImpl(size))

  /** Mutable map factory. */
  @Suppress("unused") internal companion object Factory {
    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Any, V> mutableMapImpl(size: Int? = null): MutableMap<K, V> = if (size != null) {
      HashMap(size)
    } else {
      HashMap()
    }

    /**
     * Return a generic mutable [JsMutableMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Any, V> of(map: MutableMap<K, V>): JsMutableMap<K, V> = JsMutableMap(map)

    /**
     * Return a generic mutable [JsMutableMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic fun <K: Any, V> copyOf(map: Map<K, V>): JsMap<K, V> = JsMap(mutableMapImpl<K, V>(map.size).apply {
      putAll(map)
    })

    /**
     * Return a generic mutable [JsMutableMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromPairs(pairs: Collection<Pair<K, V>>) = empty<K, V>(pairs.size).apply {
      pairs.forEach {
        put(it.first, it.second)
      }
    }

    /**
     * Return a generic mutable [JsMutableMap] instance, created from the provided set of [entries], each an instance of
     * a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> fromEntries(entries: Collection<Map.Entry<K, V>>) = empty<K, V>(entries.size).apply {
      entries.forEach {
        put(it.key, it.value)
      }
    }

    /**
     * Return a generic mutable [JsMutableMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unboundedPairs(pairs: Iterable<Pair<K, V>>) = JsMutableMap(mutableMapImpl<K, V>().apply {
      pairs.forEach { put(it.first, it.second) }
    })

    /**
     * Return a generic mutable [JsMutableMap] instance, created from the provided set of [entries], each an instance of
     * a normal Java [Map.Entry] of type [K] and [V].
     *
     * This variant explicitly creates a map from an unbounded [Iterable]. If possible, [fromPairs] should be preferred,
     * so that the underlying map implementation can be size-optimized during construction.
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>) = JsMutableMap(
      mutableMapImpl<K, V>().apply {
        entries.forEach { put(it.key, it.value) }
      }
    )

    /**
     * Return a generic mutable [JsMutableMap] instance, created from the provided set of [entries], each an instance of
     * a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Any, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>) = JsMutableMap(
      mutableMapImpl<K, V?>().apply {
        entries.forEach {
          set(it.key, it.value)
        }
      }
    )

    /**
     * Return an empty and mutable JS map instance, pre-sized to the provided [size].
     *
     * @param size Known size.
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(size: Int): JsMutableMap<K, V> = JsMutableMap(mutableMapImpl(size))

    /**
     * Return an empty and mutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Any, V> empty(): JsMutableMap<K, V> = JsMutableMap(mutableMapImpl())
  }

  /** @inheritDoc */
  @Polyglot override fun toString(): String = "Map(mutable, unsorted, size=$size)"
}
