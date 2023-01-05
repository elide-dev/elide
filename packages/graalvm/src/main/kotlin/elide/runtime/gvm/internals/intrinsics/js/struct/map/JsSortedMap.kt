package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.MapLike
import java.util.SortedMap
import java.util.TreeMap

/** Implements a non-mutable sorted map for use with JavaScript; backed by a [TreeMap]. */
@Suppress("unused")
internal class JsSortedMap<K: Comparable<K>, V> constructor (backingMap: SortedMap<K, V>) : BaseJsMap<K, V>(
  backingMap,
  sorted = true,
) {
  // Count of keys present in the map.
  private val keyCount: Int = backingMap.size

  /**
   * Constructor: Empty.
   *
   * Internal-use-only constructor for an empty backed map.
   */
  constructor() : this(mapImpl())

  /** Immutable sorted map factory. */
  @Suppress("unused") internal companion object Factory : SortedMapFactory<JsSortedMap<*, *>> {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsSortedMap<Comparable<Any>, Any?>(mapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Comparable<K>, V> mapImpl(): SortedMap<K, V> = TreeMap()

    /**
     * Return a sorted immutable [JsSortedMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic override fun <K: Comparable<K>, V> of(map: MutableMap<K, V>): JsSortedMap<K, V> =
      JsSortedMap(TreeMap(map))

    /**
     * Return a sorted immutable [JsSortedMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic override fun <K: Comparable<K>, V> copyOf(map: Map<K, V>): JsSortedMap<K, V> = JsSortedMap(
      mapImpl<K, V>().apply {
        putAll(map)
      }
    )

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Comparable<K>, V> fromPairs(pairs: Collection<Pair<K, V>>) =
      JsSortedMap(mapImpl<K, V>().apply {
        pairs.sortedBy { it.first }.forEach {
          put(it.first, it.second)
        }
      })

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided sized collection of [entries], each
     * an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Comparable<K>, V> fromEntries(
      entries: Collection<Map.Entry<K, V>>,
    ) = JsSortedMap(mapImpl<K, V>().apply {
      entries.sortedBy { it.key }.forEach {
        put(it.key, it.value)
      }
    })

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided sized collection of [entries], each
     * an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @return Created JS map instance.
     */
    @JvmStatic override fun <K: Comparable<K>, V> from(entries: Collection<MapLike.Entry<K, V>>) =
      JsSortedMap(mapImpl<K, V>().apply {
        entries.sortedBy { it.key }.forEach {
          put(it.key, it.value)
        }
      })

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
    override fun <K : Comparable<K>, V> unbounded(entries: Iterable<MapLike.Entry<K, V>>): JsSortedMap<K, V> {
      val map = mapImpl<K, V>()
      entries.forEach {
        map[it.key] = it.value
      }
      return JsSortedMap(map)
    }

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
    override fun <K : Comparable<K>, V> unboundedEntries(entries: Iterable<Map.Entry<K, V>>): JsSortedMap<K, V> {
      val map = mapImpl<K, V>()
      entries.forEach {
        map[it.key] = it.value
      }
      return JsSortedMap(map)
    }

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
    override fun <K : Comparable<K>, V> unboundedPairs(pairs: Iterable<Pair<K, V>>): JsSortedMap<K, V> {
      val map = mapImpl<K, V>()
      pairs.forEach {
        map[it.first] = it.second
      }
      return JsSortedMap(map)
    }

    /**
     * Return an empty and immutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic override fun <K: Comparable<K>, V> empty(): JsSortedMap<K, V> = EMPTY_MAP as JsSortedMap<K, V>
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
  @Polyglot override fun toString(): String = "Map(immutable, sorted, size=$keyCount)"
}
