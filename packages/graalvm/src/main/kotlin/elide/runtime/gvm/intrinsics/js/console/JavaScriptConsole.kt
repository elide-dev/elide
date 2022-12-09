package elide.runtime.gvm.intrinsics.js.console

/**
 * # JavaScript Console
 *
 * Defines a native intrinsic for use as a JavaScript `console` implementation; pipes to the central Elide logging
 * system, with each corresponding log level. See method documentation for more info.
 */
public interface JavaScriptConsole {
  /**
   *
   */
  public fun log(vararg args: Any?)

  /**
   *
   */
  public fun info(vararg args: Any?)

  /**
   *
   */
  public fun warn(vararg args: Any?)

  /**
   *
   */
  public fun error(vararg args: Any?)
}
