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

import io.ktor.utils.io.streams.asInput
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.ReflectiveAccess
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import picocli.CommandLine
import picocli.CommandLine.Command
import java.nio.charset.StandardCharsets
import jakarta.inject.Inject
import jakarta.inject.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlin.io.path.name
import kotlin.io.path.readText
import elide.runtime.core.PolyglotContext
import elide.tool.cli.CommandContext
import elide.tool.cli.CommandResult
import elide.tool.cli.ProjectAwareSubcommand
import elide.tool.cli.ToolState
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tool.cli.options.McpOptions
import elide.tool.project.ProjectManager
import elide.tooling.project.ElideConfiguredProject
import elide.tooling.project.load

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
internal class McpCommand @Inject constructor(
  private val projectManagerProvider: Provider<ProjectManager>,
) : ProjectAwareSubcommand<ToolState, CommandContext>() {
  /** Settings which apply to MCP. */
  @CommandLine.ArgGroup(
    validate = false,
    heading = "%nModel Context:%n",
  )
  internal var mcpOptions: McpOptions = McpOptions()

  // Configure MCP server settings based on the current project.
  private fun buildMcpServer(): Server = Server(
    serverInfo = Implementation(
      name = "elide",
      version = ELIDE_TOOL_VERSION,
    ),
    options = ServerOptions(
      capabilities = ServerCapabilities(
        resources = ServerCapabilities.Resources(
          subscribe = false,
          listChanged = false,
        ),
        tools = ServerCapabilities.Tools(
          listChanged = false,
        ),
      ),
    ),
  )

  // Run an MCP server instance based on specified flags and project options.
  private suspend fun CommandContext.runMcp(configured: ElideConfiguredProject, accessor: () -> PolyglotContext) {
    val name = configured.manifest.name ?: configured.root.name
    val server = buildMcpServer()
    val projectConfig = configured.root.resolve("elide.pkl")
    val ctx = accessor()
    if (mcpOptions.mcpDebug) logging.info {
      "Starting MCP server for project '$name' with context '$ctx'"
    }

    // add project config unconditionally as resource
    server.addResource(
      uri = "file:///elide.pkl",
      name = "Elide Project Manifest",
      description = "Project configuration file for an Elide project",
      mimeType = "application/x-elide-pkl",
    ) { resource ->
      ReadResourceResult(
        contents = listOf(
          TextResourceContents(
            uri = resource.uri,
            mimeType = "application/x-pkl",
            text = kotlinx.coroutines.withContext(Dispatchers.IO) {
              projectConfig.readText(StandardCharsets.UTF_8)
            },
          )
        )
      )
    }

    // build and run with transport
    val (awaitClose, transport) = when {
      else -> false to StdioServerTransport(
        inputStream = System.`in`.asInput().buffered(),
        outputStream = System.out.asSink().buffered(),
      )
    }
    try {
      server.connect(transport)
      if (awaitClose) {
        while (true) {
          // Keep the server running until interrupted.
          Thread.sleep(Long.MAX_VALUE)
        }
      }
    } catch (_: InterruptedException) {
      Thread.currentThread().interrupt()
      output {
        append("MCP server interrupted; exiting")
      }
    }
  }

  override suspend fun CommandContext.invoke(state: ToolContext<ToolState>): CommandResult {
    val project = projectManagerProvider.get().resolveProject(projectOptions().projectPath())
    if (project == null) {
      return err("No project; can't start MCP without it")
    }
    val configured = project.load()
    resolveEngine().unwrap().use {
      withDeferredContext(emptySet(), shared = true) { accessor ->
        runBlocking {
          runMcp(configured, accessor)
        }
      }
    }
    return success()
  }
}
