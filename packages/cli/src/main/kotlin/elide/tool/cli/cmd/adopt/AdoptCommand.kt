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
    }
    return success()
  }

  /**
   * Auto-detect build system and invoke appropriate adopter.
   */
  private suspend fun autoDetectAndAdopt(
    ctx: CommandContext,
    path: String,
    state: ToolContext<ToolState>
  ): CommandResult {
    val projectDir = Path.of(path).absolute()

    // Check for each build system in priority order and provide usage instructions
    return with(ctx) {
      when {
        // Maven: pom.xml
        projectDir.resolve("pom.xml").exists() -> {
          output {
            append("@|bold,fg(green) ✓ Detected:|@ Maven project")
            appendLine()
            append("  File: pom.xml")
            appendLine()
            appendLine()
            append("@|bold Run the following command to convert:|@")
            append("  elide adopt maven ${projectDir.resolve("pom.xml")}")
          }
          success()
        }

        // Gradle: build.gradle or build.gradle.kts
        projectDir.resolve("build.gradle.kts").exists() || projectDir.resolve("build.gradle").exists() -> {
          val buildFile = if (projectDir.resolve("build.gradle.kts").exists()) "build.gradle.kts" else "build.gradle"
          output {
            append("@|bold,fg(green) ✓ Detected:|@ Gradle project")
            appendLine()
            append("  File: $buildFile")
            appendLine()
            appendLine()
            append("@|bold Run the following command to convert:|@")
            append("  elide adopt gradle ${projectDir.resolve(buildFile)}")
          }
          success()
        }

        // Bazel: WORKSPACE, WORKSPACE.bazel, or MODULE.bazel
        projectDir.resolve("MODULE.bazel").exists() ||
        projectDir.resolve("WORKSPACE.bazel").exists() ||
        projectDir.resolve("WORKSPACE").exists() -> {
          output {
            append("@|bold,fg(green) ✓ Detected:|@ Bazel project")
            appendLine()
            appendLine()
            append("@|bold Run the following command to convert:|@")
            append("  elide adopt bazel $projectDir")
          }
          success()
        }

        // Node.js: package.json
        projectDir.resolve("package.json").exists() -> {
          output {
            append("@|bold,fg(green) ✓ Detected:|@ Node.js project")
            appendLine()
            append("  File: package.json")
            appendLine()
            appendLine()
            append("@|bold Run the following command to convert:|@")
            append("  elide adopt node ${projectDir.resolve("package.json")}")
          }
          success()
        }

        else -> {
          output {
            append("@|bold,fg(red) ✗ Error:|@ No supported build system detected at $projectDir")
            appendLine()
            appendLine()
            append("@|bold Looked for:|@")
            append("  • pom.xml (Maven)")
            append("  • build.gradle / build.gradle.kts (Gradle)")
            append("  • WORKSPACE / MODULE.bazel (Bazel)")
            append("  • package.json (Node.js)")
          }
          err(exitCode = 1)
        }
      }
    }
  }
}
