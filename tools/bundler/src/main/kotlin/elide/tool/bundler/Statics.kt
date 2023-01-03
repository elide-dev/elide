package elide.tool.bundler

import elide.runtime.Logger
import elide.runtime.Logging

/** Static or constant values for internal use by the bundler CLI. */
internal object Statics {
  /** Main tool logger. */
  internal val logging: Logger = Logging.named("bundler")
}
