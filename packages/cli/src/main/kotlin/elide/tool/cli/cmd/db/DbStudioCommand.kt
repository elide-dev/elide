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
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.rendering.TextStyles
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.exec.SubprocessRunner.runTask
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tooling.cli.Statics

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
  val id: String,
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

  private suspend fun <T> CommandContext.executeStep(
    stepName: String,
    block: suspend () -> T
  ): Result<T> {
    val result = runCatching { block() }

    output {
      append(TextStyles.dim("  $stepName... "))
      if (result.isSuccess) {
        append(TextColors.green("✓"))
      } else {
        append(TextColors.red("✗"))
      }
    }

    return result
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

  // Generate a stable database identifier: {filename}-{hash}
  private fun generateDatabaseId(dbPath: Path): String {
    val filename = dbPath.name
      .removeSuffix(".db")
      .removeSuffix(".sqlite")
      .removeSuffix(".sqlite3")
      .removeSuffix(".db3")

    val hash = hashFilePath(dbPath)
    return "${filename}-${hash}"
  }

  // Create hash from absolute path using SHA-256
  private fun hashFilePath(dbPath: Path): String {
    val absolutePath = dbPath.toAbsolutePath().toString()
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(absolutePath.toByteArray(Charsets.UTF_8))
    return hashBytes.take(4).joinToString("") { "%02x".format(it) }
  }

  // Extension function to safely convert Path to DiscoveredDatabase
  private fun Path.toDiscoveredDatabase(): DiscoveredDatabase? = runCatching {
    DiscoveredDatabase(
      id = generateDatabaseId(this),
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

    val allDatabases = (currentDir + subDirs).sortedByDescending { it.lastModified }

    // Handle ID collisions by appending numeric suffix
    val seenIds = mutableMapOf<String, Int>()
    return allDatabases.map { db ->
      val count = seenIds.getOrDefault(db.id, 0)
      seenIds[db.id] = count + 1
      if (count > 0) db.copy(id = "${db.id}-${count}") else db
    }
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

  // API server always runs on port 4984
  private val apiPort = 4984

  internal var host: String = "localhost"

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val outputDir = Path.of(STUDIO_OUTPUT_DIR)

    // Create organized directory structure
    val apiDir = outputDir.resolve("api")
    val uiDir = outputDir.resolve("ui")
    Files.createDirectories(apiDir)
    Files.createDirectories(uiDir)

    output {
      appendLine(TextStyles.bold("Generating DB Studio files..."))
    }

    // Extract API server files from embedded resources to .dev/db-studio/api/
    executeStep("Extracting API resources") {
      extractResourceDirectory(STUDIO_API_RESOURCE, apiDir)
    }.onFailure { e ->
      return CommandResult.err(
        message = "Failed to extract API resources from $STUDIO_API_RESOURCE: ${e.message}"
      )
    }

    // Extract React app UI files from embedded resources to .dev/db-studio/ui/
    executeStep("Extracting UI resources") {
      extractResourceDirectory(STUDIO_UI_RESOURCE, uiDir)
    }.onFailure { e ->
      return CommandResult.err(
        message = "Failed to extract UI resources from $STUDIO_UI_RESOURCE: ${e.message}"
      )
    }

    // Discover databases
    val databases = executeStep("Discovering databases") {
      if (databasePath == null) {
        discoverDatabases()
      } else {
        val dbPath = Path.of(databasePath!!)

        if (!dbPath.exists()) {
          error("Path not found: $databasePath")
        }

        when {
          // If it's a directory, discover databases within it
          dbPath.isDirectory() -> discoverDatabases(dbPath)
          // If it's a file, use it as a single database
          dbPath.isRegularFile() -> {
            listOf(
              DiscoveredDatabase(
                id = generateDatabaseId(dbPath),
                path = dbPath.toAbsolutePath().toString(),
                name = dbPath.name,
                size = dbPath.fileSize(),
                lastModified = dbPath.getLastModifiedTime().toMillis(),
              )
            )
          }
          else -> error("Path must be a file or directory: $databasePath")
        }
      }
    }.onFailure { e ->
      return CommandResult.err(message = e.message ?: "Failed to discover databases")
    }.getOrThrow()

    // Check if any databases were found
    if (databases.isEmpty()) {
      return CommandResult.err(
        message = if (databasePath == null) {
          "No SQLite databases found in current directory or user data directories. If you're targeting a database file outside of this directory, provide the path as an argument (e.g. \"elide db studio /my/database/path.db\")"
        } else {
          "No SQLite databases found in directory: $databasePath"
        }
      )
    }

    // Output discovered databases
    output {
      appendLine(TextStyles.dim("  Found ${databases.size} database(s):"))
      databases.forEach { db ->
        appendLine(TextStyles.dim("    • ${db.path}"))
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

    // Install and build API server dependencies
    executeStep("Installing dependencies") {
      runTask(stringToTask(
        "elide install > /dev/null 2>&1",
        shell = elide.tooling.runner.ProcessRunner.ProcessShell.Active,
        workingDirectory = apiDir
      )).asDeferred().await()
    }.onFailure { e ->
      return CommandResult.err(
        message = "Failed to setup API server: ${e.message}"
      )
    }

    executeStep("Building API server") {
      runTask(stringToTask(
        "elide build > /dev/null 2>&1",
        shell = elide.tooling.runner.ProcessRunner.ProcessShell.Active,
        workingDirectory = apiDir
      )).asDeferred().await()
    }.onFailure { e ->
      return CommandResult.err(
        message = "Failed to setup API server: ${e.message}"
      )
    }

    output { appendLine() }

    return try {
      coroutineScope {
        val apiTask = async {
          runTask(stringToTask(
            "elide run $STUDIO_INDEX_FILE",
            shell = elide.tooling.runner.ProcessRunner.ProcessShell.None,
            workingDirectory = apiDir
          ))
        }

        val uiTask = async {
          runTask(stringToTask(
            "elide serve --single $STUDIO_OUTPUT_DIR/ui --port $port > /dev/null 2>&1",
            shell = elide.tooling.runner.ProcessRunner.ProcessShell.Active
          ))
        }

        if (!waitForServerReady(apiPort, "API Server")) {
          return@coroutineScope CommandResult.err(message = "API Server didn't start within $MAX_WAIT_SECONDS seconds")
        }

        if (!waitForServerReady(port, "UI Server")) {
          return@coroutineScope CommandResult.err(message = "UI Server didn't start within $MAX_WAIT_SECONDS seconds")
        }


        output {
          appendLine()
          val url = "http://localhost:$port/"
          append((TextStyles.bold)(TextColors.brightMagenta("Elide Database Studio")))
          append(TextStyles.dim(" is running at "))
          append((TextStyles.bold)(url))
          appendLine()
          appendLine()
          appendLine(TextStyles.dim("  → Press ${TextStyles.bold("Ctrl+C")} to stop"))
          appendLine()
        }

        awaitAll(apiTask.await().asDeferred(), uiTask.await().asDeferred())

        CommandResult.success()
      }
    } catch (e: Exception) {
      CommandResult.success()
    }
  }
}
