package elide.tool.cli

import java.util.concurrent.atomic.AtomicReference
import elide.runtime.Logger
import elide.runtime.Logging

/** Internal static tools and utilities used across the Elide CLI. */
internal object Statics {
  /** Main tool logger. */
  internal val logging: Logger by lazy {
    Logging.named("tool")
  }

  /** Server tool logger. */
  internal val serverLogger: Logger by lazy {
    Logging.named("tool:server")
  }

  /** Invocation args. */
  internal val args: AtomicReference<List<String>> = AtomicReference(emptyList())
}
