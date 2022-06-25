package elide.runtime



/** Describes the interface for loggers shared across platforms. */
@Suppress("unused") expect interface Logger {
  /**
   * Indicate whether the provided [level] is enabled for the current logger.
   *
   * @param level Log level to check.
   * @return Whether the log level is enabled.
   */
  fun isEnabled(level: LogLevel): Boolean

  /**
   * Log one or more arbitrary [message]s to the console or log, depending on the current platform.
   *
   * Each argument will be converted to a string, and all strings will be concatenated together into one log message.
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @param level Level that this log is being logged at.
   * @param message Set of messages to log in this entry.
   * @param levelChecked Whether the log level has already been checked.
   */
  fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean = false)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.TRACE].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun trace(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.TRACE] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun trace(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.TRACE], assuming trace-level logging
   * is currently enabled.
   *
   * If trace logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  open fun trace(producer: () -> String)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.DEBUG].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun debug(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.DEBUG] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun debug(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.DEBUG], assuming debug-level logging
   * is currently enabled.
   *
   * If debug logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  open fun debug(producer: () -> String)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.INFO].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun info(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.INFO] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun info(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.INFO], assuming info-level logging
   * is currently enabled.
   *
   * If info logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  open fun info(producer: () -> String)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.WARN].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun warn(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.WARN] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun warn(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.WARN], assuming warn-level logging
   * is currently enabled.
   *
   * If warn logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  open fun warn(producer: () -> String)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.WARN].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * This method is a thin alias for the equivalent [warn] call.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun warning(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.WARN] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * This method is a thin alias for the equivalent [warn] call.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun warning(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.WARN], assuming warn-level logging
   * is currently enabled.
   *
   * If warn logging is not active, the producer will not be dispatched. This method is a thin alias for the equivalent
   * [warn] call.
   *
   * @param producer Function that produces the message to log.
   */
  open fun warning(producer: () -> String)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.ERROR].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  open fun error(vararg message: String)

  /**
   * Log the provided [message] to the console at the [LogLevel.ERROR] level along with any provided [context] values,
   * each of which will be string formatted before being concatenated and logged or printed.
   *
   * @param message Message to emit to the log as a prefix for [context] values.
   * @param context Additional context values, potentially strings, to string-format and concatenate.
   */
  open fun error(message: String, vararg context: Any)

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.ERROR], assuming error-level logging
   * is currently enabled.
   *
   * If error logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  open fun error(producer: () -> String)
}
