package elide.runtime.wasm

import elide.runtime.LogLevel

/** Specifies a lightweight [elide.runtime.Logger] implementation for use in WASM, which proxies to JavaScript. */
public data class Logger(
  public val name: String? = null,
): elide.runtime.Logger {
  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = true  // @TODO(sgammon): conditional logging in JS

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean): Unit {
    // @TODO(sgammon): not yet implemented
  }
}
