package elide.tool.cli.err

import elide.tool.cli.ToolCommand

/** Root exception for known exceptions originating from the Elide command line tool. */
@Suppress("unused")
internal abstract class AbstractToolError(
  override val command: ToolCommand? = null,
  private val errId: String? = null,
  private val case: ToolErrorCase<*>? = null,
  message: String? = null,
  cause: Throwable? = null,
) : ToolError, RuntimeException(message, cause) {
  /** Case-only constructor. */
  constructor(case: ToolErrorCase<*>) : this(
    case = case,
    errId = case.id,
  )

  /** Message-only constructor. */
  constructor(message: String) : this(
    message = message,
    cause = null,
  )

  /** Cause-only constructor. */
  constructor(cause: Throwable) : this(
    message = null,
    cause = cause,
  )

  /** @inheritDoc */
  override val id: String get() = errId ?: "GENERIC"

  /** @inheritDoc */
  override val exception: Throwable? get() = cause

  /** @inheritDoc */
  override val errMessage: String? get() = message

  /** Implements a generic error case. */
  internal class Generic internal constructor(
    command: ToolCommand? = null,
    message: String? = null,
    cause: Throwable? = null,
  ) : AbstractToolError(
    command = command,
    errId = null,
    case = null,
    message = message,
    cause = cause,
  )

  /** Implements a known error case, defined from a [ToolErrorCase] implementation. */
  internal class Known internal constructor(
    case: ToolErrorCase<*>,
    command: ToolCommand? = null,
    additionalMessage: String? = null,
    cause: Throwable? = null,
  ) : AbstractToolError(
    command = command,
    errId = case.id,
    case = case,
    message = "Error: ${case.id}. ${case.errMessage ?: ""} ${additionalMessage ?: ""}".trim(),
    cause = cause,
  )

  internal companion object {
    /** @return Generic tool exception. */
    @JvmStatic fun generic(
      message: String? = null,
      cause: Throwable? = null,
      command: ToolCommand? = null,
    ) = Generic(command, message, cause)

    /** @return Exception for a case-enumerated error. */
    @JvmStatic fun <T : Enum<T>> forCase(
      case: ToolErrorCase<T>,
      command: ToolCommand? = null,
      additionalMessage: String? = null,
      cause: Throwable? = null,
    ) = Known(
      case,
      command = command,
      additionalMessage = additionalMessage,
      cause = cause,
    )
  }
}
