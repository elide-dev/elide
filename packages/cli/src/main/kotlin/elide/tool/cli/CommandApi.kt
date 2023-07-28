package elide.tool.cli

import java.util.concurrent.atomic.AtomicReference

/**
 * # Command API
 *
 * Defines the external (publicly-accessible) API expected for command implementations.
 */
interface CommandApi {
  /** Observed exit code value; defaults to `0`. */
  val commandResult: AtomicReference<CommandResult>

  /** Shortcut for accessing the exit code for this command; only populated after execution. */
  val exitCode: Int get() = commandResult.get().exitCode
}
