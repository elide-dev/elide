package elide.tool.cli

import elide.runtime.Logger
import elide.runtime.Logging
import java.util.concurrent.atomic.AtomicReference

/** Internal static tools and utilities used across the Elide CLI. */
internal object Statics {
  /** Main tool logger. */
  internal val logging: Logger by lazy {
    Logging.named("tool")
  }

  /** Invocation args. */
  internal val args: AtomicReference<List<String>> = AtomicReference(emptyList())
}
