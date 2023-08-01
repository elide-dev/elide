package elide.tool.cli

/** Enumerates available sub-commands for the Elide command-line tool. */
@Suppress("unused")
internal enum class ToolCommand constructor(internal val commandName: String) {
  /** Root tool command (i.e. no sub-command). */
  ROOT("elide"),

  /** Tool to gather info about an application or development environment. */
  INFO("info"),

  /** Tool to run code in a guest language VM. */
  RUN("run"),
}
