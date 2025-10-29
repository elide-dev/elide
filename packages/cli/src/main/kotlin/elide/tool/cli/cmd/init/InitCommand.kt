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

package elide.tool.cli.cmd.init

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import com.github.kinquirer.components.promptListObject
import com.github.kinquirer.core.Choice
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tool.exec.SubprocessRunner.delegateTask
import elide.tool.exec.SubprocessRunner.stringToTask
import elide.tooling.project.mcp.McpProjectConfig

/**
 * Initialize a new project.
 */
@Command(
  name = "init",
  mixinStandardHelpOptions = true,
  description = [
    "Initialize a new Elide project.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) init|@",
    "   or: elide @|bold,fg(cyan) init|@ [TEMPLATE]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal open class InitCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  private companion object {
    @JvmStatic private fun MutableMap<Path, ProjectFile>.defaultFiles() {
      addFile(".gitignore") {
        // language=gitignore
        """
        .dev/
        !.dev/elide.lock*
        node_modules
        node_modules/
        """
      }
      addFile("README.md") {
        """
          # New project
          This is a new Elide project. To get started, run:

          ```bash
          elide build
          ```

          This will compile the project and install dependencies. Then, run:

          ```bash
          elide test
          ```

          To run the project's default tests.
        """.trimIndent()
      }
    }

    @JvmStatic private fun MutableMap<Path, ProjectFile>.addFile(path: Path, contents: String) {
      put(path, ProjectFile(path) { contents })
    }

    @JvmStatic private fun MutableMap<Path, ProjectFile>.addFile(path: String, contents: () -> String) {
      addFile(Path.of(path), contents.invoke().trimIndent())
    }

    private val staticTemplates = arrayOf(
      EmptyProject,
    )
  }

  /** Models a file within a new project or a project template. */
  @JvmRecord private data class ProjectFile(
    val path: Path,
    val contents: () -> String,
  )

  private sealed interface RenderableTemplateInfo {
    val name: String
    val description: String
    val languages: Set<String>
    val features: SampleFeatures
    val noManifest: Boolean
    val hasTests: Boolean
    val hasBuild: Boolean
    val hasDependencies: Boolean
  }

  private sealed interface RenderableTemplate: RenderableTemplateInfo {
    val tree: Map<Path, ProjectFile>
  }

  /** Features supported by a given sample project. */
  @Serializable @JvmRecord private data class SampleFeatures(
    val install: Boolean = true,
    val build: Boolean = true,
    val test: Boolean = true,
  ) {
    companion object {
      /** Default features for a sample project. */
      val DEFAULTS = SampleFeatures()
    }
  }

  /** Models project template info as specified in JSON from resources. */
  @Serializable @JvmRecord private data class ProjectTemplateInfo(
    override val name: String,
    override val description: String,
    override val languages: Set<String> = emptySet(),
    override val noManifest: Boolean = false,
    override val hasDependencies: Boolean = true,
    override val hasTests: Boolean = true,
    override val hasBuild: Boolean = true,
    override val features: SampleFeatures = SampleFeatures.DEFAULTS,
    val files: List<String> = emptyList(),
  ): RenderableTemplateInfo

  /** Models a project template loaded into actual file contents. */
  private data class ProjectTemplate(
    val info: ProjectTemplateInfo,
    val files: Map<Path, ProjectFile>,
  ): RenderableTemplate, RenderableTemplateInfo by info {
    override val tree: Map<Path, ProjectFile> get() = files.map {
      it.key to ProjectFile(
        path = it.key,
        contents = {
          (InitCommand::class.java.getResourceAsStream("/META-INF/elide/samples/${info.name}.zip")
            ?: error("Failed to locate embedded resource: ${it.key}")
          ).let { stream ->
            ZipInputStream(stream).use { zipIn ->
              // find the specific entry and return as a string
              var entry = zipIn.nextEntry
              while (entry != null) {
                if (entry.name == it.key.toString()) {
                  return@use zipIn.readAllBytes().decodeToString()
                }
                entry = zipIn.nextEntry
              }
              error("Failed to locate embedded resource: ${it.key}")
            }
          }
        }
      )
    }.toMap()
  }

  /** Outer config structure for built-in project templates. */
  @Serializable @JvmRecord private data class ProjectTemplates(
    val version: Int = 1,
    val projects: List<ProjectTemplateInfo> = emptyList(),
  )

  /** Template: default, empty project. Creates an `elide.pkl`, `.gitignore`, and `README.md`. */
  private data object EmptyProject: RenderableTemplate {
    override val name: String get() = "empty"
    override val languages: Set<String> get() = setOf()
    override val description: String get() = "Default empty project"
    override val noManifest: Boolean get() = false
    override val hasDependencies: Boolean get() = true
    override val hasTests: Boolean get() = true
    override val hasBuild: Boolean get() = true
    override val features: SampleFeatures get() = SampleFeatures.DEFAULTS
    override val tree: Map<Path, ProjectFile> get() = buildMap {
      defaultFiles()
    }
  }

  @OptIn(ExperimentalSerializationApi::class)
  private fun loadInstalledTemplates(): List<RenderableTemplate> {
    val suite = (InitCommand::class.java.getResourceAsStream("/META-INF/elide/samples/samples.json") ?: error(
      "Failed to locate `samples.json` embedded resource; this is a bug in Elide"
    )).use {
      Json.decodeFromStream<ProjectTemplates>(it)
    }
    return staticTemplates
      .toList()
      .plus(suite.projects.map {
        ProjectTemplate(
          info = it,
          files = it.files.associate { projectFile ->
            val path = Path.of(projectFile)
            path to ProjectFile(
              path = path,
              contents = {
                (InitCommand::class.java.getResourceAsStream("/META-INF/elide/samples/${it.name}/$projectFile")
                  ?: error("Failed to locate embedded resource: $projectFile")
                ).use { stream ->
                  stream.readAllBytes().decodeToString()
                }
              }
            )
          }
        )
      })
  }

  @CommandLine.Option(
    names = ["--name", "-n"],
    paramLabel = "<str>",
    description = ["Name to use; if not passed, will prompt or use current directory."],
  )
  var projectName: String? = null

  @CommandLine.Option(
    names = ["--yes"],
    description = ["Assume 'yes' to all questions; implied by 'force'."],
  )
  var assumeYes: Boolean = false

  @CommandLine.Option(
    names = ["--interactive", "-i"],
    negatable = true,
    defaultValue = "true",
    description = ["Whether to prompt for questions; defaults to terminal state."],
  )
  var interactive: Boolean = false

  @CommandLine.Option(
    names = ["--force"],
    defaultValue = "false",
    negatable = true,
    description = ["Whether to overwrite existing files."],
  )
  var force: Boolean = false

  @CommandLine.Option(
    names = ["--install"],
    defaultValue = "true",
    negatable = true,
    description = ["Whether to run `elide install` in the new project."],
  )
  var install: Boolean = true

  @CommandLine.Option(
    names = ["--build"],
    defaultValue = "true",
    negatable = true,
    description = ["Whether to run `elide build` in the new project."],
  )
  var build: Boolean = true

  @CommandLine.Option(
    names = ["--test"],
    defaultValue = "true",
    negatable = true,
    description = ["Whether to run `elide test` in the new project."],
  )
  var test: Boolean = true

  @CommandLine.Parameters(
    index = "0",
    description = ["Project template to use; pass --list-templates to see available templates."],
    arity = "0..1",
  )
  var template: String? = null

  @CommandLine.Parameters(
    index = "1",
    description = ["Path where this project should be created. Defaults to cwd."],
    arity = "0..1",
  )
  var path: String? = null

  @CommandLine.Option(
    names = ["--mcp"],
    defaultValue = "true",
    description = ["Initialize a `${McpProjectConfig.MCP_CONFIG_FILE}` file."],
  )
  var mcp: Boolean? = null

  private fun renderManifest(template: RenderableTemplate, name: String): StringBuilder = StringBuilder().apply {
    when (val existing = template.tree[Path.of("elide.pkl")]) {
      null -> {
        appendLine("amends \"elide:project.pkl\"")
        appendLine()
        appendLine("name = \"$name\"")
        appendLine()
        appendLine("scripts {}")
        appendLine("dependencies {}")
      }
      else -> append(existing.contents())
    }
  }

  @Suppress("ReturnCount", "CyclomaticComplexMethod")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val targetPath = (path?.let { Path.of(it) } ?: projectOptions().projectPath())
    val allTemplates = loadInstalledTemplates()
    val selectedTemplate = when (template) {
        null -> KInquirer.promptListObject(
          "Which template would you like to use?",
          choices = allTemplates.map {
            Choice(it.name, it)
          }
        ).let { choice ->
          allTemplates.find {
            it.name == choice.name
          } ?: error(
            "No such template: '$choice'. Available templates: ${allTemplates.joinToString { it.name }}"
          )
        }
        "empty" -> EmptyProject
        else -> allTemplates.find {
          it.name == template
        } ?: error(
          "No such template: '$template'. Available templates: ${allTemplates.joinToString { it.name }}"
        )
    }
    output {
      append("Using template: '${selectedTemplate.name}'")
    }
    val terminal = Statics.terminal
    val selectedName = when {
      // allow the user to select a name, and prompt for a name if we can
      projectName == null && (interactive && terminal.terminalInfo.interactive) -> KInquirer.promptInput(
        "What should the project be called?",
        default = projectName ?: targetPath.last().name,
      )

      // if we don't have an interactive terminal, use the folder name by default
      projectName == null -> targetPath.last().name

      projectName != null && projectName!!.isNotEmpty() -> projectName!!
      else -> targetPath.last().name
    }
    output {
      append("Using project name: '$selectedName'")
    }
    if (!force) {
      when (Files.exists(targetPath) && Files.list(targetPath).findFirst().isPresent) {
        true -> if (interactive && terminal.terminalInfo.interactive) {
          KInquirer.promptConfirm(
            "Project path already exists. Do you want to continue?",
            default = false,
          ).let { if (!it) return success() }
        } else {
          return err("Project path is not empty")
        }

        false -> Files.createDirectories(targetPath)
      }
    }
    val doCreateMcp: Boolean = if (mcp != null) (mcp == true) else {
      if (interactive && terminal.terminalInfo.interactive) {
        KInquirer.promptConfirm(
          "Initialize a `${McpProjectConfig.MCP_CONFIG_FILE}` file?",
          default = true,
        )
      } else true  // assume yes if not interactive
    }.also {
      mcp = it  // update for other uses
    }

    // start inflating files from the project
    val elideManifest = renderManifest(
      selectedTemplate,
      name = selectedName,
    )
    val projectFiles = mutableMapOf<Path, ProjectFile>().apply {
      // add all files from the template
      putAll(selectedTemplate.tree)

      // render a manifest and add it
      addFile("elide.pkl") { elideManifest.toString() }

      // if we should create an MCP config, add it
      if (doCreateMcp) addFile(McpProjectConfig.MCP_CONFIG_FILE) {
        McpProjectConfig.createForElideJson()
      }
    }
    projectFiles.values.forEach { file ->
      val targetFile = targetPath.resolve(file.path)
      if (Files.exists(targetFile)) {
        if (force) {
          Files.delete(targetFile)
        } else {
          return err("File already exists: ${targetFile.toAbsolutePath()}")
        }
      }
      output {
        append("Adding '${file.path}'")
      }
      Files.createDirectories(targetFile.parent)
      Files.writeString(targetFile, file.contents())
    }
    if (!install && !build && !test) {
      output {
        append("✅ New project created.")
      }
      return success()
    }
    val absoluteProjectPath = targetPath.absolutePathString()

    // run an `elide install` in the new project
    if (force) {
      // `force` implies `assumeYes`
      assumeYes = true
    }
    if (install && (selectedTemplate.hasDependencies && selectedTemplate.features.install)) {
      val doInstall = if (interactive && terminal.terminalInfo.interactive && !assumeYes) {
        KInquirer.promptConfirm("Install dependencies?", default = true)
      } else true

      if (doInstall) {
        output { appendLine() }
        delegateTask(stringToTask("elide install -p $absoluteProjectPath")).let {
          when (it) {
            is CommandResult.Success -> {}
            is CommandResult.Error -> {
              return err("Failed to install dependencies: $it")
            }
          }
        }
      }
    }
    if (build && (selectedTemplate.hasBuild && selectedTemplate.features.build)) {
      val doBuild = if (interactive && terminal.terminalInfo.interactive && !assumeYes) {
        KInquirer.promptConfirm("Build the new project?", default = true)
      } else true

      if (doBuild) {
        output { appendLine() }
        // run an `elide build` in the new project
        delegateTask(stringToTask("elide build -p $absoluteProjectPath")).let {
          when (it) {
            is CommandResult.Success -> {}
            is CommandResult.Error -> {
              return err("Failed to build new project: $it")
            }
          }
        }
      }
    }
    if (test && (selectedTemplate.hasTests && selectedTemplate.features.test)) {
      val doTest = if (interactive && terminal.terminalInfo.interactive && !assumeYes) {
        KInquirer.promptConfirm("Run new project's tests?", default = true)
      } else true

      if (doTest) {
        output { appendLine() }
        // run an `elide test` in the new project
        delegateTask(stringToTask("elide test -p $absoluteProjectPath --install")).let {
          when (it) {
            is CommandResult.Success -> {}
            is CommandResult.Error -> {
              return err("Failed to test new project: $it")
            }
          }
        }
      }
    }
    output {
      append("✅ New project ready.")
    }
    return success()
  }
}
