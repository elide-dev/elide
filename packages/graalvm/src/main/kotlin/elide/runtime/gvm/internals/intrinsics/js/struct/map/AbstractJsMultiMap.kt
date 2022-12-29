package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MultiMapLike

/** TBD. */
internal sealed class AbstractJsMultiMap<K: Any, V> constructor (
  sorted: Boolean,
  mutable: Boolean,
  threadsafe: Boolean,
) : AbstractJsMap<K, V>(
  multi = true,
  mutable = mutable,
  sorted = sorted,
  threadsafe = threadsafe,
), MultiMapLike<K, V>
