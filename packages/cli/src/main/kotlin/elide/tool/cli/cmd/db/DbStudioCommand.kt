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
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

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
    return CommandResult.err(message = "`elide db` requires a subcommand (try: studio)")
  }
}

@Serializable
data class DiscoveredDatabase(
  val path: String,
  val name: String,
  val size: Long,
  val lastModified: Long,
  val isLocal: Boolean = false, // Whether it's in the current working directory
)

@Command(
  name = "studio",
  description = ["Launch database UI for SQLite databases"],
  mixinStandardHelpOptions = true,
)
@Introspected
@ReflectiveAccess
internal class DbStudioCommand : AbstractSubcommand<ToolState, CommandContext>() {

  private companion object {
    private const val STUDIO_API_SOURCE = "samples/db-studio/api"
    private const val STUDIO_UI_SOURCE = "samples/db-studio/ui/dist"
    private const val STUDIO_OUTPUT_DIR = ".db-studio"
    private const val STUDIO_INDEX_FILE = "index.tsx"
    private val SQLITE_EXTENSIONS = setOf(".db", ".sqlite", ".sqlite3", ".db3")
  }

  // Extension function for SQLite detection
  private fun Path.isSqliteDatabase(): Boolean =
    isRegularFile() && SQLITE_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }

  // Extension function to safely convert Path to DiscoveredDatabase
  private fun Path.toDiscoveredDatabase(): DiscoveredDatabase? = runCatching {
    DiscoveredDatabase(
      path = toAbsolutePath().toString(),
      name = name,
      size = fileSize(),
      lastModified = getLastModifiedTime().toMillis(),
      isLocal = true,
    )
  }.getOrNull()

  // Functional version that returns a list
  private fun searchDirectory(
    dir: Path,
    depth: Int,
    maxDepth: Int
  ): List<DiscoveredDatabase> = runCatching {
    dir.listDirectoryEntries().flatMap { file ->
      when {
        file.isSqliteDatabase() -> listOfNotNull(file.toDiscoveredDatabase())
        file.isDirectory() && depth < maxDepth -> searchDirectory(file, depth + 1, maxDepth)
        else -> emptyList()
      }
    }
  }.getOrElse { emptyList() }

  private fun discoverDatabases(): List<DiscoveredDatabase> {
    val cwd = Path.of(System.getProperty("user.dir"))

    // Search current directory and immediate subdirectories
    val currentDir = searchDirectory(cwd, depth = 0, maxDepth = 0)
    val subDirs = runCatching {
      cwd.listDirectoryEntries()
        .filter { it.isDirectory() }
        .flatMap { searchDirectory(it, depth = 1, maxDepth = 1) }
    }.getOrElse { emptyList() }

    return (currentDir + subDirs).sortedByDescending { it.lastModified }
  }

  private fun copyDirectory(sourcePath: Path, targetDir: Path) {
    if (!sourcePath.exists() || !sourcePath.isDirectory()) {
      error("Source directory not found: $sourcePath")
    }

    Files.walk(sourcePath).use { stream ->
      stream.forEach { source ->
        val relative = sourcePath.relativize(source)
        val target = targetDir.resolve(relative.toString())

        when {
          source.isDirectory() -> Files.createDirectories(target)
          else -> {
            Files.createDirectories(target.parent)
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
          }
        }
      }
    }
  }

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
    names = ["--api-port"],
    description = ["Port to run the API server on"],
    defaultValue = "4984",
  )
  internal var apiPort: Int = 4984

  internal var host: String = "localhost"

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val outputDir = Path.of(STUDIO_OUTPUT_DIR)

    // Create organized directory structure
    val apiDir = outputDir.resolve("api")
    val uiDir = outputDir.resolve("ui")
    Files.createDirectories(apiDir)
    Files.createDirectories(uiDir)

    // Copy API server files from samples/db-studio/api to .db-studio/api/
    val apiSource = Path.of(STUDIO_API_SOURCE)
    if (!apiSource.exists() || !apiSource.isDirectory()) {
      return CommandResult.err(
        message = "API source not found at $STUDIO_API_SOURCE. Ensure samples/db-studio/api exists."
      )
    }
    copyDirectory(apiSource, apiDir)

    // Copy React app UI files from samples/db-studio/ui/dist to .db-studio/ui/
    val uiSource = Path.of(STUDIO_UI_SOURCE)
    if (!uiSource.exists() || !uiSource.isDirectory()) {
      return CommandResult.err(
        message = "UI build not found at $STUDIO_UI_SOURCE. Please run 'cd samples/db-studio/ui && npm run build' first."
      )
    }
    copyDirectory(uiSource, uiDir)

    // Always use databases array - either discover or create from single path
    val databases = if (databasePath == null) {
      val discovered = discoverDatabases()

      if (discovered.isEmpty()) {
        return CommandResult.err(message = "No SQLite databases found in current directory or user data directories. If you're targeting a database file outside of this directory, provide the path as an argument (e.g. \"elide db studio /my/database/path.db\")")
      }

      discovered
    } else {
      val dbFile = Path.of(databasePath!!)

      if (!dbFile.exists()) {
        return CommandResult.err(message = "Database file not found: $databasePath")
      }

      // Create a single-item list with the specified database
      listOf(
        DiscoveredDatabase(
          path = dbFile.toAbsolutePath().toString(),
          name = dbFile.name,
          size = dbFile.fileSize(),
          lastModified = dbFile.getLastModifiedTime().toMillis(),
          isLocal = true,
        )
      )
    }

    // Generate config.ts with port and databases
    val json = Json { prettyPrint = true }
    val databasesJson = json.encodeToString(databases)

    val configContent = buildString {
      appendLine("// Auto-generated configuration by DbStudioCommand")
      appendLine("// Do not edit this file manually")
      appendLine()
      appendLine("export default {")
      appendLine("  port: $apiPort,")
      appendLine("  databases: $databasesJson")
      appendLine("};")
    }

    val configFile = apiDir.resolve("config.ts")
    configFile.writeText(configContent)

    output {
      appendLine("Database Studio files generated in: ${outputDir.toAbsolutePath()}")
      appendLine()
      appendLine("To start the Database Studio:")
      appendLine()
      appendLine("  Terminal 1 (API Server on port $apiPort):")
      appendLine("    cd ${apiDir.toAbsolutePath()}")
      appendLine("    elide serve $STUDIO_INDEX_FILE")
      appendLine()
      appendLine("  Terminal 2 (UI Server on port 8080):")
      appendLine("    cd ${uiDir.toAbsolutePath()}")
      appendLine("    elide serve .")
      appendLine()
      appendLine("Then open: http://localhost:8080")
      appendLine()
    }

    return CommandResult.success()
  }
}
