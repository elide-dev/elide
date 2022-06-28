package elide.runtime.js

import elide.runtime.LogLevel


/** Specifies a lightweight [elide.runtime.Logger] implementation for use in JavaScript. */
public data class Logger(
  public val name: String? = null,
): elide.runtime.Logger {
  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean {
    TODO("Not yet implemented")
  }

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean) {
    TODO("Not yet implemented")
  }
}
