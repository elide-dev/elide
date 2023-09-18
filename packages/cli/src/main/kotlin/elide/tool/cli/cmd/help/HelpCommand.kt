/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.tool.cli.cmd.help

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.awt.Desktop
import java.net.URI
import java.net.URL
import elide.tool.cli.*
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.cmd.discord.ToolDiscordCommand
import elide.tool.cli.cmd.discord.ToolDiscordCommand.Companion

/** Find help or file a bug or PR against Elide. */
@Command(
  name = "help",
  aliases = ["bug", "issue"],
  description = ["%nReport an issue or bug, find help for using Elide"],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
)
internal class HelpCommand : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private const val issuesBase: String = "https://github.com/elide-dev/elide/issues/new"
    private const val issueTemplateFeature: String = "new_feature.yaml"
    private const val issueTemplateBugReport: String = "bug_report.yaml"
    private const val templateParam = "template"
    private const val labelsParam = "label"
    private const val versionParam = "version"

    @JvmStatic private fun assembleIssueUrl(type: String): URI {
      val params = mapOf(
        // set the issue template based on the type of issue (new feature or bug report)
        templateParam to type,

        // set the `version` of the current tool
        versionParam to ElideTool.version(),
      ).map {
        "${it.key}=${it.value}"
      }.joinToString("&")

      return URI.create(
        "$issuesBase?$params"
      )
    }
  }

  /** Whether to file a bug. */
  @Option(
    names = ["--bug"],
    description = ["Wizard to file a bug"],
    defaultValue = "false",
  )
  var fileBug: Boolean = false

  /** Whether to file a feature request. */
  @Option(
    names = ["--feature"],
    description = ["Wizard to file a feature request"],
    defaultValue = "false",
  )
  var fileFeatureRequest: Boolean = false

  /** Whether to open the bug report after preparing, or just print a URL. */
  @Option(
    names = ["--open"],
    negatable = true,
    description = ["Open the URL or print the URL"],
    defaultValue = "true",
  )
  var openUrl: Boolean = true

  @Suppress("DEPRECATION")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    assembleIssueUrl(if (fileBug) issueTemplateBugReport else issueTemplateFeature).let { issueUrl ->
      when (openUrl) {
        false -> output {
          appendLine("Open URL to continue: $issueUrl")
        }

        true -> output {
          appendLine("Opening new issue...")

          val os = System.getProperty("os.name", "unknown").lowercase()

          when {
            Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ->
              Desktop.getDesktop().browse(issueUrl)

            os.contains("windows") -> {
              Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler \"$issueUrl\"")
            }

            os.contains("mac") || os.contains("darwin") -> {
              Runtime.getRuntime().exec("open \"$issueUrl\"")
            }

            else -> {}
          }
        }
      }
    }
    return success()
  }
}
