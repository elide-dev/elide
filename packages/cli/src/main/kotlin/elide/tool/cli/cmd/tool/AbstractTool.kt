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

package elide.tool.cli.cmd.tool

import java.net.URI
import elide.runtime.Logging
import elide.tool.Tool
import elide.tool.cli.AbstractSubcommand
import elide.tool.cli.AbstractSubcommand.ToolContext
import elide.tool.cli.CommandContext
import elide.tool.cli.ToolState

/**
 * ## Abstract tool.
 *
 * Defines the extension point for tools which are invocable via Elide through embedded support. Such tools carry the
 * metadata of a regular command-line tool, but also provide the adapted ability to actually invoke the tool, regardless
 * of how that invocation takes place.
 */
abstract class AbstractTool protected constructor (
  override val info: Tool.CommandLineTool,
) : Tool.EmbeddedTool {
  override val name: String get() = info.name
  override val label: String get() = info.label
  override val description: String get() = info.description
  override val docs: URI get() = info.docs
  override val version: String get() = info.version
  override fun supported(): Boolean = false
  @PublishedApi internal open val logging by lazy { Logging.of(AbstractTool::class) }

  /** Wraps command-line state for access by an [AbstractTool] implementation. */
  @JvmRecord data class EmbeddedToolState(
    val cmd: ToolContext<ToolState>,
  )

  // Internal method to trigger tool delegation.
  suspend inline fun delegateToTool(ctx: CommandContext, state: ToolContext<ToolState>): Tool.Result {
    logging.debug { "Invoking delegated tool '${info.name}'" }
    return ctx.invoke(EmbeddedToolState(state))
  }

  /**
   * Invoke this tool, as-configured.
   *
   * This method is the point of entry to actually run the tool implementation, with the configuration currently held by
   * the object; by the time this call occurs, all arguments, environment, and streams, must be bound and ready, where
   * applicable.
   *
   * @param state State provided to the embedded tool.
   * @return Result of the tool invocation.
   */
  abstract suspend operator fun CommandContext.invoke(state: EmbeddedToolState): Tool.Result

  /**
   * ## Tool error.
   *
   * Special exception type which is caught and turned into a command exit.
   */
  open class EmbeddedToolError(
    internal val tool: Tool.CommandLineTool,
    internal val exitCode: Int,
    message: String,
    cause: Throwable? = null,
  ): RuntimeException(message, cause) {
    fun render(ctx: AbstractSubcommand.OutputController) {
      ctx.error(
        "Failed to run '${tool.name}': $message",
        if (ctx.settings.verbose) cause else null,
      )
    }
  }

  companion object {
    /** Throw a tool error which can be converted into a proper output and exit code. */
    @JvmStatic fun embeddedToolError(
      tool: Tool.CommandLineTool,
      message: String = "Embedded tool execution failed.",
      exitCode: Int = 1,
      cause: Throwable? = null,
    ): Nothing = throw EmbeddedToolError(
      tool = tool,
      exitCode = exitCode,
      message = message,
      cause = cause,
    )
  }
}
