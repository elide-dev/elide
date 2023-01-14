package elide.runtime.gvm.internals.intrinsics.js.url

import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsConcurrentSortedMap

/**
 * Backing map type for [URLSearchParams].
 */
internal typealias URLParamsMap = JsConcurrentSortedMap<String, MutableList<String>>
