package elide.tool.cli

/**
 * TBD.
 */
internal sealed class ToolState(
  val output: OutputSettings = OutputSettings.DEFAULTS,
) {
  /**
   * Output settings for the tool.
   *
   */
  data class OutputSettings(
    val verbose: Boolean = false,
    val quiet: Boolean = false,
    val pretty: Boolean = true,
    val stderr: Boolean = false,
  ) {
    internal companion object {
      // Default output settings.
      val DEFAULTS: OutputSettings = OutputSettings()
    }
  }

  /** Empty/default tool state. */
  private class Empty : ToolState()

  companion object {
    /** Default `Empty` singleton. */
    internal val EMPTY: ToolState = Empty()
  }
}
