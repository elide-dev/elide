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
package elide.tool.cli.cmd.project

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import elide.core.api.Symbolic
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tooling.project.ProjectManager
import elide.tooling.config.BuildConfigurator
import elide.tooling.project.ElideProject
import elide.tooling.project.agents.AgentAdvice
import elide.tooling.project.load

@Command(
  name = "advice",
  mixinStandardHelpOptions = true,
  description = [
    "Render and sync project advice, which creates Markdown files driven by the",
    "@|bold elide.pkl|@ or similar manifests like @|bold package.json|@ or @|bold pyproject.toml|@.",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) project advice [--[no-]write]|@",
    "",
  ],
)
@Introspected
@ReflectiveAccess
class ProjectAdviceCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>
  private val projectManager: ProjectManager by lazy { projectManagerProvider.get() }

  private companion object {
    private val defaultAliases = EnumSet.of(
      AgentAliasType.Claude,
    )
  }

  /** Type of agent alias to use for advice generation. */
  enum class AgentAliasType (override val symbol: String) : Symbolic<String> {
    Claude("CLAUDE.md");

    fun pathFor(dirs: BuildConfigurator.ProjectDirectories): Path = when (this) {
      Claude -> dirs.devRoot.resolve(symbol)
    }
  }

  @CommandLine.Option(
    names = ["--write"],
    description = [
      "Write advice to the project directory, if applicable. If not specified, advice will be rendered to stdout.",
      "If @|bold --write|@ is specified, advice will be written to the project directory.",
    ],
    defaultValue = "false",
    negatable = true,
  )
  var write: Boolean = false

  @CommandLine.Option(
    names = ["--provider"],
    description = [
      "Write advice directly to provider file locations, like `CLAUDE.md`, instead of `.dev/AGENT.md`.",
      "Setting the AI agent provider in @|elide.pkl|@ also sets this value as a default.",
    ],
  )
  var specificProvider: AgentAliasType? = null

  @CommandLine.Option(
    names = ["--out"],
    description = [
      "Write advice to a specific file; overrides all other output options.",
    ],
  )
  var outFile: Path? = null

  @CommandLine.Option(
    names = ["--alias"],
    description = [
      "Alias types to symlink to `AGENT.md`. Choices include: `claude`.",
    ],
  )
  var aliases: Set<AgentAliasType>? = null

  @CommandLine.Option(
    names = ["--aliases"],
    description = [
      "Write aliases alongside the advice file.",
    ],
    defaultValue = "true",
    negatable = true,
  )
  var writeAliases: Boolean = false

  // Write symlink aliases to the advice file if they don't exist.
  private suspend fun writeAliases(aliases: Set<AgentAliasType>, out: Path, project: ElideProject) = coroutineScope {
    aliases.mapNotNull { alias ->
      val target = project.root.resolve(alias.symbol)
      if (target == out) {
        // skip aliases if they are used as the "direct" advice file
        return@mapNotNull null
      }

      async {
        if (!target.exists()) runCatching {
          Files.createSymbolicLink(
            target.toAbsolutePath().relativeTo(project.root),
            out.toAbsolutePath().relativeTo(out.parent),
          )
        }.onFailure {
          logging.warn { "Failed to create agent link from '${alias.symbol}' to '${out.fileName}' (error: $it)" }
        }
      }
    }.awaitAll()
  }

  // Create or update the project's advice file.
  private suspend fun CommandContext.write(advice: AgentAdvice, path: Path, project: ElideProject): CommandResult {
    return kotlinx.coroutines.withContext(Dispatchers.IO) {
      val rendered = buildString { advice.export(this) }
      val out = path.toAbsolutePath()
      logging.debug { "Writing project advice to: $out" }
      val exists = out.exists()

      when {
        exists && !out.isRegularFile() -> return@withContext err("Advice file should be a file")
        exists && !out.isWritable() -> return@withContext err("Advice file is not writable")
        else -> success().also {
          (if (exists) { out.deleteExisting() } else null).let {
            out.outputStream().bufferedWriter(StandardCharsets.UTF_8).use { buf ->
              buf.write(rendered).also {
                logging.debug { "Project advice was written" }
              }
            }
          }.also {
            when (val aliasesToWrite = (aliases ?: defaultAliases).takeIf { writeAliases }) {
              null -> {}
              else -> writeAliases(aliasesToWrite, out, project)
            }
          }
        }
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val project = projectManager.resolveProject(projectOptions().projectPath())
    if (project == null) {
      logging.warn { "No active project; advice will be generic" }
    }
    if (write && project == null) {
      return err("No active project, so nowhere to write, but `--write` was set to `true`.")
    }
    val dirs = project?.let { BuildConfigurator.ProjectDirectories.forProject(it) }

    return AgentAdvice.withDefaults(project?.load()) {
      // any custom advice here
    }.let { advice ->
      when (write) {
        // if `write` is false, print to stdout and exit
        false -> return success().also {
          println(buildString { advice.export(this) })
        }

        // otherwise, resolve the out path and write to it
        true -> when {
          // out file is specified, write to that file
          outFile != null -> write(advice, requireNotNull(outFile), requireNotNull(project))

          else -> when (val provider = specificProvider) {
            null -> write(advice, requireNotNull(dirs).devRoot.resolve("AGENT.md"), project)
            else -> write(advice, provider.pathFor(requireNotNull(dirs)), project)
          }
        }
      }
    }
  }
}
