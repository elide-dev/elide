package elide.runtime.gvm.internals.intrinsics.js.struct.map

import java.util.*

/**
 * TBD.
 */
internal interface MutableSortedMap<K : Comparable<K>, V> : MutableMap<K, V>, SortedMap<K, V>
