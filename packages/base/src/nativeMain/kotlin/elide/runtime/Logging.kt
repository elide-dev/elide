package elide.runtime


/** Describes an expected class which is able to produce [Logger] instances as a factory. */
public actual class Logging {
  /**
   * Acquire a [Logger] for the given logger [name], which can be any identifying string; in JVM circumstances, the full
   * class name of the subject which is sending the logs is usually used.
   *
   * @param name Name of the logger to create and return.
   * @return Desired logger.
   */
  public actual fun logger(name: String): Logger {
    TODO("Not yet implemented")
  }

  /**
   * Acquire a root [Logger] which is unnamed, or uses an empty string value (`""`) for the logger name.
   *
   * @return Root logger.
   */
  public actual fun logger(): Logger {
    TODO("Not yet implemented")
  }
}
