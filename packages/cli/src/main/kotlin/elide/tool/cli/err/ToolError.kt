package elide.tool.cli.err

import elide.tool.cli.ToolCommand

/**
 * TBD.
 */
internal sealed interface ToolError {
  /** Each tool error must carry a unique ID. */
  val id: String

  /** Each tool error must specify an exit code. */
  val exitCode: Int get() = 1

  /** Exception that caused this error. */
  val exception: Throwable? get() = null

  /** Message describing this error. */
  val errMessage: String? get() = null

  /** Whether the exception should halt execution. */
  val fatal: Boolean get() = true

  /** Command that this error relates to, as applicable. */
  val command: ToolCommand? get() = null
}
