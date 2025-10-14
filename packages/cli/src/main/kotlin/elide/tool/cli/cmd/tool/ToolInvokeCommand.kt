/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.tool.cli.cmd.tool

import io.micronaut.core.annotation.Introspected
import picocli.CommandLine.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import jakarta.inject.Provider
import kotlinx.coroutines.*
import kotlin.io.path.pathString
import elide.annotations.Inject
import elide.core.api.Symbolic
import elide.runtime.Logger
import elide.tool.cli.*
import elide.tool.cli.cmd.tool.EmbeddedTool.*
import elide.tool.cli.cmd.tool.ToolAction.INSTALL
import elide.tool.io.RuntimeWorkdirManager
import elide.tooling.project.ProjectManager
import elide.tooling.cli.Statics
import elide.tooling.project.ElideProject
import elide.tooling.project.PackageManifestService

// Tool constants.
private const val AUTO_SYMBOL = "auto"
private const val RUFF_SYMBOL = "ruff"
private const val OXC_SYMBOL = "oxc"
private const val BIOME_SYMBOL = "biome"
private const val UV_SYMBOL = "uv"
private const val OROGENE_SYMBOL = "orogene"

// Action constants.
private const val INSTALL_ACTION = "install"
private const val UPDATE_ACTION = "update"

// Defaults.
private const val DEFAULT_ACTION = "check"

// Sub-commands for Ruff.
private val ruffActions = sortedSetOf(
  "check",
  "clean",
  "config",
  "format",
  "help",
  "linter",
  "rule",
  "server",
  "version",
)

// Sub-commands for Orogene.
private val orogeneActions = sortedSetOf(
  "add",
  "apply",
  "login",
  "logout",
  "ping",
  "reapply",
  "remove",
  "view",
  "help",
)

// Sub-commands for Uv.
private val uvActions = sortedSetOf(
  "pip",
  "tool",
  "toolchain",
  "venv",
  "cache",
  "self",
  "version",
  "help",
)

// Enumeration of supported tools and their IDs.
@Suppress("unused") private enum class EmbeddedTool (
  override val symbol: String,
  val langs: EnumSet<GuestLanguage>,
): Symbolic<String> {
  RUFF(RUFF_SYMBOL, EnumSet.of(GuestLanguage.PYTHON)),
  OROGENE(OROGENE_SYMBOL, EnumSet.of(GuestLanguage.JS)),
  UV(UV_SYMBOL, EnumSet.of(GuestLanguage.PYTHON)),
  OXC(OXC_SYMBOL, EnumSet.of(GuestLanguage.JS)),
  BIOME(BIOME_SYMBOL, EnumSet.of(GuestLanguage.JS));

  companion object: Symbolic.SealedResolver<String, EmbeddedTool> {
    fun defaultFor(action: ToolAction): Sequence<EmbeddedTool> = when (action) {
      INSTALL -> sequenceOf(OROGENE, UV)
      else -> sequenceOf(RUFF)
    }

    override fun resolve(symbol: String): EmbeddedTool = when (symbol) {
      RUFF_SYMBOL -> RUFF
      OROGENE_SYMBOL -> OROGENE
      OXC_SYMBOL -> OXC
      UV_SYMBOL -> UV
      else -> throw unresolved(symbol)
    }
  }
}

private const val jsHint = "package.json"
private const val pyHint = "requirements.txt"

// Check if there is configuration relevant to this language in the project manifest (e.g. dependencies)
private fun ElideProject.usesLanguage(language: GuestLanguage): Boolean = when (language) {
  GuestLanguage.JS, GuestLanguage.TYPESCRIPT -> manifest.dependencies.npm.packages.isNotEmpty() ||
          manifest.dependencies.npm.devPackages.isNotEmpty()

  GuestLanguage.PYTHON -> manifest.dependencies.pip.packages.isNotEmpty() ||
          manifest.dependencies.pip.optionalPackages.isNotEmpty()

  GuestLanguage.RUBY -> manifest.dependencies.gems.packages.isNotEmpty() ||
          manifest.dependencies.gems.devPackages.isNotEmpty()

  else -> false
}

