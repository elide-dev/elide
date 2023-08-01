package elide.runtime.intrinsics.js

/**
 * TBD.
 */
public interface MapLike<K : Any, V> : Map<K, V> {
  /**
   * TBD.
   */
  public interface Entry<K, V> {
    /**
     * TBD.
     */
    public val key: K

    /**
     * TBD.
     */
    public val value: V
  }

  /**
   * TBD.
   */
  public fun entries(): JsIterator<Entry<K, V>>

  /**
   * TBD.
   */
  public fun forEach(op: (Entry<K, V>) -> Unit)

  /**
   * TBD.
   */
  public override fun get(key: K): V?

  /**
   * TBD.
   */
  public fun has(key: K): Boolean

  /**
   * TBD.
   */
  public fun keys(): JsIterator<K>

  /**
   * TBD.
   */
  override fun toString(): String

  /**
   * TBD.
   */
  public fun values(): JsIterator<V>
}
