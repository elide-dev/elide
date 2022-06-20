package elide.runtime

import elide.runtime.js.ConsoleCallable


/** Enumerates log levels on a given platform. */
actual enum class LogLevel {
  TRACE,
  DEBUG,
  WARN,
  INFO,
  ERROR;

  /** @return Console function implementing the current log level. */
  internal fun resolve(): ConsoleCallable = when (this) {
    TRACE, DEBUG -> console::log
    WARN -> console::warn
    INFO -> console::info
    ERROR -> console::error
  }
}