// Filter eligible tools by their applicability based on language and project settings.
private suspend fun Sequence<EmbeddedTool>.appliesToCurrent(
  logger: Logger,
  tools: Array<String>,
  langs: EnumSet<GuestLanguage>,
  project: ElideProject?,
  langsExplicit: Boolean,
): Sequence<EmbeddedTool> {
  // probing is only enabled if language support isn't explicitly specified.
  val effectiveLangs = if (!langsExplicit) {
    // we should probe for project-related files within the project root, or the current directory.
    val projectDir = (project?.root?.pathString ?: System.getProperty("user.dir"))
      ?: error("Failed to resolve current or project directory; this is a bug in Elide.")

    coroutineScope {
      sequence {
        yield(GuestLanguage.JS to arrayOf(jsHint))
        yield(GuestLanguage.PYTHON to arrayOf(pyHint))
      }.map { (lang, hint) ->
        async {
          withContext(Dispatchers.IO) {
            lang to hint.any { hintFile ->
              Files.exists(Paths.get(projectDir, hintFile).toAbsolutePath())
            }
          }
        }
      }.toList().awaitAll().filter {
        it.second || project?.usesLanguage(it.first) == true
      }.map { it.first }.toSortedSet()
    }
  } else {
    // otherwise, `langs` contains the explicit languages set by the user, so we defer to that.
    langs
  }
  val supportedTools = tools.asSequence().map {
    EmbeddedTool.resolve(it)
  }.toSortedSet()

  return filter { tool ->
    // filter by tool support; then, filter by active language support
    val supported = tool in supportedTools
    val applicable = tool.langs.intersect(effectiveLangs).isNotEmpty()
    (supported && applicable).also {
      if (logger.isDebugEnabled) logger.debug {
        "Tool '${tool.name}' supported=$supported, applicable=$applicable"
      }
    }
  }
}

// Map of defaults for each action.
private enum class ToolAction (
  override val symbol: String,
  private val aliases: Array<String> = emptyArray(),
): Symbolic<String> {
  // Run installer.
  INSTALL(INSTALL_ACTION, arrayOf("i")),

  // Run updater.
  UPDATE(UPDATE_ACTION);

  companion object: Symbolic.SealedResolver<String, ToolAction> {
    override fun resolve(symbol: String): ToolAction = entries
      .firstOrNull { it.symbol == symbol || symbol in it.aliases }
        ?: throw unresolved(symbol)
  }
}

