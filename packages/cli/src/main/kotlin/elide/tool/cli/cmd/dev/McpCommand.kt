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
package elide.tool.cli.cmd.dev

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import picocli.CommandLine
import picocli.CommandLine.Command
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.runBlocking
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.options.McpOptions
import elide.tooling.project.ProjectManager
import elide.tooling.project.load
import elide.tooling.project.mcp.ModelContextProtocol
import elide.tooling.project.mcp.ModelContextProtocol.build
import elide.tooling.project.mcp.ModelContextProtocol.McpOverHttp
import elide.tooling.project.mcp.ModelContextProtocol.McpOverStdio
import elide.tooling.project.mcp.ModelContextProtocol.McpServingMode.Http
import elide.tooling.project.mcp.ModelContextProtocol.McpServingMode.Stdio

/** Starts an MCP for an Elide project. */
@Command(
  name = "mcp",
  description = ["Run an MCP instance for an Elide project."],
  mixinStandardHelpOptions = true,
  showDefaultValues = true,
  abbreviateSynopsis = true,
  usageHelpAutoWidth = true,
  synopsisHeading = "",
)
@Introspected
@ReflectiveAccess
internal class McpCommand : ProjectAwareSubcommand<ToolState, CommandContext>() {
  @Inject private lateinit var projectManagerProvider: Provider<ProjectManager>

  /** Settings which apply to MCP. */
  @CommandLine.ArgGroup(
    validate = false,
    heading = "%nModel Context:%n",
  )
  internal var mcpOptions: McpOptions = McpOptions()

  /** Whether to wait for server exit. */
  @CommandLine.Option(
    names = ["--wait"],
    description = ["Wait for the MCP to exit before returning."],
    defaultValue = "true",
    negatable = true,
  )
  internal var waitForMcp: Boolean = true

  /** Server mode to use for the MCP. */
  @CommandLine.Option(
    names = ["--server"],
    description = ["Server mode to use for MCP services."],
  )
  internal var serverMode: ModelContextProtocol.McpServingMode? = null

  /** Hostname to use when operating over HTTP. */
  @CommandLine.Option(
    names = ["--http:host"],
    description = ["Hostname to use when operating over HTTP."],
  )
  internal var httpHost: String? = null

  /** Port to use when operating over HTTP. */
  @CommandLine.Option(
    names = ["--http:port"],
    description = ["Port to use when operating over HTTP."],
  )
  internal var httpPort: Int? = null

  // Configure a transport to use for MCP.
  private fun configureTransport(): ModelContextProtocol.McpServerConfig = when (serverMode) {
    Stdio -> McpOverStdio
    Http -> McpOverHttp.of(host = httpHost, port = httpPort?.toUShort())
    else -> McpOverStdio
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult = success().also {
    withDeferredContext(emptySet(), shared = true) { accessor ->
      runBlocking {
        ModelContextProtocol.configure(
          projectManagerProvider.get().resolveProject(projectOptions().projectPath())?.load(),
          { accessor().unwrap() },
          version = ELIDE_TOOL_VERSION,
          debug = mcpOptions.mcpDebug,
        ).apply {
          build().launchAndWait(configureTransport(), awaitClose = waitForMcp)
        }
      }
    }
  }
}
