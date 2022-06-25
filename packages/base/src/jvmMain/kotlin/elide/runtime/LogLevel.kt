package elide.runtime


/** Enumerates log levels on a given platform. */
actual enum class LogLevel {
  TRACE,
  DEBUG,
  WARN,
  INFO,
  ERROR;

  /** @return SLF4J (JVM) equivalent of the current log level. */
  val jvmLevel: org.slf4j.event.Level get() = when (this) {
    TRACE -> org.slf4j.event.Level.TRACE
    DEBUG -> org.slf4j.event.Level.DEBUG
    WARN -> org.slf4j.event.Level.WARN
    INFO -> org.slf4j.event.Level.INFO
    ERROR -> org.slf4j.event.Level.ERROR
  }

  /** @return Whether the provided [logger] enables the current log level. */
  fun isEnabled(logger: org.slf4j.Logger): Boolean = when (this) {
    TRACE -> logger.isTraceEnabled
    DEBUG -> logger.isDebugEnabled
    WARN -> logger.isWarnEnabled
    INFO -> logger.isInfoEnabled
    ERROR -> logger.isErrorEnabled
  }

  /** @return Logger method which should be called for the current log level. */
  internal fun resolve(logger: org.slf4j.Logger): (String) -> Unit = when (this) {
    TRACE -> logger::trace
    DEBUG -> logger::debug
    WARN -> logger::warn
    INFO -> logger::info
    ERROR -> logger::error
  }
}
