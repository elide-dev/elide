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
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.writeText
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/**
 * Adopt Bazel BUILD and WORKSPACE files to elide.pkl.
 *
 * This command parses Bazel build files and generates an equivalent elide.pkl
 * configuration file. It handles:
 * - WORKSPACE/MODULE.bazel files for maven_install dependencies
 * - BUILD/BUILD.bazel files for targets and sources
 * - Java and Kotlin rules (java_library, java_binary, java_test, kt_jvm_*)
 */
@Command(
  name = "bazel",
  mixinStandardHelpOptions = true,
  description = [
    "Adopt Bazel BUILD and WORKSPACE files to elide.pkl.",
  ],
  customSynopsis = [
    "elide adopt @|bold,fg(cyan) bazel|@ [PROJECT_DIR]",
    "elide adopt @|bold,fg(cyan) bazel|@ [OPTIONS] [PROJECT_DIR]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class BazelAdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = ["Path to Bazel project directory (default: current directory)"]
  )
  var projectDir: String? = null

  @Option(
    names = ["--output", "-o"],
    description = ["Output file path (default: elide.pkl in project directory)"]
  )
  var outputFile: String? = null

  @Option(
    names = ["--dry-run"],
    description = ["Print generated elide.pkl to stdout without writing to file"]
  )
  var dryRun: Boolean = false

  @Option(
    names = ["--force", "-f"],
    description = ["Overwrite existing elide.pkl if it exists"]
  )
  var force: Boolean = false

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // Determine project directory
    val bazelProjectDir = when {
      projectDir != null -> Path.of(projectDir).absolute()
      else -> Path.of(".").absolute()
    }

    output {
      append("@|bold,cyan 🏗️ Parsing Bazel project...|@")
      append("  Path: $bazelProjectDir")
    }

    // Parse Bazel project
    val bazelDescriptor = try {
      BazelParser.parse(bazelProjectDir)
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to parse Bazel project|@\n  ${e.message}\n\n" +
        "Tip: Ensure the project directory contains WORKSPACE or MODULE.bazel file.")
    }

    output {
      append("@|bold,green ✓ Parsed Bazel project successfully|@")
      append("  Project: @|bold ${bazelDescriptor.name}|@")
      if (bazelDescriptor.workspaceFile != null) {
        append("  Workspace: ${bazelDescriptor.workspaceFile.fileName}")
      }
      if (bazelDescriptor.buildFile != null) {
        append("  Build file: ${bazelDescriptor.buildFile.fileName}")
      }
      if (bazelDescriptor.dependencies.isNotEmpty()) {
        append("  Dependencies: ${bazelDescriptor.dependencies.size}")
      }
      if (bazelDescriptor.targets.isNotEmpty()) {
        append("  Targets: ${bazelDescriptor.targets.size}")
      }
    }

    // Generate PKL content
    val pklContent = PklGenerator.generate(bazelDescriptor)

    // Determine output path
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> bazelProjectDir.resolve("elide.pkl")
    }

    // Dry run - print to stdout
    if (dryRun) {
      output {
        appendLine()
        append("@|bold,cyan Generated elide.pkl:|@")
        appendLine()
        append(pklContent)
      }
      return success()
    }

    // Check if output file exists and handle force flag
    if (outputPath.exists() && !force) {
      return err("@|bold,red ✗ Output file already exists|@: $outputPath\n\n" +
        "Use @|bold --force|@ to overwrite or @|bold --output|@ to specify a different location.")
    }

    // Write PKL file
    try {
      outputPath.writeText(pklContent)
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to write elide.pkl file|@\n  ${e.message}")
    }

    output {
      appendLine()
      append("@|bold,green ✓ Successfully generated elide.pkl!|@")
      append("  Output: @|bold $outputPath|@")
      appendLine()
      append("@|cyan Next steps:|@")
      append("  1. Review the generated elide.pkl file")
      append("  2. Adjust source mappings if needed")
      if (bazelDescriptor.targets.isNotEmpty()) {
        append("  3. Review Bazel targets listed as comments")
      }
    }

    return success()
  }
}
