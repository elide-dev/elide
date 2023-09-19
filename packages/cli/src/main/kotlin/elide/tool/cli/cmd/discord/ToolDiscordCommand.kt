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

package elide.tool.cli.cmd.discord

import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.awt.Desktop
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import elide.annotations.Singleton
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState

/** Opens the Discord invite redirect. */
@Command(
  name = "discord",
  description = ["Open or show a Discord invite link"],
  mixinStandardHelpOptions = true,
)
@Singleton internal class ToolDiscordCommand : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private const val REDIRECT_TARGET = "https://elide.dev/discord"
    private val REDIRECT_URI = URI(REDIRECT_TARGET)
  }

  /** Whether to open the link in the default browser (defaults to `true`). */
  @Option(
    names = ["--open"],
    description = ["Open the link, if possible"],
    defaultValue = "true",
  )
  var openLink: Boolean = true

  @Suppress("DEPRECATION")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val printLink: () -> Unit = {
      println("Open link to join Discord: $REDIRECT_TARGET")
    }
    if (openLink) withContext(Dispatchers.IO) {
      val os = System.getProperty("os.name", "unknown").lowercase()

      when {
        Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE) ->
          Desktop.getDesktop().browse(REDIRECT_URI)

        os.contains("windows") -> {
          Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $REDIRECT_TARGET")
        }
        os.contains("mac") || os.contains("darwin") -> {
          Runtime.getRuntime().exec("open $REDIRECT_TARGET")
        }
        else -> printLink.invoke()
      }
    } else printLink.invoke()
    return success()
  }
}
