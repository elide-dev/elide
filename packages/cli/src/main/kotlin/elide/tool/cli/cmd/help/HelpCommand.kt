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

package elide.tool.cli.cmd.help

import com.github.ajalt.mordant.markdown.Markdown
import com.github.ajalt.mordant.terminal.Terminal
import picocli.CommandLine
import java.awt.Desktop
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import elide.annotations.Singleton
import elide.tool.cli.*

/** Find help or file a bug or PR against Elide. */
@CommandLine.Command(
  name = "help",
  aliases = ["docs", "bug", "issue", "feature"],
  description = [
    "Report an issue or bug, find help for using Elide. Can search Elide's documentation at this version, and can " +
      "open links to GitHub.",
    "",
    "Examples:",
    "  elide help projects",
    "  elide feature \"I wish I could do X\"",
    "  elide docs kotlin",
    "",
    "Topics:",
    "  @|bold projects|@: Explains the concept of Elide projects",
    "  @|bold jvm|@: How Java and Kotlin projects work on Elide",
    "  @|bold polyglot|@: Polyglot apps and cross-language interop",
    "  @|bold servers|@: How to run Elide apps in server mode",
  ],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  customSynopsis = [
    "elide @|bold,fg(cyan) help|@ [TOPIC]",
    "   or: elide @|bold,fg(cyan) issue|bug|feature|@ [OPTIONS] [TITLE]",
    "   or: elide @|bold,fg(cyan) docs|@ [OPTIONS] [SEARCH]",
    "",
  ],
  subcommands = [
    HelpCommand.ProjectHelpCommand::class,
    HelpCommand.JvmHelpCommand::class,
    HelpCommand.PolyglotHelpCommand::class,
    HelpCommand.ServersHelpCommand::class,
  ],
)
@Singleton internal class HelpCommand : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private const val issuesBase: String = "https://github.com/elide-dev/elide/issues/new"
    private const val issueTemplateFeature: String = "new_feature.yaml"
    private const val issueTemplateBugReport: String = "bug_report.yaml"
    private const val templateParam = "template"
    private const val versionParam = "version"

    @JvmStatic private fun assembleIssueUrl(type: String): URI {
      val params = mapOf(
        // set the issue template based on the type of issue (new feature or bug report)
        templateParam to type,

        // set the `version` of the current tool
        versionParam to Elide.version(),
      ).map {
        "${it.key}=${it.value}"
      }.joinToString("&")

      return URI.create(
        "$issuesBase?$params"
      )
    }
  }

  internal abstract class HelpTopic (private val path: String): Callable<Unit> {
    override fun call() {
      val terminal = Terminal()
      requireNotNull(this::class.java.getResourceAsStream("/META-INF/elide/help/$path")) {
        "Failed to locate help topic at path '$path'"
      }.bufferedReader(StandardCharsets.UTF_8).use { stream ->
        terminal.println(Markdown(stream.readText()))
      }
    }
  }

  @CommandLine.Spec
  internal lateinit var spec: CommandLine.Model.CommandSpec

  /** Help topic for Elide projects. */
  @CommandLine.Command(
    name = "projects",
    mixinStandardHelpOptions = false,
    hidden = true,
  )
  internal class ProjectHelpCommand: HelpTopic(path = "projects.md")

  /** Help topic for JVM projects. */
  @CommandLine.Command(
    name = "jvm",
    aliases = ["java", "kotlin"],
    mixinStandardHelpOptions = false,
    hidden = true,
  )
  internal class JvmHelpCommand: HelpTopic(path = "jvm.md")

  /** Help topic for polyglot info. */
  @CommandLine.Command(
    name = "polyglot",
    mixinStandardHelpOptions = false,
    hidden = true,
  )
  internal class PolyglotHelpCommand: HelpTopic(path = "polyglot.md")

  /** Help topic for servers. */
  @CommandLine.Command(
    name = "servers",
    mixinStandardHelpOptions = false,
    hidden = true,
  )
  internal class ServersHelpCommand: HelpTopic(path = "servers.md")

  /** Whether to file a bug. */
  @CommandLine.Option(
    names = ["--bug"],
    description = ["Wizard to file a bug"],
    defaultValue = "false",
  )
  var fileBug: Boolean = false

  /** Whether to file a feature request. */
  @CommandLine.Option(
    names = ["--feature"],
    description = ["Wizard to file a feature request"],
    defaultValue = "false",
  )
  var fileFeatureRequest: Boolean = false

  /** Whether to open the bug report after preparing, or just print a URL. */
  @CommandLine.Option(
    names = ["--open"],
    negatable = true,
    description = ["Open the URL or print the URL"],
    defaultValue = "true",
  )
  var openUrl: Boolean = true

  @Suppress("DEPRECATION")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    if (spec.name() == "help") {
      // render help manually
      spec.commandLine().usage(System.out)
      return success()
    }
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
