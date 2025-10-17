package elide.tool.cli.cmd.db

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState

@Command(
  name = "db",
  description = ["Database tools for Elide projects."],
  mixinStandardHelpOptions = true,
  subcommands = [DbStudioCommand::class],
)
@Introspected
@ReflectiveAccess
internal class DbCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    return err("`elide db` requires a subcommand (try: studio)")
  }
}

@Command(
  name = "studio",
  description = ["Launch database UI for SQLite databases"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class DbStudioCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {

  @Option(
    names = ["--port", "-p"],
    description = ["Port to run the database UI on"],
    defaultValue = "4983",
  )
  internal var port: Int = 4983

  @Option(
    names = ["--host"],
    description = ["Host to bind the database UI to"],
    defaultValue = "localhost",
  )
  internal var host: String = "localhost"

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    output {
      append("Starting database UI on http://$host:$port")
    }

    // TODO

    return success()
  }
}
