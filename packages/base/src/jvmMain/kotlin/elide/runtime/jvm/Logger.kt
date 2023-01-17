package elide.runtime.jvm

import elide.runtime.LogLevel

/** JVM implementation of a cross-platform Elide [elide.runtime.Logger] which wraps an [org.slf4j.Logger]. */
public class Logger (private val logger: org.slf4j.Logger): elide.runtime.Logger {
  // Format a list of values for emission as part of a log message.
  private fun formatLogLine(message: List<Any>): String {
    val builder = StringBuilder()
    val portions = message.size
    message.forEachIndexed { index, value ->
      builder.append(value)
      if (index < portions - 1)
        builder.append(" ")
    }
    return builder.toString()
  }

  /** @inheritDoc */
  override fun isEnabled(level: LogLevel): Boolean = if (System.getProperty("elide.test", "false") == "true") {
    true
  } else {
    level.isEnabled(logger)
  }

  /** @inheritDoc */
  override fun log(level: LogLevel, message: List<Any>, levelChecked: Boolean) {
    val enabled = levelChecked || isEnabled(level)
    if (enabled) {
      level.resolve(logger).invoke(
        formatLogLine(message)
      )
    }
  }
}
