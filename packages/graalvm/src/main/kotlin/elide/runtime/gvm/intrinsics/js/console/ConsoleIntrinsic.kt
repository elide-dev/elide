package elide.runtime.gvm.intrinsics.js.console

import elide.annotations.core.Polyglot
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.intrinsics.Intrinsic
import elide.runtime.gvm.intrinsics.js.AbstractJsIntrinsic

/**
 * # Console
 *
 * Defines a native intrinsic for use as a JavaScript `console` implementation; pipes to the central Elide logging
 * system, with each corresponding log level. See method documentation for more info.
 */
@Intrinsic(global = "console") internal class ConsoleIntrinsic : JavaScriptConsole, AbstractJsIntrinsic() {
  private companion object {
    // Name of the primary console logger.
    const val loggerName = "gvm:js.console"
  }

  // Logger which receives console calls.
  private val logging: Logger = Logging.named(loggerName)

  /**
   * Emit a log message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * controlled the same way as any other system logger, via the installed logging implementation.
   *
   * @param level Log level to emit this log message at.
   * @param args Set of arguments to format and include with the log message.
   */
  private fun handleLog(level: LogLevel, args: Array<out Any?>) {
    logging.log(level, args.toList().filterNotNull())
  }

  /** @inheritDoc */
  @Polyglot override fun log(vararg args: Any?) = handleLog(LogLevel.DEBUG, args)

  /** @inheritDoc */
  @Polyglot override fun info(vararg args: Any?) = handleLog(LogLevel.INFO, args)

  /** @inheritDoc */
  @Polyglot override fun warn(vararg args: Any?) = handleLog(LogLevel.WARN, args)

  /** @inheritDoc */
  @Polyglot override fun error(vararg args: Any?) = handleLog(LogLevel.ERROR, args)
}
