/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.tool.cli.cmd.db

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Files
import java.nio.file.Path
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.exec.SubprocessRunner.delegateTask
import elide.tool.exec.SubprocessRunner.subprocess
import elide.tool.exec.which

@Command(
  name = "db",
  description = ["Database tools for Elide projects."],
  mixinStandardHelpOptions = true,
  subcommands = [DbStudioCommand::class],
)
@Introspected
@ReflectiveAccess
internal class DbCommand : AbstractSubcommand<ToolState, CommandContext>() {
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
internal class DbStudioCommand : AbstractSubcommand<ToolState, CommandContext>() {

  @Parameters(
    index = "0",
    description = ["Path to SQLite database file"],
    arity = "0..1",
    paramLabel = "DATABASE_PATH",
  )
  internal var databasePath: String? = null

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
    val dbPath = databasePath ?: return err("Database path is required")

    val dbFile = Path.of(dbPath)
    if (!Files.exists(dbFile)) {
      return err("Database file not found: $dbPath")
    }

    val absoluteDbPath = dbFile.toAbsolutePath().toString()

    val npxPath = which(Path.of("npx")) ?: return err("npx not found. Please install Node.js.")

    val task = subprocess(npxPath) {
      args.add("@outerbase/studio")
      args.add("--port")
      args.add(port.toString())
      args.add(absoluteDbPath)
    }

    output {
      appendLine("Starting database UI on http://$host:$port")
      appendLine("Database: $absoluteDbPath")
      appendLine()
      appendLine("Press Ctrl+C to stop the server")
    }

    return delegateTask(task)
  }
}
