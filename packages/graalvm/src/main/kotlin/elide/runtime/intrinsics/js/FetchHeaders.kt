package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface FetchHeaders {
  /**
   * TBD.
   */
  @Polyglot public fun entries(): JsIterator<Array<String>>

  /**
   * TBD.
   */
  @Polyglot public fun forEach(op: (value: String, name: String) -> Unit)

  /**
   * TBD.
   */
  @Polyglot public fun forEach(op: (value: String, name: String, obj: FetchHeaders) -> Unit)

  /**
   * TBD.
   */
  @Polyglot public fun get(name: String): String?

  /**
   * TBD.
   */
  @Polyglot public fun has(name: String): Boolean

  /**
   * TBD.
   */
  @Polyglot public fun keys(): JsIterator<String>

  /**
   * TBD.
   */
  @Polyglot public fun values(): JsIterator<String>
}
