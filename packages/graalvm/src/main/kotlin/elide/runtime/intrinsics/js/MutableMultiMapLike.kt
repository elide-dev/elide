package elide.runtime.intrinsics.js

/**
 * TBD.
 */
public interface MutableMultiMapLike<K: Any, V> : MutableMapLike<K, V>, MultiMapLike<K, V> {
  /**
   * TBD.
   */
  public fun append(key: K, value: V)
}
