package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * TBD.
 */
public interface JavaScriptBase64 {
  /**
   * TBD.
   */
  @Polyglot public fun encode(input: String): String = encode(input, false)

  /**
   * TBD.
   */
  @Polyglot public fun encode(input: String, websafe: Boolean): String

  /**
   *
   */
  public fun decode(input: String): String

  /**
   * TBD.
   */
  public fun btoa(input: String): String = encode(input)

  /**
   *
   */
  public fun atob(input: String): String = decode(input)
}
