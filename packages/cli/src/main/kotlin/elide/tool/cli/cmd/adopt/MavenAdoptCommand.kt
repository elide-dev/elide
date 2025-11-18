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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.exists
import kotlin.io.path.writeText
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/**
 * Adopt Maven pom.xml to elide.pkl.
 *
 * This command parses a Maven POM file and generates an equivalent elide.pkl
 * configuration file. It handles:
 * - Basic project metadata (groupId, artifactId, version, description)
 * - Dependencies (compile and test scopes)
 * - Dependency management version resolution
 * - Source directory mappings
 * - Multi-module projects
 * - Parent POM resolution
 * - Maven properties interpolation
 */
@Command(
  name = "maven",
  mixinStandardHelpOptions = true,
  description = [
    "Adopt Maven pom.xml to elide.pkl.",
  ],
  customSynopsis = [
    "elide adopt @|bold,fg(cyan) maven|@ [POM_FILE]",
    "elide adopt @|bold,fg(cyan) maven|@ [OPTIONS] [POM_FILE]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class MavenAdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = ["Path to pom.xml file (default: pom.xml in current directory)"]
  )
  var pomFile: String? = null

  @Option(
    names = ["--output", "-o"],
    description = ["Output file path (default: elide.pkl in same directory as pom.xml)"]
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
    names = ["--skip-modules"],
    description = ["Skip processing multi-module children (only convert the parent POM)"]
  )
  var skipModules: Boolean = false

  @Option(
    names = ["--activate-profile", "-P"],
    description = ["Activate Maven profile(s) by ID (can be specified multiple times)"]
  )
  var activateProfiles: List<String> = emptyList()

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // Determine POM file path
    val pomPath = when {
      pomFile != null -> Path.of(pomFile).absolute()
      else -> Path.of("pom.xml").absolute()
    }

    // Validate POM file exists
    if (!pomPath.exists()) {
      return err("POM file not found: $pomPath")
    }

    output {
      append("Parsing ${pomPath.fileName}...")
    }

    // Parse POM
    val basePom = try {
      PomParser.parse(pomPath)
    } catch (e: Exception) {
      return err("Failed to parse POM file: ${e.message}")
    }

    // Activate profiles if specified
    val pom = if (activateProfiles.isNotEmpty()) {
      output {
        append("Activating profile(s): ${activateProfiles.joinToString(", ")}")
      }
      PomParser.activateProfiles(basePom, activateProfiles)
    } else {
      basePom
    }

    // Check if this is a multi-module project
    val isMultiModule = pom.modules.isNotEmpty()

    output {
      append("Found project: ${pom.name ?: pom.artifactId}")
      append("  GroupId: ${pom.groupId}")
      append("  ArtifactId: ${pom.artifactId}")
      append("  Version: ${pom.version}")
      if (pom.description != null) {
        append("  Description: ${pom.description}")
      }
      if (isMultiModule) {
        append("  Type: Multi-module project")
        append("  Modules: ${pom.modules.size}")
      }
      if (pom.profiles.isNotEmpty()) {
        append("  Profiles: ${pom.profiles.size} (${pom.profiles.joinToString(", ") { it.id }})")
      }
      append("  Dependencies: ${pom.dependencies.size} (${pom.dependencies.count { it.scope == "compile" || it.scope == "runtime" }} compile, ${pom.dependencies.count { it.scope == "test" }} test)")
    }

    // Handle multi-module projects
    if (isMultiModule && !skipModules) {
      return convertMultiModuleProject(pom, pomPath)
    }

    // Handle single-module project
    return convertSinglePom(pom, pomPath) ?: success()
  }

  /**
   * Convert a multi-module Maven project to a single root elide.pkl with workspaces.
   */
  private suspend fun CommandContext.convertMultiModuleProject(parentPom: PomDescriptor, pomPath: Path): CommandResult {
    output {
      appendLine()
      append("Processing ${parentPom.modules.size} module(s)...")
    }

    val modulePoms = mutableListOf<PomDescriptor>()
    var failureCount = 0

    // Parse all module POMs
    for (moduleName in parentPom.modules) {
      val modulePomPath = pomPath.parent.resolve(moduleName).resolve("pom.xml")

      if (!modulePomPath.exists()) {
        output {
          append("  ⚠ Warning: Module '$moduleName' pom.xml not found at $modulePomPath")
        }
        failureCount++
        continue
      }

      try {
        val modulePom = PomParser.parse(modulePomPath)
        modulePoms.add(modulePom)
        output {
          append("  ✓ Parsed module: ${modulePom.artifactId}")
        }
      } catch (e: Exception) {
        failureCount++
        output {
          append("  ✗ Failed to parse module '$moduleName': ${e.message}")
        }
      }
    }

    if (modulePoms.isEmpty()) {
      return err("No modules could be parsed successfully")
    }

    // Generate single root elide.pkl with workspaces
    val pklContent = PklGenerator.generateMultiModule(parentPom, modulePoms)

    // Determine output path (always at root for multi-module)
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> pomPath.parent.resolve("elide.pkl")
    }

    // Handle dry run
    if (dryRun) {
      output {
        appendLine()
        append("Generated multi-module elide.pkl:")
        append("=".repeat(60))
        append(pklContent)
        append("=".repeat(60))
      }
      return success()
    }

    // Check if output file exists
    if (outputPath.exists() && !force) {
      return err("Output file already exists: $outputPath\nUse --force to overwrite")
    }

    // Write output file
    try {
      outputPath.writeText(pklContent)
      output {
        appendLine()
        append("✓ Successfully created multi-module elide.pkl at $outputPath")
        append("  Included ${modulePoms.size} module(s), skipped $failureCount")
        append("  Total dependencies: ${(parentPom.dependencies + modulePoms.flatMap { it.dependencies }).distinctBy { it.coordinate() }.size}")
      }
    } catch (e: Exception) {
      return err("Failed to write output file: ${e.message}")
    }

    return success()
  }

  /**
   * Convert a single POM to elide.pkl.
   * Returns an error result if conversion fails, null if successful.
   */
  private suspend fun CommandContext.convertSinglePom(pom: PomDescriptor, pomPath: Path): CommandResult? {
    // Generate elide.pkl content
    val pklContent = PklGenerator.generate(pom)

    // Determine output path
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> pomPath.parent.resolve("elide.pkl")
    }

    // Handle dry run
    if (dryRun) {
      output {
        appendLine()
        append("Generated elide.pkl for ${pom.artifactId}:")
        append("=".repeat(60))
        append(pklContent)
        append("=".repeat(60))
      }
      return null
    }

    // Check if output file exists
    if (outputPath.exists() && !force) {
      return err("Output file already exists: $outputPath\nUse --force to overwrite")
    }

    // Write output file
    try {
      outputPath.writeText(pklContent)
      output {
        appendLine()
        append("✓ Successfully created $outputPath")
      }
    } catch (e: Exception) {
      return err("Failed to write output file: ${e.message}")
    }

    return null
  }
}
