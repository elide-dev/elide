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
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/**
 * Adopt Python project configuration to elide.pkl.
 *
 * This command parses Python project configuration files and generates an
 * equivalent elide.pkl configuration file. It handles:
 * - pyproject.toml (PEP 621)
 * - requirements.txt
 * - requirements-dev.txt / dev-requirements.txt
 * - Basic project metadata (name, version, description)
 * - Dependencies (production and development)
 * - Optional dependencies (documented as comments)
 * - Python version requirements
 * - Entry points/scripts (documented as comments)
 */
@Command(
  name = "python",
  mixinStandardHelpOptions = true,
  description = [
    "Adopt Python project configuration to elide.pkl.",
  ],
  customSynopsis = [
    "elide adopt @|bold,fg(cyan) python|@ [CONFIG_FILE]",
    "elide adopt @|bold,fg(cyan) python|@ [OPTIONS] [CONFIG_FILE]",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal class PythonAdoptCommand : AbstractSubcommand<ToolState, CommandContext>() {
  @Parameters(
    index = "0",
    arity = "0..1",
    description = [
      "Path to Python configuration file (pyproject.toml or requirements.txt)",
      "If omitted, will search for pyproject.toml or requirements.txt in current directory"
    ]
  )
  var configFile: String? = null

  @Option(
    names = ["--output", "-o"],
    description = ["Output file path (default: elide.pkl in same directory as config file)"]
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
    names = ["--python-version"],
    description = ["Override Python version requirement (e.g., >=3.11)"]
  )
  var pythonVersion: String? = null

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    // Determine configuration file path
    val configPath = when {
      configFile != null -> {
        val path = Path.of(configFile).absolute()
        if (path.isDirectory()) {
          // If directory provided, search for config files
          detectConfigFile(path)
        } else {
          path
        }
      }
      else -> detectConfigFile(Path.of(".").absolute())
    }

    if (configPath == null) {
      return err("@|bold,red ✗ No Python configuration file found|@\n\n" +
        "Tip: Expected pyproject.toml or requirements.txt in current directory.\n" +
        "     You can specify a file path: elide adopt python /path/to/pyproject.toml")
    }

    // Validate configuration file exists
    if (!configPath.exists()) {
      return err("Python configuration file not found: $configPath")
    }

    val configFileName = configPath.fileName.toString()
    output {
      append("@|bold,cyan 🐍 Parsing Python configuration...|@")
      append("  File: @|bold $configFileName|@")
      append("  Path: ${configPath.parent}")
    }

    // Parse Python configuration
    val descriptor = try {
      when {
        configFileName == "pyproject.toml" -> {
          val parsed = PyProjectParser.parse(configPath)
          // Extract dev dependencies from optional-dependencies
          PyProjectParser.extractDevDependencies(parsed)
        }
        configFileName.matches(Regex("requirements.*\\.txt", RegexOption.IGNORE_CASE)) -> {
          RequirementsTxtParser.detectAndParse(configPath.parent, configPath.parent.fileName.toString())
            ?: return err("@|bold,red ✗ Failed to parse requirements.txt|@")
        }
        else -> return err("@|bold,red ✗ Unsupported configuration file|@: $configFileName\n\n" +
          "Supported files: pyproject.toml, requirements.txt")
      }
    } catch (e: Exception) {
      return err("@|bold,red ✗ Failed to parse Python configuration|@\n  ${e.message}\n\n" +
        "Tip: Ensure the configuration file is valid.")
    }

    // Override Python version if specified
    val finalDescriptor = if (pythonVersion != null) {
      descriptor.copy(pythonVersion = pythonVersion)
    } else {
      descriptor
    }

    output {
      append("@|bold,green ✓ Parsed Python configuration successfully|@")
      append("  Project: @|bold ${finalDescriptor.name}|@")
      if (finalDescriptor.version != null) {
        append("  Version: ${finalDescriptor.version}")
      }
      if (finalDescriptor.pythonVersion != null) {
        append("  Python: ${finalDescriptor.pythonVersion}")
      }
      append("  Dependencies: ${finalDescriptor.dependencies.size} package(s)")
      if (finalDescriptor.devDependencies.isNotEmpty()) {
        append("  Dev Dependencies: ${finalDescriptor.devDependencies.size} package(s)")
      }
      if (finalDescriptor.hasOptionalDependencies()) {
        append("  Optional Dependencies: ${finalDescriptor.optionalDependencies.size} group(s)")
      }
    }

    // Generate PKL content
    val pklContent = PklGenerator.generateFromPython(finalDescriptor)

    // Determine output path
    val outputPath = when {
      outputFile != null -> Path.of(outputFile).absolute()
      else -> configPath.parent.resolve("elide.pkl")
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
      append("  2. Adjust Python version if needed")
      if (finalDescriptor.hasOptionalDependencies()) {
        append("  3. Review optional dependencies (currently documented as comments)")
      }
      if (finalDescriptor.hasScripts()) {
        append("  4. Review entry points/scripts (currently documented as comments)")
      }
    }

    return success()
  }

  /**
   * Detect Python configuration file in a directory.
   *
   * Priority:
   * 1. pyproject.toml
   * 2. requirements.txt
   *
   * @param directory Directory to search
   * @return Path to configuration file or null if not found
   */
  private fun detectConfigFile(directory: Path): Path? {
    // Check for pyproject.toml first (modern standard)
    val pyproject = directory.resolve("pyproject.toml")
    if (pyproject.exists() && PyProjectParser.isValidPyProjectToml(pyproject)) {
      return pyproject
    }

    // Check for requirements.txt
    val requirements = directory.resolve("requirements.txt")
    if (requirements.exists()) {
      return requirements
    }

    return null
  }
}
