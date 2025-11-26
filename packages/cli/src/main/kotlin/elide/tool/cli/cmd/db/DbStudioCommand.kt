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
import java.net.Socket
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.exec.SubprocessRunner.runTask
import elide.tool.exec.SubprocessRunner.stringToTask

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
    private const val STUDIO_API_RESOURCE = "/META-INF/elide/db-studio/api"
    private const val STUDIO_UI_RESOURCE = "/META-INF/elide/db-studio/ui"
    private const val STUDIO_OUTPUT_DIR = ".dev/db-studio"
    private const val STUDIO_INDEX_FILE = "index.ts"
    private val SQLITE_EXTENSIONS = setOf(".db", ".sqlite", ".sqlite3", ".db3")
    private const val MAX_WAIT_SECONDS = 60
    private const val PORT_CHECK_INTERVAL_MS = 1000L
  }

  private suspend fun waitForServerReady(port: Int, name: String, maxWait: Int = MAX_WAIT_SECONDS): Boolean {
    var waited = 0
    while (waited < maxWait) {
      if (isPortListening(port)) {
        return true
      }
      delay(PORT_CHECK_INTERVAL_MS)
      waited++
    }
    return false
  }

  private fun isPortListening(port: Int): Boolean = runCatching {
    Socket("localhost", port).use { true }
  }.getOrDefault(false)

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

  private fun discoverDatabases(baseDir: Path? = null): List<DiscoveredDatabase> {
    val searchDir = baseDir ?: Path.of(System.getProperty("user.dir"))

    // Search current directory and immediate subdirectories
    val currentDir = searchDirectory(searchDir, depth = 0, maxDepth = 0)
    val subDirs = runCatching {
      searchDir.listDirectoryEntries()
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

  private fun extractResourceDirectory(resourcePath: String, targetDir: Path) {
    val classLoader = DbStudioCommand::class.java.classLoader
    val resourceUrl = classLoader.getResource(resourcePath.trimStart('/'))
      ?: error("Resource not found: $resourcePath")

    when (resourceUrl.protocol) {
      "file" -> {
        // Development mode: copy from file system
        val sourcePath = Path.of(resourceUrl.toURI())
        copyDirectory(sourcePath, targetDir)
      }
      "jar" -> {
        // Production mode: extract from JAR
        val jarPath = resourceUrl.path.substringBefore("!")
        val jarFile = java.util.jar.JarFile(Path.of(java.net.URI(jarPath)).toFile())
        val resourcePrefix = resourcePath.trimStart('/') + "/"

        jarFile.entries().asIterator().forEach { entry ->
          if (entry.name.startsWith(resourcePrefix) && !entry.isDirectory) {
            val relativePath = entry.name.removePrefix(resourcePrefix)
            val targetFile = targetDir.resolve(relativePath)
            Files.createDirectories(targetFile.parent)

            jarFile.getInputStream(entry).use { input ->
              Files.copy(input, targetFile, StandardCopyOption.REPLACE_EXISTING)
            }
          }
        }
      }
      else -> error("Unsupported resource protocol: ${resourceUrl.protocol}")
    }
  }

  @Parameters(
    index = "0",
    description = ["Path to SQLite database file or directory for discovery"],
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

    // Extract API server files from embedded resources to .dev/db-studio/api/
    try {
      extractResourceDirectory(STUDIO_API_RESOURCE, apiDir)
    } catch (e: Exception) {
      return CommandResult.err(
        message = "Failed to extract API resources from $STUDIO_API_RESOURCE: ${e.message}"
      )
    }

    // Extract React app UI files from embedded resources to .dev/db-studio/ui/
    try {
      extractResourceDirectory(STUDIO_UI_RESOURCE, uiDir)
    } catch (e: Exception) {
      return CommandResult.err(
        message = "Failed to extract UI resources from $STUDIO_UI_RESOURCE: ${e.message}"
      )
    }

    // Always use databases array - either discover or create from single path
    val databases = if (databasePath == null) {
      val discovered = discoverDatabases()

      if (discovered.isEmpty()) {
        return CommandResult.err(message = "No SQLite databases found in current directory or user data directories. If you're targeting a database file outside of this directory, provide the path as an argument (e.g. \"elide db studio /my/database/path.db\")")
      }

      discovered
    } else {
      val dbPath = Path.of(databasePath!!)

      if (!dbPath.exists()) {
        return CommandResult.err(message = "Path not found: $databasePath")
      }

      when {
        // If it's a directory, discover databases within it
        dbPath.isDirectory() -> {
          val discovered = discoverDatabases(dbPath)

          if (discovered.isEmpty()) {
            return CommandResult.err(message = "No SQLite databases found in directory: $databasePath")
          }

          discovered
        }
        // If it's a file, use it as a single database
        dbPath.isRegularFile() -> {
          listOf(
            DiscoveredDatabase(
              path = dbPath.toAbsolutePath().toString(),
              name = dbPath.name,
              size = dbPath.fileSize(),
              lastModified = dbPath.getLastModifiedTime().toMillis(),
            )
          )
        }
        else -> {
          return CommandResult.err(message = "Path must be a file or directory: $databasePath")
        }
      }
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
      appendLine("Starting Database Studio...")
      appendLine()
    }

    // Start both servers concurrently and wait for them
    return try {
      coroutineScope {
        // Start API server task
        val apiTask = async {
          runTask(stringToTask(
            "elide run $STUDIO_INDEX_FILE",
            shell = elide.tooling.runner.ProcessRunner.ProcessShell.None,
            workingDirectory = apiDir
          ))
        }

        // Start UI server task
        val uiTask = async {
          runTask(stringToTask(
            "elide serve $STUDIO_OUTPUT_DIR/ui",
            shell = elide.tooling.runner.ProcessRunner.ProcessShell.None
          ))
        }

        // Wait for API server to be ready
        if (!waitForServerReady(apiPort, "API Server")) {
          return@coroutineScope CommandResult.err(message = "API Server didn't start within $MAX_WAIT_SECONDS seconds")
        }

        // Wait for UI server to be ready
        if (!waitForServerReady(port, "UI Server")) {
          return@coroutineScope CommandResult.err(message = "UI Server didn't start within $MAX_WAIT_SECONDS seconds")
        }

        output {
          appendLine()
          appendLine("✓ Database Studio is running!")
          appendLine()
          appendLine("  UI:  http://localhost:$port")
          appendLine("  API: http://localhost:$apiPort")
          appendLine()
          appendLine("Press Ctrl+C to stop all servers")
          appendLine()
        }

        // Wait for both servers to complete (they run until interrupted)
        awaitAll(apiTask.await().asDeferred(), uiTask.await().asDeferred())

        CommandResult.success()
      }
    } catch (e: Exception) {
      // Servers were interrupted or failed, clean up gracefully
      CommandResult.success()
    }
  }
}
