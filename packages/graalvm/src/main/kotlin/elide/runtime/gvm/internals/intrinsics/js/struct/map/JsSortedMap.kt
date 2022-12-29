package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.MapLike
import java.util.SortedMap
import java.util.TreeMap

/** TBD. */
@Suppress("unused")
internal class JsSortedMap<K: Comparable<K>, V> constructor (backingMap: SortedMap<K, V>) : BaseJsMap<K, V>(
  backingMap,
  sorted = true,
) {
  // Count of keys present in the map.
  private val keyCount: Int = backingMap.size

  /**
   * TBD.
   */
  constructor() : this(sortedMapImpl())

  /** Immutable sorted map factory. */
  @Suppress("unused") internal companion object Factory {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsSortedMap<Comparable<Any>, Any?>(sortedMapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Comparable<K>, V> sortedMapImpl(): SortedMap<K, V> = TreeMap()

    /**
     * Return a sorted immutable [JsSortedMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> of(map: SortedMap<K, V>): JsSortedMap<K, V> = JsSortedMap(map)

    /**
     * Return a sorted immutable [JsSortedMap] instance, which is a copy of the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Copied JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> copyOf(map: Map<K, V>): JsMap<K, V> = JsMap(sortedMapImpl<K, V>().apply {
      putAll(map)
    })

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided set of [pairs], each an instance of
     * [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromPairs(pairs: Collection<Pair<K, V>>, presorted: Boolean = false) =
      JsSortedMap(sortedMapImpl<K, V>().apply {
        val sorted = if (presorted) {
          pairs
        } else {
          pairs.sortedBy { it.first }
        }
        sorted.forEach {
          put(it.first, it.second)
        }
      })

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided sized collection of [entries], each
     * an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromEntries(entries: Collection<Map.Entry<K, V>>, presorted: Boolean = false) =
      JsSortedMap(sortedMapImpl<K, V>().apply {
        val sorted = if (presorted) {
          entries
        } else {
          entries.sortedBy { it.key }
        }
        sorted.forEach {
          put(it.key, it.value)
        }
      })

    /**
     * Return a generic immutable [JsSortedMap] instance, created from the provided sized collection of [entries], each
     * an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> from(entries: Collection<MapLike.Entry<K, V>>, presorted: Boolean = false) =
      JsSortedMap(sortedMapImpl<K, V>().apply {
        val sorted = if (presorted) {
          entries
        } else {
          entries.sortedBy { it.key }
        }
        sorted.forEach {
          put(it.key, it.value)
        }
      })

    /**
     * Return an empty and immutable JS map instance.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Comparable<K>, V> empty(): JsSortedMap<K, V> = EMPTY_MAP as JsSortedMap<K, V>
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
