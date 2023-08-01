package elide.runtime.gvm.internals.intrinsics.js.console

import java.time.Instant
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.LogLevel
import elide.runtime.Logger
import elide.runtime.Logging
import elide.runtime.gvm.internals.intrinsics.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.internals.intrinsics.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.js.JavaScriptConsole
import elide.vm.annotations.Polyglot
import org.graalvm.polyglot.Value as GuestValue

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
    const val GLOBAL_CONSOLE = "Console"

    /** Base64 symbol. */
    private val CONSOLE_SYMBOL = GLOBAL_CONSOLE.asJsSymbol()

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
  internal fun setInterceptor(interceptor: Logger?) {
    this.interceptor.set(interceptor)
    this.intercept.set(interceptor != null)
  }

  /**
   * Format an object returned from a guest context which evaluates as a "meta-object;" this is referred to in JVM
   * circumstances as "class" or "type."
   *
   * If no special formatting can be applied to the object, the value is returned verbatim.
   *
   * @param obj Object to format.
   * @return Formatted value to emit, or the original object if no formatting was applied.
   */
  @Suppress("unused", "UNUSED_PARAMETER")
  internal fun formatMetaObject(obj: GuestValue): Any = obj

  /**
   * Format a guest exception received via a log message call; this involves checking to see if it declares a message,
   * and using that if so, or otherwise delegating to the error's string representation method.
   *
   * If no special formatting can be applied to the [err], the value is returned verbatim.
   *
   * @param err Error to format and return.
   * @return Formatted value to emit, or the original object if no formatting was applied.
   */
  @Suppress("UNUSED_PARAMETER")
  private fun formatGuestException(err: GuestValue): Any {
    TODO("not yet implemented")
  }

  /**
   * Format a guest exception received via a log message call; this involves checking to see if it declares a message,
   * and using that if so, or otherwise delegating to the error's string representation method.
   *
   * If no special formatting can be applied to the [err], the value is returned verbatim.
   *
   * @param err Error to format and return.
   * @return Formatted value to emit, or the original object if no formatting was applied.
   */
  private fun formatGuestError(err: Map<*, *>): Any {
    return if (err.containsKey("message")) {
      err["message"] as? String ?: "unknown error"
    } else {
      err
    }
  }

  /**
   * Format an object that originates from the host VM, and should therefore be some recognized Java type or class; if
   * the object cannot be recognized, `toString` is called and returned.
   *
   * @param obj Host object to format and return.
   * @return Formatted value to emit.
   */
  private fun formatHostObject(obj: GuestValue): Any = when (val value = obj.asHostObject<Any?>()) {
    null -> "null"
    else -> value.toString()
  }

  /**
   * Format a log message component ([arg]) into a string which is suitable to be emitted to the console; if the object
   * is an error or another special type, it will be formatted as such.
   *
   * If no special formatting can be applied to the [arg], the value is returned verbatim.
   *
   * @param arg Log argument which should be formatted.
   * @return Formatted value to emit.
   */
  internal fun formatLogComponent(arg: Any?): Any = when (arg) {
    // print `null` for null values
    null -> "null"

    // if it's already a `String`, it doesn't need to be formatted.
    is String -> arg

    // for primitives, just stringify
    is Boolean, is Int, is Long, is Double, is Float -> arg.toString()

    // if the arg originates as a guest value, we can interrogate it to determine its type.
    is GuestValue -> when {
      // primitive / most likely log types
      arg.isNull -> "null"
      arg.isString -> arg.asString()
      arg.isBoolean -> arg.asBoolean().toString()
      arg.isException -> formatGuestException(arg)
      arg.isNumber -> when {
        arg.fitsInShort() -> arg.asShort().toString()
        arg.fitsInInt() -> arg.asInt().toString()
        arg.fitsInLong() -> arg.asLong().toString()
        arg.fitsInFloat() -> arg.asFloat().toString()
        arg.fitsInDouble() -> arg.asDouble().toString()
        else -> "{unknown number value}"
      }

      // complex types
      arg.isHostObject -> formatHostObject(arg)
      arg.isNativePointer -> "NativePointer(${arg.asNativePointer()})"
      arg.isIterator -> "Iterator(...)"
      arg.isMetaObject -> {}
      arg.isBufferWritable -> {}

      // temporal types
      arg.isDate -> arg.asDate().toString()
      arg.isTime -> arg.asTime().toString()
      arg.isDuration -> arg.asDuration().toString()
      arg.isInstant -> arg.asInstant().toString()
      arg.isTimeZone -> arg.asTimeZone().toString()

      // can't identify the value further
      else -> arg
    }

    // if the arg expresses as a map and has a `message` property, it is an error-like type.
    is Map<*, *> -> when {
      arg.containsKey("message") -> formatGuestError(arg)
      else -> arg
    }

    // format temporal types in standard terms
    is Instant, is kotlinx.datetime.Instant, is Date -> arg.toString()

    // no special formatting can be applied to this type, so return it directly.
    else -> arg
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
    logging.log(level, serializedArgs.map(this::formatLogComponent))
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
    bindings[CONSOLE_SYMBOL] = this
  }
}
