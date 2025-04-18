package elide.tool.cli.cmd.project

import io.micronaut.core.annotation.Introspected
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import jakarta.inject.Singleton
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import elide.annotations.Inject
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.project.PackageManifestService
import elide.tool.project.ProjectEcosystem
import elide.tool.project.ProjectManager

@Command(
  name = "project",
  description = ["Manage Elide project manifests and other package declarations"],
  mixinStandardHelpOptions = true,
)
@Introspected @Singleton
internal class ToolProjectCommand : AbstractSubcommand<ToolState, CommandContext>() {
  enum class Target(val targetName: String, val ecosystem: ProjectEcosystem, val description: String) {
    NODE("node", ProjectEcosystem.Node, "Node.js package.json"),
    REQUIREMENTS("requirements", ProjectEcosystem.PythonRequirements, "Python requirements.txt"),
  }

  @Inject private lateinit var projectManager: ProjectManager

  @Inject private lateinit var manifests: PackageManifestService

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
        Target.entries.forEach { target -> appendLine("- ${target.targetName} ${target.description}") }
      }

      return success()
    }

    val project = projectManager.resolveProject()
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

    return if (failed) CommandResult.err() else CommandResult.success()
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // only exporting manifests is currently supported
    return export()
  }
}
