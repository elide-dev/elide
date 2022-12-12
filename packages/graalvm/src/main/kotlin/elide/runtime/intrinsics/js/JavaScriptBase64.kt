package elide.runtime.intrinsics.js

/**
 * TBD.
 */
public interface JavaScriptBase64 {
  /**
   * TBD.
   */
  public fun encode(input: String): String

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
