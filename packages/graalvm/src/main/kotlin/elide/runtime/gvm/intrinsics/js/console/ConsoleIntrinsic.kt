package elide.runtime.gvm.intrinsics.js.console

import elide.runtime.LogLevel
import elide.runtime.gvm.intrinsics.Intrinsic

/**
 * # Console
 *
 * Defines a native intrinsic for use as a JavaScript `console` implementation; pipes to the central Elide logging
 * system, with each corresponding log level. See method documentation for more info.
 */
@Intrinsic
internal class ConsoleIntrinsic : JavaScriptConsole {
  /**
   *
   */
  @Suppress("UNUSED_PARAMETER") private fun handleLog(level: LogLevel, args: Array<out Any?>) {

  }

  /** @inheritDoc */
  override fun log(vararg args: Any?) = handleLog(LogLevel.DEBUG, args)

  /** @inheritDoc */
  override fun info(vararg args: Any?) = handleLog(LogLevel.INFO, args)

  /** @inheritDoc */
  override fun warn(vararg args: Any?) = handleLog(LogLevel.WARN, args)

  /** @inheritDoc */
  override fun error(vararg args: Any?) = handleLog(LogLevel.ERROR, args)
}
