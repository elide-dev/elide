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
import elide.tooling.project.adopt.PklGenerator
import elide.tooling.project.adopt.node.NodeParser
import elide.tooling.project.adopt.node.PackageJsonDescriptor

/**
 * Adopt Node.js package.json to elide.pkl.
 *
 * This command parses a package.json file and generates an equivalent elide.pkl
 * configuration file. It handles:
 * - Basic project metadata (name, version, description)
 * - Dependencies (dependencies, devDependencies)
 * - Peer and optional dependencies (documented as comments)
 * - Workspaces/monorepos
 * - NPM scripts (documented as comments)
 */
@Command(
  name = "node",
  mixinStandardHelpOptions = true,
  description = [
    "Adopt Node.js package.json to elide.pkl.",
  ],
  customSynopsis = [
    "elide adopt @|bold,fg(cyan) node|@ [PACKAGE_JSON]",
    "elide adopt @|bold,fg(cyan) node|@ [OPTIONS] [PACKAGE_JSON]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class NodeAdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = ["Path to package.json file (default: package.json in current directory)"]
  )
  var packageJsonFile: String? = null

  @Option(
    names = ["--output", "-o"],
    description = ["Output file path (default: elide.pkl in same directory as package.json)"]
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

  @Option(
    names = ["--skip-workspaces"],
    description = ["Skip processing workspace packages (only convert the root package.json)"]
  )
  var skipWorkspaces: Boolean = false

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // Determine package.json file path
    val packageJsonPath = when {
      packageJsonFile != null -> Path.of(packageJsonFile).absolute()
      else -> Path.of("package.json").absolute()
    }

    // Validate package.json file exists
    if (!packageJsonPath.exists()) {
      return err("package.json file not found: $packageJsonPath")
    }

    output {
      append("@|bold,cyan 📦 Parsing package.json...|@")
      append("  File: @|bold ${packageJsonPath.fileName}|@")
      append("  Path: ${packageJsonPath.parent}")
    }

    // Parse package.json
    val rootPkg = try {
      NodeParser.parse(packageJsonPath)
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to parse package.json file|@\n  ${e.message}\n\n" +
        "Tip: Ensure the package.json file is valid JSON.")
    }

    output {
      append("@|bold,green ✓ Parsed package.json successfully|@")
      append("  Package: @|bold ${rootPkg.name}|@")
      if (rootPkg.version != null) {
        append("  Version: ${rootPkg.version}")
      }
      if (rootPkg.workspaces.isNotEmpty() && !skipWorkspaces) {
        append("  Workspaces: ${rootPkg.workspaces.size} package(s)")
      }
    }

    // Handle workspaces if present
    val pklContent = if (rootPkg.workspaces.isNotEmpty() && !skipWorkspaces) {
      output {
        append("@|bold,yellow 📁 Processing workspace packages...|@")
      }

      // Parse workspace packages
      val workspacePackages = rootPkg.workspaces.mapNotNull { workspacePath ->
        val workspaceDir = packageJsonPath.parent.resolve(workspacePath)
        val workspacePkgJson = workspaceDir.resolve("package.json")

        if (workspacePkgJson.exists()) {
          try {
            NodeParser.parse(workspacePkgJson)
          } catch (e: Exception) {
            output {
              append("@|yellow ⚠ Warning: Failed to parse workspace package.json at $workspacePath|@")
            }
            null
          }
        } else {
          null
        }
      }

      output {
        append("@|bold,green ✓ Parsed ${workspacePackages.size} workspace package(s)|@")
      }

      // Generate workspace PKL
      PklGenerator.generateWorkspace(rootPkg, workspacePackages)
    } else {
      // Generate single-package PKL
      PklGenerator.generate(rootPkg)
    }

    // Determine output path
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> packageJsonPath.parent.resolve("elide.pkl")
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
      if (rootPkg.scripts.isNotEmpty()) {
        append("  3. Convert NPM scripts (currently documented as comments)")
      }
    }

    return success()
  }
}
