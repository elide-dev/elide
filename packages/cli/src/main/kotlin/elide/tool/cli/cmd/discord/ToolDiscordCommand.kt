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

package elide.tool.cli.cmd.discord

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine.Command
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ToolState
import elide.tool.cli.promptForLink

/** Opens the Discord invite redirect. */
@Command(
  name = "discord",
  mixinStandardHelpOptions = true,
  description = [
    "Open or show a link to join the Elide Discord server.",
  ],
  customSynopsis = [
    "elide @|bold,fg(cyan) discord|@",
    "",
  ]
)
@Introspected
@ReflectiveAccess
internal open class ToolDiscordCommand : AbstractSubcommand<ToolState, CommandContext>() {
  companion object {
    private const val REDIRECT_TARGET = "https://elide.dev/discord"
  }

  @Suppress("DEPRECATION")
  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    promptForLink(
      redirectTarget = REDIRECT_TARGET,
      forThing = "Discord",
      promptMessage = "Open link to join Discord",
    )
    return success()
  }
}
