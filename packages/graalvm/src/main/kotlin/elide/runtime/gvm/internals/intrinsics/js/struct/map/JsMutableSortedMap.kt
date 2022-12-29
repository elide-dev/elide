package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.MapLike
import java.util.Comparator
import java.util.SortedMap
import java.util.TreeMap

/** TBD. */
@Suppress("unused")
internal class JsMutableSortedMap<K: Comparable<K>, V> private constructor (
  backingMap: MutableMap<K, V>
) : BaseMutableJsMap<K, V>(backingMap, sorted = true), MutableSortedMap<K, V> {
  /**
   * TBD.
   */
  constructor() : this(sortedMapImpl())

  /** Immutable sorted map factory. */
  @Suppress("unused") internal companion object Factory {
    // Singleton empty map instance.
    private val EMPTY_MAP = JsMutableSortedMap<Comparable<Any>, Any?>(sortedMapImpl())

    // Internal function to create a backing-map implementation.
    @JvmStatic private fun <K: Comparable<K>, V> sortedMapImpl(): MutableMap<K, V> = TreeMap()

    /**
     * Return a sorted and mutable [JsMutableSortedMap] instance, which wraps the provided [map].
     *
     * @param map Existing map instance to wrap.
     * @return Wrapped JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> of(map: SortedMap<K, V>): JsMutableSortedMap<K, V> = JsMutableSortedMap(map)

    /**
     * Return a sorted and mutable [JsMutableSortedMap] instance, created from the provided set of [pairs], each an
     * instance of [Pair] of type [K] and [V].
     *
     * @param pairs Pairs from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromPairs(pairs: Collection<Pair<K, V>>, presorted: Boolean = false) =
      JsMutableSortedMap(sortedMapImpl<K, V>().apply {
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
     * Return a sorted and mutable [JsMutableSortedMap] instance, created from the provided sized collection of
     * [entries], each an instance of a normal Java [Map.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> fromEntries(
      entries: Collection<Map.Entry<K, V>>,
      presorted: Boolean = false,
    ) = JsMutableSortedMap(sortedMapImpl<K, V>().apply {
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
     * Return a sorted and mutable [JsMutableSortedMap] instance, created from the provided sized collection of
     * [entries], each an instance of a JS [MapLike.Entry] of type [K] and [V].
     *
     * @param entries Map entries from which to create a JS map.
     * @param presorted Whether the collection of entries is known to already be sorted. Defaults to `false`, in which
     *   case the entries are sorted before key-wise insertion.
     * @return Created JS map instance.
     */
    @JvmStatic fun <K: Comparable<K>, V> from(entries: Collection<MapLike.Entry<K, V>>, presorted: Boolean = false) =
      JsMutableSortedMap(sortedMapImpl<K, V>().apply {
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
     * Return an empty mutable JS map instance that maintains sort order.
     *
     * @return Empty JS map instance.
     */
    @Suppress("UNCHECKED_CAST")
    @JvmStatic fun <K: Comparable<K>, V> empty(): JsMutableSortedMap<K, V> = EMPTY_MAP as JsMutableSortedMap<K, V>
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
  @Polyglot override fun sort() = Unit  // no-op: map is already sorted

  /** @inheritDoc */
  @Polyglot override fun toString(): String = "Map(mutable, sorted, size=$size)"
}
