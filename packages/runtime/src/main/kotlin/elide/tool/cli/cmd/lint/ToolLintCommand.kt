/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.tool.cli.cmd.lint

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.io.WorkdirManager
import elide.tool.project.ProjectManager

/** Interactive REPL entrypoint for Elide on the command-line. */
@Command(
  name = "lint",
  aliases = ["check"],
  description = ["%nRun polyglot linters on your code"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  hidden = true,
)
@Suppress("unused", "UnusedPrivateProperty")
@Singleton internal class ToolLintCommand : AbstractSubcommand<ToolState, CommandContext>() {
  /**
   * Tools to run with the current linter invocation; optional.
   *
   * There is a special value, `auto`, which automatically selects the appropriate tools to use; otherwise, the suite of
   * tools is loaded and run as described (comma-separated or multiple argument forms are supported).
   */
  @Option(
    names = ["-t", "--tool"],
    description = ["The linter tool(s) to use"],
    defaultValue = "auto",
    arity = "0..N",
  )
  private var tools: List<String> = emptyList()

  /**
   * List supported tools, and their versions, and exit.
   */
  @Option(
    names = ["-l", "--list-tools"],
    description = ["List supported tools and exit; supports the format parameter"],
  )
  private var listTools: Boolean = false

  /**
   * Paths to apply to the linter; optional.
   *
   * By default, all source files are scanned, modulo the current ignore configuration and file.
   */
  @Parameters(
    index = "0",
    description = ["The path to the file or directory to lint"],
    defaultValue = ".",
    arity = "0..N",
  )
  private var paths: List<String> = emptyList()

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val version = dev.elide.cli.bridge.CliNativeBridge.apiVersion()
    val tools = dev.elide.cli.bridge.CliNativeBridge.supportedTools()
    val versions = tools.associateWith { dev.elide.cli.bridge.CliNativeBridge.toolVersion(it) }

    if (listTools) {
      output {
        append("Supported tools (API: $version):")
        for ((tool, toolVersion) in versions) {
          append("  - $tool (version: $toolVersion)")
        }
      }
      return success()
    }

    output {
      append("Running linter (testing, version: $version)")
    }
    return success()
  }
}
