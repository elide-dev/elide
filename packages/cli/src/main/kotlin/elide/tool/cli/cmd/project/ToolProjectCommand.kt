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

import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.rendering.TextStyles
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import jakarta.inject.Provider
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo
import elide.annotations.Inject
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tooling.cli.Statics
import elide.tool.cli.ToolState
import elide.tooling.project.ProjectManager
import elide.tooling.project.PackageManifestService
import elide.tooling.project.ProjectEcosystem
import elide.tooling.project.flags.ProjectFlagType
import elide.tooling.project.flags.ProjectFlagValue
import elide.tooling.project.manifest.ElidePackageManifest

@Command(
  name = "project",
  mixinStandardHelpOptions = true,
  description = [
    "Manage Elide projects, which are defined by @|bold elide.pkl|@ or",
    "similar manifests like @|bold package.json|@ or @|bold pyproject.toml|@.",
    "",
    "For more information, run @|fg(magenta) elide help projects|@.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) project|@",
    "   or: elide @|bold,fg(cyan) project|@ [OPTIONS] [TASKS] [--] [ARGS]",
    "   or: elide @|bold,fg(cyan) project|@ @|bold,fg(cyan) --export|@ [@|bold,fg(cyan) --target=...|@]",
    "",
  ],
  subcommands = [
    ProjectAdviceCommand::class,
  ],
)
@Introspected
@ReflectiveAccess
internal class ToolProjectCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  enum class Target(val targetName: String, val ecosystem: ProjectEcosystem, val description: String) {
    NODE("node", ProjectEcosystem.Node, "Node.js package.json"),
    REQUIREMENTS("requirements", ProjectEcosystem.PythonRequirements, "Python requirements.txt"),
    MAVEN("maven", ProjectEcosystem.MavenPom, "Maven pom.xml"),
  }

  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>

  @Inject private lateinit var manifestsProvider: Provider<PackageManifestService>

  private val projectManager: ProjectManager by lazy { projectManagerProvider.get() }
  private val manifests: PackageManifestService by lazy { manifestsProvider.get() }

  @Suppress("unused")
  @Option(
    names = ["--export"],
    description = ["Export third-party manifests for the current Elide project"],
  )
  private var export: Boolean = false

  @Option(
    names = ["-t", "--target"],
    description = ["The target ecosystems to be exported"],
    defaultValue = "auto",
    arity = "0..N",
  )
  private var targets: List<String> = emptyList()

  @Option(
    names = ["-l", "--list-targets"],
    description = ["List supported targets and exit"],
  )
  private var listTargets: Boolean = false

  @Option(
    names = ["-f", "--overwrite"],
    description = ["Overwrite existing manifests when exporting"],
  )
  private var overwrite: Boolean = false

  private suspend fun CommandContext.export(): CommandResult {
    if (listTargets) {
      output {
        appendLine("Supported export targets:")
        Target.entries.forEach { target -> appendLine("- ${target.targetName}: ${target.description}") }
      }

      return success()
    }

    val project = projectManager.resolveProject(projectOptions().projectPath())
    if (project == null) return CommandResult.err(message = "No valid Elide project found, nothing to export")

    val exportTargets = targets.mapNotNull { Target.entries.find { target -> target.targetName == it } }
      .takeUnless { it.isEmpty() } ?: Target.entries

    var failed = false

    for (target in exportTargets) {
      val manifest = runCatching { manifests.export(project.manifest, target.ecosystem) }
        .onFailure { output { appendLine("Failed to export ${target.targetName}: $it") } }
        .getOrNull() ?: continue

      val manifestFile = manifests.resolve(project.root, target.ecosystem)

      if (manifestFile.exists() && !overwrite) {
        output {
          appendLine("Target $manifestFile already exists and will be skipped, use --overwrite to force export")
        }

        continue
      }

      runCatching {
        manifestFile.outputStream().use { output -> manifests.encode(manifest, output) }
      }.onSuccess {
        output { appendLine("Successfully exported $manifestFile") }
      }.onFailure {
        output { appendLine("Failed to write $manifestFile: $it") }
        failed = true
      }
    }

    return if (failed) err() else success()
  }

  // Render project flags to markdown so they can be rendered in the terminal.
  private fun renderFlagsToMd(manifest: ElidePackageManifest): String = buildString {
    appendLine("## Flags")
    appendLine("Pass these when running `elide build` for this project:\n")

    manifest.dev?.flags?.sortedBy { it.name }?.forEach { flag ->
      flag.description?.let { description ->
        val boldedName = TextStyles.bold(flag.name.replace("--", ""))

        val label = when (flag.type) {
          ProjectFlagType.BOOLEAN -> "--[no-]$boldedName"
          ProjectFlagType.STRING -> "--$boldedName=<value>"
          else -> {}
        }
        append("- `$label`")
        when (val flagDefault = flag.defaultValue) {
          ProjectFlagValue.NoValue -> append(": ")
          ProjectFlagValue.True -> append(" (default: `true`): ")
          ProjectFlagValue.False -> append(" (default: `false`): ")
          is ProjectFlagValue.StringValue -> append(" (default: `${flagDefault.value}`): ")
        }
        append(description)
        append("\n")
      }
    }
  }

  // Render dependencies to markdown so they can be rendered in the terminal.
  private fun renderDependenciesToMd(manifest: ElidePackageManifest): String = buildString {
    appendLine("## Dependencies")
    if (manifest.dependencies.maven.hasPackages()) {
      appendLine("- Maven:")
      if (manifest.dependencies.maven.packages.isNotEmpty()) {
        appendLine("  - Compile & Runtime:")
        manifest.dependencies.maven.packages.forEach { dep ->
          appendLine("    - ${dep.coordinate}")
        }
      }
      if (manifest.dependencies.maven.testPackages.isNotEmpty()) {
        appendLine("  - Test-only:")
        manifest.dependencies.maven.testPackages.forEach { dep ->
          appendLine("    - ${dep.coordinate}")
        }
      }
    }
    if (manifest.dependencies.npm.hasPackages()) {
      appendLine("- NPM:")
      if (manifest.dependencies.npm.packages.isNotEmpty()) {
        appendLine("  - Packages:")
        manifest.dependencies.npm.packages.forEach { dep ->
          appendLine("    - ${dep.name}@${dep.version}")
        }
      }
      if (manifest.dependencies.npm.devPackages.isNotEmpty()) {
        appendLine("  - Dev-only:")
        manifest.dependencies.npm.devPackages.forEach { dep ->
          appendLine("    - ${dep.name}@${dep.version}")
        }
      }
    }
    if (manifest.dependencies.pip.hasPackages()) {
      appendLine("- PyPI:")
      manifest.dependencies.pip.packages.forEach { dep ->
        appendLine("  - ${dep.name}")
      }
      manifest.dependencies.pip.optionalPackages.entries.flatMap {
        it.value
      }.forEach { dep ->
        appendLine("  - ${dep.name} (optional)")
      }
    }
  }

  // Render declared project artifacts to markdown so they can be rendered in the terminal.
  private fun renderArtifactsToMd(manifest: ElidePackageManifest): String = buildString {
    if (manifest.artifacts.isNotEmpty()) {
      appendLine("## Artifacts")
      appendLine("Outputs produced by the project's build, via `elide build`.")
      appendLine()
      manifest.artifacts.forEach { artifact ->
        val name = artifact.key
        val type = artifact.value::class.java.simpleName
        val value = artifact.value
        val dimmed = TextStyles.dim(type.toString())
        appendLine("- `$name` ($dimmed)")
        if (value is ElidePackageManifest.Jar) {
          // @TODO: rename this to classes so that sources can be embedded clearly in jars
          if (value.sources.isNotEmpty()) {
            value.sources.forEach { sourceSet ->
              appendLine("  - ${TextStyles.dim("classes:")} $sourceSet")
            }
          } else {
            appendLine("  - ${TextStyles.dim("classes:")} main")
          }
        }
        if (value.from.isNotEmpty()) {
          appendLine("  - ${TextStyles.dim("from:")} ${value.from.joinToString(", ")}")
        }
      }
    }
  }

  // Render declared scripts to markdown so they can be rendered in the terminal.
  private fun renderScriptsToMd(manifest: ElidePackageManifest): String = buildString {
    appendLine("## Scripts")
    appendLine("Run scripts with `elide run <script>`.")
    if (manifest.scripts.isNotEmpty()) {
      appendLine()
      manifest.scripts.forEach {
        val dimmed = TextStyles.dim(it.value)
        appendLine("- `${it.key}`: $dimmed")
      }
    } else {
      appendLine()
      appendLine("(None yet.)")
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // only exporting manifests is currently supported
    return when {
      export || listTargets -> export()

      // print project info if no subcommand is provided
      else -> projectManager.resolveProject(projectOptions().projectPath()).let { project ->
        when (project) {
          null -> err("No project").also {
            if (!quiet) output {
              append("No current project, use `--project`/`-p` to specify a project path.")
            }
          }
          else -> success().also {
            if (!quiet) {
              val descOrNone = project.manifest.description?.ifBlank { null }?.let { description ->
                "$description\n"
              } ?: ""

              // basic project info
              val activeWorkspace = project.activeWorkspace()
              val workspaceInfo = activeWorkspace?.let { (path, manifest) ->
                buildString {
                  append(manifest.name)
                  append(" ")
                  append("(")
                  append(path.parent.absolutePathString())
                  append(")")
                }
              }
              val projectPath = activeWorkspace?.let { (path, _) ->
                path.parent.resolve(project.root).relativeTo(path.parent)
              } ?: project.root.absolute()

              Statics.terminal.println(Markdown("""
                  # ${project.manifest.name}
                  $descOrNone
                  - Version: ${project.manifest.version ?: "(None specified.)"}
                  - Root: $projectPath
                  - Workspace: ${workspaceInfo ?: "(Same as root)"}
                """.trimIndent()))

              // next up, scripts
              if (project.manifest.scripts.isNotEmpty()) {
                Statics.terminal.println(Markdown(renderScriptsToMd(project.manifest)))
              }

              // next up, flags
              if (project.manifest.dev?.flags?.isNotEmpty() == true) {
                Statics.terminal.println(Markdown(renderFlagsToMd(project.manifest)))
              }

              // dependencies up next
              Statics.terminal.println(Markdown(renderDependenciesToMd(project.manifest)))

              // artifacts
              project.manifest.artifacts.takeIf { it.isNotEmpty() }?.let { artifacts ->
                Statics.terminal.println(Markdown(renderArtifactsToMd(project.manifest)))
              }
            }
          }
        }
      }
    }
  }
}
