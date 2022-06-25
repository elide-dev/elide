package elide.server.runtime

import elide.runtime.Logger
import elide.runtime.Logging
import jakarta.inject.Singleton


/**
 *
 */
@Singleton open class UncaughtExceptionHandler: Thread.UncaughtExceptionHandler {
  // Root logger.
  private val logging: Logger = Logging.root()

  override fun uncaughtException(thread: Thread, e: Throwable) {
    // not yet implemented
    logging.error(
      "Encountered critical uncaught error (thread: '${thread.name}'): '${e.message ?: "NO_MESSAGE"}'",
      e
    )
  }
}
