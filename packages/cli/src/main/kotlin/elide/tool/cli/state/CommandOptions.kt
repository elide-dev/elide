package elide.tool.cli.state

/**
 * # Command Options
 *
 * Carries information as part of [CommandState] about the original invocation flags from the command line, along with
 * (very basic) interpreted values. Flags such as [debug], [verbose], and so on, which control output, are modeled here
 * so that their values can be accessed statically and from command execution context.
 *
 * Command options should not be created directly.
 */
@JvmInline value class CommandOptions private constructor (private val info: CommandLineInvocation) {
  /**
   * ## Command Line Invocation
   *
   * Describes information about the command line invocation which ran this CLI execution. Parameters provided here are
   * not validated.
   *
   * @param args Original command-line arguments.
   * @param debug Whether debug mode is active.
   * @param verbose Whether verbose mode is active.
   * @param quiet Whether quiet mode is active.
   * @param pretty Whether pretty output mode is active.
   */
  internal data class CommandLineInvocation(
    val args: List<String>,
    val debug: Boolean,
    val verbose: Boolean,
    val quiet: Boolean,
    val pretty: Boolean,
  )

  companion object {
    /**
     * Spawn a [CommandOptions] set from the provided inputs.
     *
     * @param args Original command-line arguments.
     * @param debug Whether debug mode is active.
     * @param verbose Whether verbose mode is active.
     * @param quiet Whether quiet mode is active.
     * @param pretty Whether pretty output mode is active.
     * @return Command options.
     */
    @JvmStatic fun of(
      args: List<String>,
      debug: Boolean,
      verbose: Boolean,
      quiet: Boolean,
      pretty: Boolean
    ): CommandOptions = CommandOptions(
      CommandLineInvocation(
        args,
        debug = debug,
        verbose = verbose,
        quiet = quiet,
        pretty = pretty,
      )
    )
  }

  /** Original command line invocation arguments. */
  val args: List<String> get() = info.args

  /** Whether debug mode is active. */
  val debug: Boolean get() = info.debug

  /** Whether verbose mode is active. */
  val verbose: Boolean get() = info.verbose

  /** Whether quiet mode is active. */
  val quiet: Boolean get() = info.quiet

  /** Whether pretty output mode is active. */
  val pretty: Boolean get() = info.pretty
}
