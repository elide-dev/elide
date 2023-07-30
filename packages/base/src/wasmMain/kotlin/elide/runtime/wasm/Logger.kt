package elide.runtime.wasm

import elide.runtime.LogLevel


/** Specifies a lightweight [elide.runtime.Logger] implementation for use in pure WASM. */
public data class Logger(
  public val name: String? = null,
): elide.runtime.Logger {
  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = true  // @TODO(sgammon): conditional logging in WASM

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean): Unit = when (level) {
    LogLevel.TRACE,
    LogLevel.DEBUG,
    LogLevel.INFO,
    LogLevel.WARN,
    LogLevel.ERROR -> println(message.toTypedArray())
  }
}
