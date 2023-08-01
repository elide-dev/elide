package elide.tool.cli

/**
 * # Command Result
 *
 * Defines the structure of a unary command invocation result, either as a [CommandResult.Success], or a
 * [CommandResult.Error]; if we are modeling a non-successful exit, an exit code is additionally defined which should be
 * used to communicate the underlying error.
 *
 * If no exit code is defined and we are exiting in an error state, [DEFAULT_ERROR_EXIT_CODE] is used (`-1`).
 */
sealed interface CommandResult {
  companion object {
    /** Default exit code for errors. */
    const val DEFAULT_ERROR_EXIT_CODE: Int = -1

    /** Default error message when no other message is available. */
    const val DEFAULT_ERROR_MESSAGE = "An unknown error occurred"

    /** @return Successful command execution result. */
    fun success(): CommandResult = Success.of()

    /**
     * Create an error result.
     *
     * @param exitCode Exit code to use.
     * @param message Message to show (optional).
     * @return Command result indicating the provided error.
     */
    fun err(
      exitCode: Int = DEFAULT_ERROR_EXIT_CODE,
      message: String? = null,
    ): CommandResult = Error.of(exitCode, message)
  }

  /** Whether the command exited with a non-error state. */
  val ok: Boolean

  /** Exit code to communicate for this result. */
  val exitCode: Int

  /** Defines a successful command run result; always uses an exit code of `0`. */
  data object Success : CommandResult {
    override val ok: Boolean get() = true
    override val exitCode: Int get() = 0

    /** */
    @JvmStatic fun of(): CommandResult = Success
  }

  /**
   * Defines an error command run result.
   *
   * @param exitCode Exit code to communicate for this error; if none is provided, defaults to `-1`
   *   ([DEFAULT_ERROR_EXIT_CODE]).
   * @param message Message to communicate for this error; if none is provided, defaults to [DEFAULT_ERROR_MESSAGE].
   */
  data class Error internal constructor(override val exitCode: Int, val message: String) : CommandResult {
    override val ok: Boolean get() = false

    internal companion object {
      /** */
      @JvmStatic fun of(
        exitCode: Int = DEFAULT_ERROR_EXIT_CODE,
        message: String? = null,
      ): CommandResult = Error(exitCode, message ?: DEFAULT_ERROR_MESSAGE)
    }
  }
}
