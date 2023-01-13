package elide.runtime.js

import elide.runtime.LogLevel


/** Specifies a lightweight [elide.runtime.Logger] implementation for use in JavaScript. */
public data class Logger(
  public val name: String? = null,
): elide.runtime.Logger {
  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = true  // @TODO(sgammon): conditional logging in JS

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean): Unit = when (level) {
    LogLevel.TRACE,
    LogLevel.DEBUG -> console.log(*message.toTypedArray())
    LogLevel.INFO -> console.info(*message.toTypedArray())
    LogLevel.WARN -> console.warn(*message.toTypedArray())
    LogLevel.ERROR -> console.error(*message.toTypedArray())
  }
}
