package elide.runtime.gvm.internals.intrinsics.js.struct.map

import elide.runtime.intrinsics.js.MapLike
import java.util.stream.Stream

/** TBD. */
internal sealed class AbstractJsMap<K: Any, V> constructor (
  protected val sorted: Boolean,
  protected val mutable: Boolean,
  protected val multi: Boolean,
  protected val threadsafe: Boolean,
) : MapLike<K, V> {
  /**
   * Resolve the [key] [K] to a value [V], or return `null` if it is not present in the map.
   *
   * This method is used to implement map-like objects.
   *
   * @param key Key to resolve.
   * @return Value associated with the key, or `null` if it is not present.
   */
  internal abstract fun resolve(key: K): V?

  /**
   * Hook: A map [key] [K] was resolved to a value [V] (or `null` if the value is not present in the map).
   *
   * This method is used internally to implement validation, logging, and other behaviors.
   *
   * @param key Key that was retrieved.
   * @param value Value that resulted.
   */
  protected abstract fun onResolve(key: K, value: V?)

  /**
   * TBD.
   */
  internal abstract fun keysStream(parallel: Boolean = false): Stream<K>

  /**
   * TBD.
   */
  internal abstract fun keysSequence(): Sequence<K>

  /**
   * TBD.
   */
  internal abstract fun valuesStream(parallel: Boolean = false): Stream<V>

  /**
   * TBD.
   */
  internal abstract fun valuesSequence(): Sequence<V>
}
