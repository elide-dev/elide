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
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
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
import kotlinx.serialization.encodeToString
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
    private const val STUDIO_RESOURCE_PATH = "db-studio"
    private const val STUDIO_OUTPUT_DIR = ".db-studio"
    private const val STUDIO_INDEX_FILE = "index.tsx"
    private val SQLITE_EXTENSIONS = setOf(".db", ".sqlite", ".sqlite3", ".db3")
  }

  private fun discoverDatabases(): List<DiscoveredDatabase> {
    val databases = mutableListOf<DiscoveredDatabase>()

    val cwd = Path.of(System.getProperty("user.dir"))

    searchDirectory(cwd, databases, depth = 0, maxDepth = 0, isLocal = true)

    try {
      cwd.listDirectoryEntries()
        .filter { it.isDirectory() }
        .forEach { subDir ->
          searchDirectory(subDir, databases, depth = 1, maxDepth = 1, isLocal = true)
        }
    } catch (e: Exception) {

    }

    val userHome = Path.of(System.getProperty("user.home"))
    val osName = System.getProperty("os.name").lowercase()

    val userDataDirs = when {
      osName.contains("mac") -> listOf(
        userHome.resolve("Library/Application Support")
      )
      osName.contains("win") -> listOf(
        Path.of(System.getenv("APPDATA") ?: userHome.resolve("AppData/Roaming").toString())
      )
      else -> listOf( // Linux/Unix
        userHome.resolve(".local/share")
      )
    }

    userDataDirs.forEach { dir ->
      if (dir.exists() && dir.isDirectory()) {
        searchDirectory(dir, databases, depth = 0, maxDepth = 1, isLocal = false)
      }
    }

    return databases.sortedWith(
      compareByDescending<DiscoveredDatabase> { it.isLocal }
        .thenByDescending { it.lastModified }
    )
  }

  private fun searchDirectory(
    dir: Path,
    databases: MutableList<DiscoveredDatabase>,
    depth: Int,
    maxDepth: Int,
    isLocal: Boolean
  ) {
    try {
      dir.listDirectoryEntries().forEach { file ->
        when {
          file.isRegularFile() && SQLITE_EXTENSIONS.any { file.name.endsWith(it, ignoreCase = true) } -> {
            try {
              databases.add(
                DiscoveredDatabase(
                  path = file.toAbsolutePath().toString(),
                  name = file.name,
                  size = file.fileSize(),
                  lastModified = file.getLastModifiedTime().toMillis(),
                  isLocal = isLocal,
                )
              )
            } catch (e: Exception) {
              // Silently ignore files we can't read
            }
          }
          file.isDirectory() && depth < maxDepth -> {
            searchDirectory(file, databases, depth + 1, maxDepth, isLocal)
          }
        }
      }
    } catch (e: Exception) {
      // Silently ignore permission errors
    }
  }

  private fun copyResourceDirectory(resourcePath: String, targetDir: Path) {
    val resourceUrl = this::class.java.classLoader.getResource(resourcePath)
      ?: error("Resource not found: $resourcePath")

    val uri = resourceUrl.toURI()
    val fileSystem = when (uri.scheme) {
      "jar" -> FileSystems.newFileSystem(uri, emptyMap<String, Any>())
      else -> null
    }

    fileSystem.use {
      val sourcePath = fileSystem?.getPath(resourcePath) ?: Path.of(uri)

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

  internal var host: String = "localhost"

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val outputDir = Path.of(STUDIO_OUTPUT_DIR)
    copyResourceDirectory(STUDIO_RESOURCE_PATH, outputDir)

    val indexFile = outputDir.resolve(STUDIO_INDEX_FILE)
    val baseContent = indexFile.readText()

    // Handle database selection mode vs direct database path
    val processedContent = if (databasePath == null) {
      // Discovery mode: find available databases
      val discovered = discoverDatabases()

      if (discovered.isEmpty()) {
        return CommandResult.err(message = "No SQLite databases found in current directory or user data directories")
      }

      val json = Json { prettyPrint = false }
      val databasesJson = json.encodeToString(discovered)

      baseContent
        .replace("\"__DB_PATH__\"", "null")
        .replace("__PORT__", port.toString())
        .replace("\"__DATABASES__\"", databasesJson)
        .replace("__SELECTION_MODE__", "true")
    } else {
      // Direct path mode: use provided database
      val dbFile = Path.of(databasePath!!)

      if (!dbFile.exists()) {
        return CommandResult.err(message = "Database file not found: $databasePath")
      }

      val absoluteDbPath = dbFile.toAbsolutePath().toString()

      baseContent
        .replace("\"__DB_PATH__\"", "\"$absoluteDbPath\"")
        .replace("__PORT__", port.toString())
        .replace("\"__DATABASES__\"", "[]")
        .replace("__SELECTION_MODE__", "false")
    }

    indexFile.writeText(processedContent)

    output {
      appendLine("Generated database studio in: ${outputDir.toAbsolutePath()}")
      appendLine()
      appendLine("To start the database UI, run:")
      appendLine("elide serve $STUDIO_OUTPUT_DIR/$STUDIO_INDEX_FILE")
      appendLine()
      appendLine("Then open: http://$host:$port")
    }

    return CommandResult.success()
  }
}
