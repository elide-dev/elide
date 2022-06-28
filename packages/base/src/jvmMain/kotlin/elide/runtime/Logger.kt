@file:Suppress("unused")

package elide.runtime

/** Describes the interface for loggers shared across platforms. */
public actual interface Logger {
  /**
   * Indicate whether the provided [level] is enabled for the current logger.
   *
   * @param level Log level to check.
   * @return Whether the log level is enabled.
   */
  public actual fun isEnabled(level: LogLevel): Boolean

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
  public actual fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean)

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.TRACE].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  public actual fun trace(vararg message: Any) {
    this.log(LogLevel.TRACE, message.toList())
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.TRACE], assuming trace-level logging
   * is currently enabled.
   *
   * If trace logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun trace(producer: () -> String) {
    if (isEnabled(LogLevel.TRACE)) {
      log(LogLevel.TRACE, listOf(producer.invoke()), levelChecked = true)
    }
  }

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.DEBUG].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  public actual fun debug(vararg message: Any) {
    this.log(LogLevel.DEBUG, message.toList())
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.DEBUG], assuming debug-level logging
   * is currently enabled.
   *
   * If debug logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun debug(producer: () -> String) {
    if (isEnabled(LogLevel.DEBUG)) {
      log(LogLevel.DEBUG, listOf(producer.invoke()), levelChecked = true)
    }
  }

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.INFO].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  public actual fun info(vararg message: Any) {
    this.log(LogLevel.INFO, message.toList())
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.INFO], assuming info-level logging
   * is currently enabled.
   *
   * If info logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun info(producer: () -> String) {
    if (isEnabled(LogLevel.DEBUG)) {
      log(LogLevel.DEBUG, listOf(producer.invoke()), levelChecked = true)
    }
  }

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.WARN].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  public actual fun warn(vararg message: Any) {
    this.log(LogLevel.WARN, message.toList())
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.WARN], assuming warn-level logging
   * is currently enabled.
   *
   * If warn logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun warn(producer: () -> String) {
    if (isEnabled(LogLevel.WARN)) {
      log(LogLevel.WARN, listOf(producer.invoke()), levelChecked = true)
    }
  }

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
  public actual fun warning(vararg message: Any) {
    this.warn(*message)
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.WARN], assuming warn-level logging
   * is currently enabled.
   *
   * If warn logging is not active, the producer will not be dispatched. This method is a thin alias for the equivalent
   * [warn] call.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun warning(producer: () -> String) {
    this.warn(producer)
  }

  /**
   * Log one or more arbitrary [message]s to the console or log, at the level of [LogLevel.ERROR].
   *
   * Each argument is expected to be a string. For automatic string conversion or direct log level control, see [log].
   * To engage in string formatting, or callable log messages, see other variants of this same method.
   *
   * @see info other variants of this method.
   * @param message Set of messages to log in this entry.
   */
  public actual fun error(vararg message: Any) {
    this.log(LogLevel.ERROR, message.toList())
  }

  /**
   * Log the message produced by the provided [producer], at the level of [LogLevel.ERROR], assuming error-level logging
   * is currently enabled.
   *
   * If error logging is not active, the producer will not be dispatched.
   *
   * @param producer Function that produces the message to log.
   */
  public actual fun error(producer: () -> String) {
    if (isEnabled(LogLevel.ERROR)) {
      log(LogLevel.ERROR, listOf(producer.invoke()), levelChecked = true)
    }
  }
}
