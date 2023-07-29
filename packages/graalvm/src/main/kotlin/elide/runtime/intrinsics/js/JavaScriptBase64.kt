package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

/**
 * TBD.
 */
public interface JavaScriptBase64 {
  /**
   * TBD.
   */
  @Polyglot public fun encode(input: String): String

  /**
   * TBD.
   */
  @Polyglot public fun encode(input: String, websafe: Boolean): String

  /**
   *
   */
  public fun decode(input: String): String
}
