package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * # JavaScript Console
 *
 * Defines a native intrinsic for use as a JavaScript `console` implementation; pipes to the central Elide logging
 * system, with each corresponding log level. See method documentation for more info.
 */
public interface JavaScriptConsole {
  /**
   * ## `console.log`
   *
   * Emit a log message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * moderated via standard logging system mechanisms (i.e. on the JVM, this is SLF4J). The regular `console.log` method
   * is considered a `DEBUG`-level call.
   *
   * All [args] are converted to a string value before emission; all values are joined by the string `" "`.
   *
   * @param args Arguments to emit as part of this log message.
   */
  @Polyglot public fun log(vararg args: Any?)

  /**
   * ## `console.info`
   *
   * Emit a warning message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * moderated via standard logging system mechanisms (i.e. on the JVM, this is SLF4J). The `console.info` method
   * corresponds to the `INFO` level.
   *
   * All [args] are converted to a string value before emission; all values are joined by the string `" "`.
   *
   * @param args Arguments to emit as part of this log message.
   */
  @Polyglot public fun info(vararg args: Any?)

  /**
   * ## `console.warn`
   *
   * Emit a log message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * moderated via standard logging system mechanisms (i.e. on the JVM, this is SLF4J). The `console.warn` method
   * corresponds to the `WARN` level.
   *
   * All [args] are converted to a string value before emission; all values are joined by the string `" "`.
   *
   * @param args Arguments to emit as part of this log message.
   */
  @Polyglot public fun warn(vararg args: Any?)

  /**
   * ## `console.error`
   *
   * Emit an error message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * moderated via standard logging system mechanisms (i.e. on the JVM, this is SLF4J). The `console.error` method
   * corresponds to the `ERROR` level.
   *
   * All [args] are converted to a string value before emission; all values are joined by the string `" "`.
   *
   * @param args Arguments to emit as part of this log message.
   */
  @Polyglot public fun error(vararg args: Any?)
}
