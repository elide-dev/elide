package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MultiMapLike

/**
 * # JS: Abstract Multi-Map
 *
 * Extends the base [AbstractJsMap] with support for the [MultiMapLike] interface, which allows for multiple map values
 * per key. Additional methods are available which resolve all values for a given key. Methods which typically return a
 * single value for a key instead return the first value, if any.
 *
 * @param K Key type for the map. Keys cannot be `null`.
 * @param V Value type for the map. Values can be `null`.
 * @param sorted Whether the map implementation holds a sorted representation.
 * @param mutable Whether the map implementation is mutable.
 * @param threadsafe Whether the map implementation is thread-safe.
 */
internal sealed class AbstractJsMultiMap<K : Any, V> constructor(
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMap<K, V>(
  multi = true,
  mutable = mutable,
  sorted = sorted,
  threadsafe = threadsafe,
),
MultiMapLike<K, V>
