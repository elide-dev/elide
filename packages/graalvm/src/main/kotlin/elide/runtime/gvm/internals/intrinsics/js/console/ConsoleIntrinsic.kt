package elide.runtime.gvm.internals.intrinsics.js.console

import com.google.common.annotations.VisibleForTesting
import elide.annotations.core.Polyglot
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.internals.intrinsics.GuestIntrinsic
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.intrinsics.js.JavaScriptConsole
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * # Console
 *
 * Defines a native intrinsic for use as a JavaScript `console` implementation; pipes to the central Elide logging
 * system, with each corresponding log level. See method documentation for more info.
 */
@Intrinsic(global = ConsoleIntrinsic.GLOBAL_CONSOLE)
internal class ConsoleIntrinsic : JavaScriptConsole, AbstractJsIntrinsic() {
  internal companion object {
    // Global name of the JS console intrinsic.
    const val GLOBAL_CONSOLE = "console"

    // Name of the primary console logger.
    const val loggerName = "gvm:js.console"
  }

  // Logger which receives console calls.
  private val logging: Logger = Logging.named(loggerName)

  // Whether to intercept logs.
  private val intercept: AtomicBoolean = AtomicBoolean(false)

  // Interception logger (mostly for testing).
  private var interceptor: AtomicReference<Logger> = AtomicReference(null)

  // Set an interceptor which receives a mirror of all logging calls.
  @VisibleForTesting internal fun setInterceptor(interceptor: Logger?) {
    if (interceptor != null) {
      this.interceptor.set(interceptor)
      this.intercept.set(true)
    } else {
      this.interceptor.set(null)
      this.intercept.set(false)
    }
  }

  /**
   * Emit a log message to the main logging system, sent to us by the JS `console` intrinsic; log message output is
   * controlled the same way as any other system logger, via the installed logging implementation.
   *
   * @param level Log level to emit this log message at.
   * @param args Set of arguments to format and include with the log message.
   */
  private fun handleLog(level: LogLevel, args: Array<out Any?>) {
    val serializedArgs = args.toList().filterNotNull()
    if (intercept.get()) {
      interceptor.get().log(level, serializedArgs)
    }
    logging.log(level, serializedArgs)
  }

  /** @inheritDoc */
  @Polyglot override fun log(vararg args: Any?) = handleLog(LogLevel.DEBUG, args)

  /** @inheritDoc */
  @Polyglot override fun info(vararg args: Any?) = handleLog(LogLevel.INFO, args)

  /** @inheritDoc */
  @Polyglot override fun warn(vararg args: Any?) = handleLog(LogLevel.WARN, args)

  /** @inheritDoc */
  @Polyglot override fun error(vararg args: Any?) = handleLog(LogLevel.ERROR, args)

  /** @inheritDoc */
  override fun install(bindings: GuestIntrinsic.MutableIntrinsicBindings) {
    // bind self to `console`
    bindings[GLOBAL_CONSOLE] = this
  }
}
