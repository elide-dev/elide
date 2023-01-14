package elide.runtime.intrinsics.js

/**
 * TBD.
 */
public interface MultiMapLike<K: Any, V> : MapLike<K, V> {
  /**
   * TBD.
   */
  public fun getAll(key: K): List<V>
}