/** Interactive REPL entrypoint for Elide on the command-line. */
@Command(
  name = "tool",
  aliases = [
    "update",
    "oro",
    "ruff",
    "uv",
    "venv",
    "pip",
  ],
  description = ["%nRun polyglot linters on your code"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  hidden = true,
)
@Introspected
@Suppress("unused", "UnusedPrivateProperty")
internal class ToolInvokeCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject lateinit var activeProjectProvider: Provider<ProjectManager>
  @Inject lateinit var workdirManagerProvider: Provider<RuntimeWorkdirManager>
  @Inject lateinit var manifestsProvider: Provider<PackageManifestService>

  private val activeProject: ProjectManager by lazy { activeProjectProvider.get() }
  private val workdirManager: RuntimeWorkdirManager by lazy { workdirManagerProvider.get() }
  private val manifests: PackageManifestService by lazy { manifestsProvider.get() }

  /**
   * Tools to run with the current linter invocation; optional.
   *
   * There is a special value, `auto`, which automatically selects the appropriate tools to use; otherwise, the suite of
   * tools is loaded and run as described (comma-separated or multiple argument forms are supported).
   */
  @Option(
    names = ["-t", "--tool"],
    description = ["The linter tool(s) to use"],
    defaultValue = "auto",
    arity = "0..N",
  )
  private var tools: List<String> = emptyList()

  /**
   * List supported tools, and their versions, and exit.
   */
  @Option(
    names = ["-l", "--list-tools"],
    description = ["List supported tools and exit; supports the format parameter"],
  )
  private var listTools: Boolean = false

  /**
   * Show a stacktrace in the event of a fatal error
   */
  @Option(
    names = ["-s", "--stacktrace"],
    description = ["Show a stacktrace for fatal errors"],
  )
  private var stacktrace: Boolean = false

  /**
   * Paths to apply to the linter; optional.
   *
   * By default, all source files are scanned, modulo the current ignore configuration and file.
   */
  @Parameters(
    index = "0",
    description = ["Tool options, or the path to the file or directory to lint"],
    arity = "0..N",
  )
  private var argsAndPaths: List<String> = emptyList()

  private fun tempToolDir(): Path {
    return workdirManager.tmpDirectory(create = true).toPath()
  }

  // Suspending call to run a single tool through the native interface.
  private suspend fun CommandContext.runSingle(
    selectedTool: EmbeddedTool,
    toolArgs: List<String>,
  ): CommandResult = when (selectedTool) {
    RUFF -> dev.elide.cli.bridge.CliNativeBridge::runRuff
    OROGENE -> dev.elide.cli.bridge.CliNativeBridge::runOrogene
    UV -> dev.elide.cli.bridge.CliNativeBridge::runUv
    OXC, BIOME -> error("Tool '$selectedTool' is not supported yet")
  }.let { tool ->
    val version = dev.elide.cli.bridge.CliNativeBridge.apiVersion()

    if (verbose) output {
      append("Running tool '$selectedTool' (protocol version: $version)")
    }
    try {
      val exit = tool.invoke(listOf(selectedTool.symbol).plus(toolArgs).toTypedArray())
      if (exit != 0) {
        return err("Tool '$selectedTool' failed with exit code $exit", exitCode = exit)
      }
    } catch (err: Throwable) {
      if (stacktrace) {
        err.printStackTrace()
      }
      return err("Failed to run tool", exitCode = -3)
    }
    success()
  }

  // Suspending call to run a single tool through the native interface.
  private suspend fun CommandContext.runMulti(vararg toolCalls: Pair<EmbeddedTool, List<String>>): CommandResult {
    return when (val errExit = toolCalls.map {
      runSingle(it.first, it.second)
    }.find {
      !it.ok
    }) {
      // if the return code is `null`, all jobs succeeded.
      null -> success()

      // otherwise, some tool failed; return the first failure.
      else -> errExit
    }
  }

  // Resolve the tools which apply to this project or operating context.
  private suspend fun resolveApplicable(
    action: ToolAction,
    tools: Array<String>,
    langs: EnumSet<GuestLanguage> = EnumSet.noneOf(GuestLanguage::class.java),
    project: ElideProject? = null,
    langsExplicit: Boolean = false,
  ): Sequence<EmbeddedTool> = EmbeddedTool.defaultFor(action).appliesToCurrent(
    Statics.logging,
    tools,
    langs,
    project,
    langsExplicit,
  )

  // Resolve the list of named tools.
  private fun resolveTools(): Sequence<EmbeddedTool> = tools.asSequence().map { EmbeddedTool.resolve(it) }

  // Build tool arguments for a given action and tool.
  @Suppress("UNUSED_PARAMETER") private fun buildArgs(action: ToolAction, tool: EmbeddedTool): List<String> {
    val customArgs = argsAndPaths.ifEmpty { emptyList() }
    return if (customArgs.isEmpty()) when (tool) {
      RUFF -> listOf("check", ".")
      OROGENE -> listOf("apply", "--no-first-time", "--no-telemetry")
      UV -> listOf("--native-tls", "pip", "install", "-r", "requirements.txt").also {
        println("Running uv with: ${it.joinToString(" ")}")
      }
      else -> emptyList()
    } else when (tool) {
      RUFF -> {
        // if the first arg parses as a path, pass `check` first
        val firstArg = customArgs.first()
        if (firstArg in ruffActions) customArgs else {
          listOf("check").plus(customArgs)
        }
      }

      else -> customArgs
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // tools typically require native access; force early init
    Elide.requestNatives(server = false, tooling = true)

    val version = dev.elide.cli.bridge.CliNativeBridge.apiVersion()
    val tools = dev.elide.cli.bridge.CliNativeBridge.supportedTools()
    val versions = tools.associateWith { dev.elide.cli.bridge.CliNativeBridge.toolVersion(it) }

    // if we are asked to just list tools, do so and exit.
    if (listTools) {
      output {
        appendLine("Supported tools (API: $version):")
        for ((tool, toolVersion) in versions) {
          val relatesTo = dev.elide.cli.bridge.CliNativeBridge.relatesTo(tool).joinToString(", ")
          appendLine("- $tool (version: $toolVersion, lang: $relatesTo)")
        }
      }
      return success()
    }

    // resolve and parse project configuration from manifests
    val project = activeProject.resolveProject(projectOptions().projectPath())

    // decode instruction alias and resolve it to an action
    val instruction = commandSpec?.commandLine()?.parent?.parseResult?.originalArgs()?.firstOrNull() ?: DEFAULT_ACTION
    val action = ToolAction.resolve(instruction)
    val isAutoToolsMode = tools.isEmpty() || tools.firstOrNull() == AUTO_SYMBOL
    val selectedTools = (if (isAutoToolsMode) resolveTools() else resolveApplicable(
      action,
      tools = tools,
      langs = EnumSet.noneOf(GuestLanguage::class.java),
      project = project,
      langsExplicit = false, // @TODO explicit language selection
    )).toList()

    return when (selectedTools.size) {
      // if there's nothing to do, it's a no-op.
      0 -> output {
        appendLine("No tools/langs apply for this run; nothing to do.")
      }.let {
        success()
      }

      1 -> selectedTools.first().let { runSingle(it, buildArgs(action, it)) }
      else -> runMulti(*selectedTools.map { it to buildArgs(action, it) }.toTypedArray())
    }

    // handle auto-selection of tools, based on action
//    val selectedTool = when (val toolArg = this@ToolInvokeCommand.tools.firstOrNull() ?: AUTO_SYMBOL) {
//      AUTO_SYMBOL -> when (action) {
//        FORMAT -> RUFF_SYMBOL
//        CHECK -> RUFF_SYMBOL
//        INSTALL -> OROGENE_SYMBOL
//        UPDATE -> OROGENE_SYMBOL
//      }
//      else -> toolArg
//    }
//    val customArgs = buildToolArgs()
//    val toolArgs: List<String> = if (customArgs.isEmpty()) when (selectedTool) {
//      RUFF_SYMBOL -> listOf("check", ".")
//      OROGENE_SYMBOL -> listOf("apply", "--no-first-time", "--no-telemetry")
//      else -> emptyList()
//    } else when (selectedTool) {
//      RUFF_SYMBOL -> {
//        // if the first arg parses as a path, pass `check` first
//        val firstArg = customArgs.first()
//        if (firstArg in ruffActions) customArgs else {
//          listOf("check").plus(customArgs)
//        }
//      }
//
//      else -> customArgs
//    }
//
//    return when (selectedTool) {
//      RUFF_SYMBOL -> dev.elide.cli.bridge.CliNativeBridge::runRuff
//      OROGENE_SYMBOL -> dev.elide.cli.bridge.CliNativeBridge::runOrogene
//      else -> return err("No tool available at: '$selectedTool'")
//    }.let {
//      if (verbose) output {
//        append("Running tool '$selectedTool' (protocol version: $version)")
//      }
//      try {
//        val exit = it.invoke(listOf(selectedTool).plus(toolArgs).toTypedArray())
//        if (exit != 0) {
//          return err("Tool '$selectedTool' failed with exit code $exit", exitCode = exit)
//        }
//      } catch (err: Throwable) {
//        if (stacktrace) {
//          err.printStackTrace()
//        }
//        return err("Failed to run tool", exitCode = -3)
//      }
//      success()
//    }
  }
}
