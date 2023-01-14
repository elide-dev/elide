package elide.runtime.intrinsics.js

/**
 * TBD.
 */
public interface JsPromise<T> {
  /**
   * TBD.
   */
  public fun then(onFulfilled: (T) -> Unit): JsPromise<T>

  /**
   * TBD.
   */
  public fun catch(onRejected: (Throwable) -> Unit): JsPromise<T>
}
