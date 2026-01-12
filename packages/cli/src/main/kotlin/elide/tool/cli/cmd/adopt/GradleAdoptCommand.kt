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
 * Adopt Gradle build files to elide.pkl.
 *
 * This command parses Gradle build files and generates an equivalent elide.pkl
 * configuration file. It handles:
 * - Groovy DSL (build.gradle) and Kotlin DSL (build.gradle.kts)
 * - Basic project metadata (name, group, version, description)
 * - Dependencies (implementation, testImplementation, etc.)
 * - Repositories (mavenCentral, google, custom maven repos)
 * - Multi-module projects (via settings.gradle[.kts])
 * - Plugin detection
 */
@Command(
  name = "gradle",
  mixinStandardHelpOptions = true,
  description = [
    "Adopt Gradle build files to elide.pkl.",
  ],
  customSynopsis = [
    "elide adopt @|bold,fg(cyan) gradle|@ [BUILD_FILE]",
    "elide adopt @|bold,fg(cyan) gradle|@ [OPTIONS] [BUILD_FILE]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class GradleAdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = ["Path to build.gradle[.kts] file (default: build.gradle.kts or build.gradle in current directory)"]
  )
  var buildFile: String? = null

  @Option(
    names = ["--output", "-o"],
    description = ["Output file path (default: elide.pkl in same directory as build file)"]
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
    names = ["--skip-subprojects"],
    description = ["Skip processing multi-module subprojects (only convert the root project)"]
  )
  var skipSubprojects: Boolean = false

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // Determine build file path
    val buildFilePath = when {
      buildFile != null -> Path.of(buildFile).absolute()
      else -> {
        // Try build.gradle.kts first, then build.gradle
        val kts = Path.of("build.gradle.kts").absolute()
        val groovy = Path.of("build.gradle").absolute()
        when {
          kts.exists() -> kts
          groovy.exists() -> groovy
          else -> return err("@|bold,red ✗ No Gradle build file found|@\n\n" +
            "Tip: Run this command from a Gradle project directory, or specify the build file path.")
        }
      }
    }

    // Validate build file exists
    if (!buildFilePath.exists()) {
      return err("@|bold,red ✗ Build file not found|@\n  Path: $buildFilePath\n\n" +
        "Tip: Ensure the build file exists and the path is correct.")
    }

    val isKotlinDsl = buildFilePath.fileName.toString().endsWith(".kts")

    output {
      append("@|bold,cyan 📋 Parsing Gradle build file...|@")
      append("  File: @|bold ${buildFilePath.fileName}|@ ${if (isKotlinDsl) "@|cyan (Kotlin DSL)|@" else "@|yellow (Groovy DSL)|@"}")
      append("  Path: ${buildFilePath.parent}")
    }

    // Parse build file
    val rootProject = try {
      GradleParser.parse(buildFilePath)
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to parse Gradle build file|@\n  ${e.message}\n\n" +
        "Tip: Ensure the build file is valid Gradle syntax. Complex builds may require manual conversion.")
    }

    // Check if this is a multi-module project
    val isMultiModule = rootProject.modules.isNotEmpty()

    output {
      appendLine()
      append("@|bold,green ✓ Project parsed successfully|@")
      append("  Name: @|bold ${rootProject.name}|@")
      if (rootProject.group.isNotBlank()) {
        append("  Group: @|cyan ${rootProject.group}|@")
      }
      if (rootProject.version != "unspecified" && rootProject.version.isNotBlank()) {
        append("  Version: ${rootProject.version}")
      }
      if (rootProject.description != null) {
        val desc = if (rootProject.description!!.length > 80) {
          rootProject.description!!.take(77) + "..."
        } else {
          rootProject.description
        }
        append("  Description: $desc")
      }
      appendLine()

      if (isMultiModule) {
        append("@|bold,magenta 📦 Multi-module project|@")
        append("  Modules: @|bold ${rootProject.modules.size}|@")
        if (rootProject.modules.size <= 10) {
          rootProject.modules.forEach { module ->
            append("    - $module")
          }
        } else {
          rootProject.modules.take(10).forEach { module ->
            append("    - $module")
          }
          append("    ... and ${rootProject.modules.size - 10} more")
        }
        appendLine()
      }

      val compileDeps = rootProject.dependencies.count { !it.isTestScope() }
      val testDeps = rootProject.dependencies.count { it.isTestScope() }
      append("@|bold 📚 Dependencies|@")
      append("  Total: @|bold ${rootProject.dependencies.size}|@ (@|cyan ${compileDeps} compile|@, @|yellow ${testDeps} test|@)")
      if (rootProject.repositories.isNotEmpty()) {
        append("  Repositories: ${rootProject.repositories.size}")
      }

      if (rootProject.plugins.isNotEmpty()) {
        appendLine()
        append("@|bold 🔌 Plugins|@")
        append("  Count: @|bold ${rootProject.plugins.size}|@")
        if (rootProject.plugins.size <= 5) {
          rootProject.plugins.forEach { plugin ->
            val pluginStr = if (plugin.version != null) {
              "${plugin.id}:${plugin.version}"
            } else {
              plugin.id
            }
            append("    - $pluginStr")
          }
        } else {
          rootProject.plugins.take(5).forEach { plugin ->
            val pluginStr = if (plugin.version != null) {
              "${plugin.id}:${plugin.version}"
            } else {
              plugin.id
            }
            append("    - $pluginStr")
          }
          append("    ... and ${rootProject.plugins.size - 5} more")
        }
      }
    }

    // Handle multi-module projects
    if (isMultiModule && !skipSubprojects) {
      return convertMultiModuleProject(rootProject, buildFilePath)
    }

    // Handle single-module project
    return convertSingleProject(rootProject, buildFilePath) ?: success()
  }

  /**
   * Convert a multi-module Gradle project to a single root elide.pkl with workspaces.
   */
  private suspend fun CommandContext.convertMultiModuleProject(rootProject: GradleDescriptor, buildFilePath: Path): CommandResult {
    output {
      appendLine()
      append("@|bold,cyan 🔍 Processing subprojects...|@")
      append("  Total modules: @|bold ${rootProject.modules.size}|@")
      appendLine()
    }

    val subprojects = mutableListOf<GradleDescriptor>()
    var failureCount = 0

    // Parse all subproject build files
    for ((index, moduleName) in rootProject.modules.withIndex()) {
      val moduleBuildFileKts = buildFilePath.parent.resolve(moduleName).resolve("build.gradle.kts")
      val moduleBuildFileGroovy = buildFilePath.parent.resolve(moduleName).resolve("build.gradle")

      val moduleBuildFile = when {
        moduleBuildFileKts.exists() -> moduleBuildFileKts
        moduleBuildFileGroovy.exists() -> moduleBuildFileGroovy
        else -> {
          output {
            append("  @|yellow ⚠|@ Module @|bold $moduleName|@ has no build file")
          }
          failureCount++
          continue
        }
      }

      try {
        val subproject = GradleParser.parse(moduleBuildFile)
        subprojects.add(subproject)
        output {
          append("  @|green ✓|@ [@|bold ${index + 1}/${rootProject.modules.size}|@] ${subproject.name}")
        }
      } catch (e: Exception) {
        failureCount++
        output {
          append("  @|red ✗|@ [@|bold ${index + 1}/${rootProject.modules.size}|@] Failed to parse @|bold $moduleName|@")
          append("      Error: ${e.message}")
        }
      }
    }

    if (subprojects.isEmpty() && rootProject.dependencies.isEmpty()) {
      return err("@|bold,red ✗ No subprojects could be parsed successfully|@\n\n" +
        "Tip: Verify that the subproject directories exist and contain valid build files.")
    }

    output {
      appendLine()
      append("@|bold,green ✓ Parsed ${subprojects.size} subproject(s) successfully|@")
      if (failureCount > 0) {
        append("  @|yellow Skipped $failureCount subproject(s) due to errors|@")
      }
      appendLine()
    }

    // Generate single root elide.pkl with workspaces
    val pklContent = PklGenerator.generateMultiModule(rootProject, subprojects)

    // Determine output path (always at root for multi-module)
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> buildFilePath.parent.resolve("elide.pkl")
    }

    // Handle dry run
    if (dryRun) {
      output {
        append("@|bold,cyan 📄 Generated multi-module elide.pkl (dry-run)|@")
        append("  Output path: @|bold $outputPath|@")
        append("  Modules: ${subprojects.size}")
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

      val totalDeps = (rootProject.dependencies + subprojects.flatMap { it.dependencies })
        .distinctBy { it.coordinate() }
        .size

      output {
        appendLine()
        append("@|bold,green ✓ Successfully created multi-module elide.pkl|@")
        append("  Location: @|bold $outputPath|@")
        append("  Modules: @|bold ${subprojects.size}|@ included, @|yellow $failureCount|@ skipped")
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
   * Convert a single Gradle project to elide.pkl.
   * Returns an error result if conversion fails, null if successful.
   */
  private suspend fun CommandContext.convertSingleProject(project: GradleDescriptor, buildFilePath: Path): CommandResult? {
    // Generate elide.pkl content
    val pklContent = PklGenerator.generate(project)

    // Determine output path
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> buildFilePath.parent.resolve("elide.pkl")
    }

    // Handle dry run
    if (dryRun) {
      output {
        appendLine()
        append("@|bold,cyan 📄 Generated elide.pkl (dry-run)|@")
        append("  Project: @|bold ${project.name}|@")
        append("  Output path: @|bold $outputPath|@")
        append("  Dependencies: ${project.dependencies.size}")
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
        append("  Dependencies: @|bold ${project.dependencies.size}|@ total")
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
