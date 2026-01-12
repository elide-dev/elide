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
import elide.tooling.project.adopt.maven.MavenParser
import elide.tooling.project.adopt.maven.PomDescriptor

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
      append("@|bold,cyan 📋 Parsing Maven POM...|@")
      append("  File: @|bold ${pomPath.fileName}|@")
      append("  Path: ${pomPath.parent}")
    }

    // Parse POM
    val basePom = try {
      MavenParser.parse(pomPath)
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to parse POM file|@\n  ${e.message}\n\n" +
        "Tip: Ensure the POM file is valid XML and follows Maven POM schema.")
    }

    // Activate profiles if specified
    val pom = if (activateProfiles.isNotEmpty()) {
      output {
        append("@|bold,yellow ⚙ Activating Maven profiles...|@")
        append("  Profiles: ${activateProfiles.joinToString(", ")}")
      }
      MavenParser.activateProfiles(basePom, activateProfiles)
    } else {
      basePom
    }

    // Check if this is a multi-module project
    val isMultiModule = pom.modules.isNotEmpty()

    output {
      appendLine()
      append("@|bold,green ✓ Project parsed successfully|@")
      append("  Name: @|bold ${pom.name ?: pom.artifactId}|@")
      append("  Coordinates: @|cyan ${pom.groupId}:${pom.artifactId}:${pom.version}|@")
      if (pom.description != null) {
        val desc = if (pom.description!!.length > 80) {
          pom.description!!.take(77) + "..."
        } else {
          pom.description
        }
        append("  Description: $desc")
      }
      if (pom.parent != null) {
        append("  Parent: ${pom.parent!!.groupId}:${pom.parent!!.artifactId}:${pom.parent!!.version}")
      }
      appendLine()

      if (isMultiModule) {
        append("@|bold,magenta 📦 Multi-module project|@")
        append("  Modules: @|bold ${pom.modules.size}|@")
        if (pom.modules.size <= 10) {
          pom.modules.forEach { module ->
            append("    - $module")
          }
        } else {
          pom.modules.take(10).forEach { module ->
            append("    - $module")
          }
          append("    ... and ${pom.modules.size - 10} more")
        }
        appendLine()
      }

      val compileDeps = pom.dependencies.count { it.scope == "compile" || it.scope == "runtime" || it.scope == null }
      val testDeps = pom.dependencies.count { it.scope == "test" }
      append("@|bold 📚 Dependencies|@")
      append("  Total: @|bold ${pom.dependencies.size}|@ (@|cyan ${compileDeps} compile|@, @|yellow ${testDeps} test|@)")
      if (pom.dependencyManagement.isNotEmpty()) {
        append("  Managed: @|bold ${pom.dependencyManagement.size}|@ (from dependencyManagement)")
      }
      if (pom.repositories.isNotEmpty()) {
        append("  Repositories: ${pom.repositories.size}")
      }

      if (pom.profiles.isNotEmpty()) {
        appendLine()
        append("@|bold 🎯 Available profiles|@")
        append("  Count: @|bold ${pom.profiles.size}|@")
        append("  IDs: ${pom.profiles.joinToString(", ") { it.id }}")
      }

      // Warn about unconverted build plugins
      if (pom.plugins.isNotEmpty()) {
        appendLine()
        append("@|bold,yellow ⚠ Build plugins detected|@")
        append("  Count: @|bold ${pom.plugins.size}|@ (manual conversion may be needed)")
        if (pom.plugins.size <= 5) {
          pom.plugins.forEach { plugin ->
            val pluginId = "${plugin.groupId}:${plugin.artifactId}${plugin.version?.let { ":$it" } ?: ""}"
            append("    - $pluginId")
          }
        } else {
          pom.plugins.take(5).forEach { plugin ->
            val pluginId = "${plugin.groupId}:${plugin.artifactId}${plugin.version?.let { ":$it" } ?: ""}"
            append("    - $pluginId")
          }
          append("    ... and ${pom.plugins.size - 5} more")
        }
      }
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
      append("@|bold,cyan 🔍 Processing modules...|@")
      append("  Total modules: @|bold ${parentPom.modules.size}|@")
      appendLine()
    }

    val modulePoms = mutableListOf<PomDescriptor>()
    var failureCount = 0

    // Parse all module POMs
    for ((index, moduleName) in parentPom.modules.withIndex()) {
      val modulePomPath = pomPath.parent.resolve(moduleName).resolve("pom.xml")

      if (!modulePomPath.exists()) {
        output {
          append("  @|yellow ⚠|@ Module @|bold $moduleName|@ pom.xml not found")
        }
        failureCount++
        continue
      }

      try {
        val modulePom = MavenParser.parse(modulePomPath)
        modulePoms.add(modulePom)
        output {
          append("  @|green ✓|@ [@|bold ${index + 1}/${parentPom.modules.size}|@] ${modulePom.artifactId}")
        }
      } catch (e: Exception) {
        failureCount++
        output {
          append("  @|red ✗|@ [@|bold ${index + 1}/${parentPom.modules.size}|@] Failed to parse @|bold $moduleName|@")
          append("      Error: ${e.message}")
        }
      }
    }

    if (modulePoms.isEmpty()) {
      return err("@|bold,red ✗ No modules could be parsed successfully|@\n\n" +
        "Tip: Verify that the module directories exist and contain valid pom.xml files.")
    }

    output {
      appendLine()
      append("@|bold,green ✓ Parsed ${modulePoms.size} module(s) successfully|@")
      if (failureCount > 0) {
        append("  @|yellow Skipped $failureCount module(s) due to errors|@")
      }
      appendLine()
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
        append("@|bold,cyan 📄 Generated multi-module elide.pkl (dry-run)|@")
        append("  Output path: @|bold $outputPath|@")
        append("  Modules: ${modulePoms.size}")
        append("  Total lines: ${pklContent.lines().size}")
        appendLine()
        append("@|bold ${"─".repeat(80)}|@")
        append(pklContent)
        append("@|bold ${"─".repeat(80)}|@")
      }
      return success()
    }

    // Check if output file exists
    if (outputPath.exists() && !force) {
      return err("@|bold,red ✗ Output file already exists|@\n  Path: $outputPath\n\n" +
        "Tip: Use @|bold --force|@ to overwrite the existing file.")
    }

    // Write output file
    try {
      outputPath.writeText(pklContent)

      val totalDeps = (parentPom.dependencies + modulePoms.flatMap { it.dependencies })
        .distinctBy { it.coordinate() }
        .size

      output {
        appendLine()
        append("@|bold,green ✓ Successfully created multi-module elide.pkl|@")
        append("  Location: @|bold $outputPath|@")
        append("  Modules: @|bold ${modulePoms.size}|@ included, @|yellow $failureCount|@ skipped")
        append("  Dependencies: @|bold $totalDeps|@ unique dependencies")
        append("  Size: ${pklContent.length} bytes (${pklContent.lines().size} lines)")
        appendLine()
        append("@|bold,cyan 💡 Next steps:|@")
        append("  1. Review the generated elide.pkl file")
        append("  2. Run @|bold elide build|@ to build your project")
        append("  3. Customize repositories and dependencies as needed")
      }
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to write output file|@\n  ${e.message}\n\n" +
        "Tip: Check that you have write permissions for the target directory.")
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
        append("@|bold,cyan 📄 Generated elide.pkl (dry-run)|@")
        append("  Project: @|bold ${pom.artifactId}|@")
        append("  Output path: @|bold $outputPath|@")
        append("  Dependencies: ${pom.dependencies.size}")
        append("  Total lines: ${pklContent.lines().size}")
        appendLine()
        append("@|bold ${"─".repeat(80)}|@")
        append(pklContent)
        append("@|bold ${"─".repeat(80)}|@")
      }
      return null
    }

    // Check if output file exists
    if (outputPath.exists() && !force) {
      return err("@|bold,red ✗ Output file already exists|@\n  Path: $outputPath\n\n" +
        "Tip: Use @|bold --force|@ to overwrite the existing file.")
    }

    // Write output file
    try {
      outputPath.writeText(pklContent)
      output {
        appendLine()
        append("@|bold,green ✓ Successfully created elide.pkl|@")
        append("  Location: @|bold $outputPath|@")
        append("  Dependencies: @|bold ${pom.dependencies.size}|@ total")
        append("  Size: ${pklContent.length} bytes (${pklContent.lines().size} lines)")
        appendLine()
        append("@|bold,cyan 💡 Next steps:|@")
        append("  1. Review the generated elide.pkl file")
        append("  2. Run @|bold elide build|@ to build your project")
        append("  3. Customize repositories and dependencies as needed")
      }
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to write output file|@\n  ${e.message}\n\n" +
        "Tip: Check that you have write permissions for the target directory.")
    }

    return null
  }
}
