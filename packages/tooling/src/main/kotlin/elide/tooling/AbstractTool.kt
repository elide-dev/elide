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
package elide.tooling

import java.net.URI
import java.nio.file.Path
import elide.runtime.Logger
import elide.runtime.Logging
import elide.tooling.project.ElideProject

/**
 * ## Abstract tool.
 *
 * Defines the extension point for tools which are invocable via Elide through embedded support. Such tools carry the
 * metadata of a regular command-line tool, but also provide the adapted ability to actually invoke the tool, regardless
 * of how that invocation takes place.
 */
public abstract class AbstractTool protected constructor (
  override val info: Tool.CommandLineTool,
) : Tool.EmbeddedTool {
  override val name: String get() = info.name
  override val label: String get() = info.label
  override val description: String get() = info.description
  override val docs: URI get() = info.docs
  override val version: String get() = info.version
  override fun supported(): Boolean = false

  @PublishedApi internal open val logging: Logger by lazy { Logging.of(AbstractTool::class) }

  /** Wraps command-line state for access by an [AbstractTool] implementation. */
  public interface EmbeddedToolState {
    public val resourcesPath: Path
    public val project: ElideProject? get() = null
  }

  // Internal method to trigger tool delegation.
  public suspend inline fun delegateToTool(resourcesPath: Path): Tool.Result {
    logging.debug { "Invoking delegated tool '${info.name}'" }
    return invoke(object : EmbeddedToolState {
      override val resourcesPath: Path get() = resourcesPath
    })
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
  public abstract suspend operator fun invoke(state: EmbeddedToolState): Tool.Result

  /**
   * ## Tool error.
   *
   * Special exception type which is caught and turned into a command exit.
   */
  public open class EmbeddedToolError(
    public val tool: Tool.CommandLineTool,
    public val exitCode: Int,
    message: String,
    cause: Throwable? = null,
  ) : RuntimeException(message, cause)

  public companion object {
    /** Throw a tool error which can be converted into a proper output and exit code. */
    @JvmStatic public fun embeddedToolError(
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
