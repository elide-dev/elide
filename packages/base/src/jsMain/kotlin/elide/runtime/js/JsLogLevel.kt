package elide.runtime.js

import elide.runtime.LogLevel


/** @return Console function implementing the current log level. */
internal fun LogLevel.resolve(): ConsoleCallable = when (this) {
  LogLevel.TRACE, LogLevel.DEBUG -> console::log
  LogLevel.WARN -> console::warn
  LogLevel.INFO -> console::info
  LogLevel.ERROR -> console::error
}
