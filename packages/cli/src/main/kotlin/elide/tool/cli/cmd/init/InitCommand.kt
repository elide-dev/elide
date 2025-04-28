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

import com.github.ajalt.mordant.terminal.Terminal
import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.promptConfirm
import com.github.kinquirer.components.promptInput
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.file.Files
import java.nio.file.Path
import jakarta.inject.Singleton
import kotlinx.serialization.Serializable
import kotlin.io.path.name
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState

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
@Singleton
internal open class InitCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  private companion object {
    @JvmStatic private fun MutableMap<Path, ProjectFile>.defaultFiles() {
      addFile(".gitignore") {
        // language=gitignore
        """
        .dev/
        node_modules/
        """
      }
    }

    @JvmStatic private fun MutableMap<Path, ProjectFile>.addFile(path: Path, contents: String) {
      put(path, ProjectFile(path, contents))
    }

    @JvmStatic private fun MutableMap<Path, ProjectFile>.addFile(path: String, contents: String) {
      addFile(Path.of(path), contents)
    }

    @JvmStatic private fun MutableMap<Path, ProjectFile>.addFile(path: String, contents: () -> String) {
      addFile(Path.of(path), contents.invoke().trimIndent())
    }

    private val staticTemplates = arrayOf(
      EmptyProject,
    )
  }

  /** Models a file within a new project or a project template. */
  @Serializable @JvmRecord private data class ProjectFile(
    val path: Path,
    val contents: String,
  )

  /** Models a rendered project init template. */
  @Serializable @JvmRecord private data class PreparedProject(
    val name: String,
    val root: Path,
    val template: String?,
    val parameters: Map<String, String>,
    val tree: Map<Path, ProjectFile>,
  )

  private sealed interface RenderableTemplateInfo {
    val name: String
    val description: String
    val languages: Set<String>
  }

  private sealed interface RenderableTemplate: RenderableTemplateInfo {
    val tree: Map<Path, ProjectFile>
  }

  /** Models project template info as specified in JSON from resources. */
  @Serializable @JvmRecord private data class ProjectTemplateInfo(
    override val name: String,
    override val description: String,
    override val languages: Set<String>,
  ): RenderableTemplateInfo

  /** Models a project template loaded into actual file contents. */
  @Serializable @JvmRecord private data class ProjectTemplate(
    val info: ProjectTemplateInfo,
    val files: Map<Path, ProjectFile>,
  )

  /** Template: default, empty project. Creates an `elide.pkl`, `.gitignore`, and `README.md`. */
  private data object EmptyProject: RenderableTemplate {
    override val name: String get() = "empty"
    override val languages: Set<String> get() = setOf()
    override val description: String get() = "Default empty project"
    override val tree: Map<Path, ProjectFile> get() = buildMap {
      defaultFiles()
    }
  }

  private fun loadInstalledTemplates(): List<RenderableTemplate> {
    return staticTemplates.toList() // @TODO more from classpath
  }

  private val terminal by lazy { Terminal() }

  @CommandLine.Option(
    names = ["--template", "-t"],
    paramLabel = "<name>",
    defaultValue = "empty",
    description = ["Project template to use; pass --list-templates to see available templates."],
  )
  var template: String? = "empty"

  @CommandLine.Option(
    names = ["--name", "-n"],
    paramLabel = "<str>",
    description = ["Name to use; if not passed, will prompt or use current directory."],
  )
  var projectName: String? = null

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
    description = ["Whether to overwrite existing files."],
  )
  var force: Boolean = false

  private fun renderManifest(template: RenderableTemplate, name: String): StringBuilder = StringBuilder().apply {
    appendLine("amends \"elide:project.pkl\"")
    appendLine()
    appendLine("name = \"$name\"")
    appendLine()
    appendLine("scripts {}")
    appendLine("dependencies {}")
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val targetPath = (projectOptions().projectPath ?: System.getProperty("user.dir"))
        .let { Path.of(it) }
    val selectedTemplate = when {
      template == null || template == "empty" -> EmptyProject
      else -> error("No such template: '$template'")
    }
    output {
      append("Using template: '${selectedTemplate.name}'")
    }
    val selectedName = when {
      projectName == null && (interactive || terminal.terminalInfo.inputInteractive) -> KInquirer.promptInput(
        "What should the project be called?",
        default = projectName ?: targetPath.last().name,
      )

      projectName != null && projectName!!.isNotEmpty() -> projectName!!
      else -> targetPath.last().name
    }
    output {
      append("Using project name: '$selectedName'")
    }
    if (!force) {
      when (Files.exists(targetPath)) {
        true -> if (interactive) {
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
      Files.writeString(targetFile, file.contents)
    }
    output {
      append("✅ New project created.")
    }
    return success()
  }
}
