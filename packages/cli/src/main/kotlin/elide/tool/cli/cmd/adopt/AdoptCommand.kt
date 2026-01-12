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

package elide.tool.cli.cmd.adopt

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import picocli.CommandLine.Parameters
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/**
 * Adopt build files from other tools to Elide format.
 *
 * This command provides subcommands for adopting from various build tools:
 * - maven: Adopt Maven pom.xml to elide.pkl
 * - gradle: Adopt Gradle build files to elide.pkl
 */
@Command(
  name = "adopt",
  mixinStandardHelpOptions = true,
  subcommands = [
    MavenAdoptCommand::class,
    GradleAdoptCommand::class,
    BazelAdoptCommand::class,
    NodeAdoptCommand::class,
    PythonAdoptCommand::class,
  ],
  description = [
    "Adopt build files from other tools to Elide format.",
    "Automatically detects build system if path is provided.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) adopt|@ [PATH]",
    "elide @|bold,fg(cyan) adopt|@ <COMMAND>",
    "",
    "Auto-detection:",
    "  elide adopt .                Auto-detect build system in current directory",
    "  elide adopt /path/to/project Auto-detect build system at path",
    "",
    "Available Commands:",
    "  @|bold maven|@     Adopt Maven pom.xml to elide.pkl",
    "  @|bold gradle|@    Adopt Gradle build files to elide.pkl",
    "  @|bold bazel|@     Adopt Bazel BUILD and WORKSPACE files to elide.pkl",
    "  @|bold node|@      Adopt Node.js package.json to elide.pkl",
    "  @|bold python|@    Adopt Python pyproject.toml or requirements.txt to elide.pkl",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class AdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = ["Path to project directory (auto-detects build system)"]
  )
  var projectPath: String? = null

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // If a path was provided, auto-detect and run appropriate adopter
    if (projectPath != null) {
      return autoDetectAndAdopt(this, projectPath!!, state)
    }

    // Otherwise show help
    output {
      append("Use 'elide adopt <command>' to adopt build files, or provide a path for auto-detection.")
      appendLine()
      appendLine()
      append("@|bold Auto-detection:|@")
      append("  elide adopt .                Auto-detect build system in current directory")
      append("  elide adopt /path/to/project Auto-detect build system at path")
      appendLine()
      append("@|bold Available commands:|@")
      append("  maven     Adopt Maven pom.xml to elide.pkl")
      append("  gradle    Adopt Gradle build files to elide.pkl")
      append("  bazel     Adopt Bazel BUILD and WORKSPACE files to elide.pkl")
      append("  node      Adopt Node.js package.json to elide.pkl")
      append("  python    Adopt Python pyproject.toml or requirements.txt to elide.pkl")
    }
    return success()
  }

  /**
   * Data class to hold detected build system information.
   */
  private data class DetectedBuildSystem(
    val name: String,
    val files: List<String>,
    val command: String,
    val description: String? = null
  )

  /**
   * Auto-detect build system(s) and provide guidance.
   * Supports monorepos with multiple build systems.
   */
  private suspend fun autoDetectAndAdopt(
    ctx: CommandContext,
    path: String,
    state: ToolContext<ToolState>
  ): CommandResult {
    val projectDir = Path.of(path).absolute()
    val detected = mutableListOf<DetectedBuildSystem>()

    // Check for Maven
    if (projectDir.resolve("pom.xml").exists()) {
      detected.add(
        DetectedBuildSystem(
          name = "Maven",
          files = listOf("pom.xml"),
          command = "elide adopt maven ${projectDir.resolve("pom.xml")}"
        )
      )
    }

    // Check for Gradle
    val gradleFiles = listOfNotNull(
      if (projectDir.resolve("build.gradle.kts").exists()) "build.gradle.kts" else null,
      if (projectDir.resolve("build.gradle").exists()) "build.gradle" else null
    )
    if (gradleFiles.isNotEmpty()) {
      val buildFile = gradleFiles.first()  // Prefer .kts
      detected.add(
        DetectedBuildSystem(
          name = "Gradle",
          files = gradleFiles,
          command = "elide adopt gradle ${projectDir.resolve(buildFile)}"
        )
      )
    }

    // Check for Bazel
    val bazelFiles = listOfNotNull(
      if (projectDir.resolve("MODULE.bazel").exists()) "MODULE.bazel" else null,
      if (projectDir.resolve("WORKSPACE.bazel").exists()) "WORKSPACE.bazel" else null,
      if (projectDir.resolve("WORKSPACE").exists()) "WORKSPACE" else null
    )
    if (bazelFiles.isNotEmpty()) {
      detected.add(
        DetectedBuildSystem(
          name = "Bazel",
          files = bazelFiles,
          command = "elide adopt bazel $projectDir"
        )
      )
    }

    // Check for Node.js/NPM
    if (projectDir.resolve("package.json").exists()) {
      detected.add(
        DetectedBuildSystem(
          name = "Node.js",
          files = listOf("package.json"),
          command = "elide adopt node ${projectDir.resolve("package.json")}"
        )
      )
    }

    // Check for Python
    val pythonFiles = listOfNotNull(
      if (projectDir.resolve("pyproject.toml").exists()) "pyproject.toml" else null,
      if (projectDir.resolve("requirements.txt").exists()) "requirements.txt" else null,
    )
    if (pythonFiles.isNotEmpty()) {
      val primaryFile = if (pythonFiles.contains("pyproject.toml")) "pyproject.toml" else "requirements.txt"
      detected.add(
        DetectedBuildSystem(
          name = "Python",
          files = pythonFiles,
          command = "elide adopt python ${projectDir.resolve(primaryFile)}"
        )
      )
    }

    return with(ctx) {
      when {
        detected.isEmpty() -> {
          output {
            append("@|bold,fg(red) ✗ Error:|@ No supported build system detected at $projectDir")
            appendLine()
            appendLine()
            append("@|bold Looked for:|@")
            append("  • pom.xml (Maven)")
            append("  • build.gradle / build.gradle.kts (Gradle)")
            append("  • WORKSPACE / MODULE.bazel (Bazel)")
            append("  • package.json (Node.js)")
            append("  • pyproject.toml / requirements.txt (Python)")
          }
          err(exitCode = 1)
        }

        detected.size == 1 -> {
          // Single build system detected
          val system = detected.first()
          output {
            append("@|bold,fg(green) ✓ Detected:|@ ${system.name} project")
            appendLine()
            append("  File${if (system.files.size > 1) "s" else ""}: ${system.files.joinToString(", ")}")
            appendLine()
            appendLine()
            if (system.description != null) {
              append("@|bold,fg(yellow) Note:|@ ${system.description}")
              appendLine()
              appendLine()
            }
            append("@|bold Run the following command to convert:|@")
            append("  ${system.command}")
          }
          success()
        }

        else -> {
          // Multiple build systems detected (monorepo)
          output {
            append("@|bold,fg(cyan) ✓ Detected:|@ Monorepo with ${detected.size} build systems")
            appendLine()
            appendLine()
            append("@|bold Detected build systems:|@")
            for (system in detected) {
              appendLine()
              append("  @|bold ${system.name}:|@")
              append("    Files: ${system.files.joinToString(", ")}")
              if (system.description != null) {
                append("    Note: ${system.description}")
              }
            }
            appendLine()
            appendLine()
            append("@|bold Run these commands to convert:|@")
            for (system in detected) {
              append("  ${system.command}")
            }
            appendLine()
            appendLine()
            append("@|bold,fg(yellow) Tip:|@ For monorepos, you may want to convert each build system separately.")
            append("Elide supports polyglot projects natively!")
          }
          success()
        }
      }
    }
  }
}
