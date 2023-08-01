package elide.runtime.intrinsics.js

import elide.runtime.intrinsics.js.err.TypeError

/**
 * TBD.
 */
public interface MutableMapLike<K : Any, V> : MapLike<K, V>, MutableMap<K, V> {
  /**
   * TBD.
   */
  public fun delete(key: K)

  /**
   * TBD.
   */
  public fun set(key: K, value: V)

  /**
   * TBD.
   *
   * @throws TypeError if the underlying map key type is not sortable.
   */
  @Throws(TypeError::class)
  public fun sort()
}
