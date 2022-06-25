package elide.server

import elide.runtime.Logging
import kotlin.reflect.KClass


/** Abstract definition of handler controllers. Provides shared handler logic. */
@Suppress("unused") abstract class AppController {
  companion object {
    /** @return Logger created for the [AppController] subclass in question. */
    @JvmStatic fun <C: AppController> logger(subclass: KClass<C>) = Logging.of(subclass)
  }

  // Private logger for AppController-level logs.
  private val logging = Logging.of(AppController::class)

  /** @return Whether this page is cacheable across requests. */
  fun cacheable(): Boolean = false
}
