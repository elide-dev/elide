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
package elide.tooling.project.mcp

import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import org.graalvm.polyglot.Context
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlin.io.path.readText
import elide.tooling.project.ElideConfiguredProject

/**
 * # Model Context Protocol (MCP)
 */
public object ModelContextProtocol {
  /**
   * ## MCP Configuration
   */
  public interface McpConfiguration {
    /**
     * Factory which can create a polyglot execution context.
     */
    public val contextFactory: () -> Context

    /**
     * Factory which can produce a configured Elide project.
     */
    public val project: () -> ElideConfiguredProject?

    /**
     * Running Elide version.
     */
    public val elideVersion: String

    /**
     * Whether to enable debug mode.
     */
    public val debug: Boolean
  }

  /**
   * ## MCP Server
   */
  public interface McpServer {
    /**
     * Server which is running MCP facilities.
     */
    public val server: Server

    /**
     * Launch and Wait for MCP Server
     *
     * @param awaitClose Whether to keep the server running until interrupted.
     * @param transport Transport to use for the MCP server.
     */
    public suspend fun launchAndWait(awaitClose: Boolean, transport: AbstractTransport) {
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
      }
    }

    /**
     * Launch and Wait for MCP Server using standard I/O.
     *
     * @param awaitClose Whether to keep the server running until interrupted.
     */
    public suspend fun launchAndWaitStdio(awaitClose: Boolean = true) {
      // build and run with transport
      val (awaitClose, transport) = when {
        else -> false to StdioServerTransport(
          inputStream = System.`in`.asInput().buffered(),
          outputStream = System.out.asSink().buffered(),
        )
      }
      return launchAndWait(awaitClose, transport)
    }
  }

  // Configure MCP server settings based on the current project.
  private fun buildMcpServer(version: String): Server = Server(
    serverInfo = Implementation(
      name = "elide",
      version = version,
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
  private suspend fun McpConfiguration.buildServer(configured: ElideConfiguredProject?): Server {
    val server = buildMcpServer(elideVersion)
    val projectConfig = configured?.root?.resolve("elide.pkl")

    // add project config as resource
    if (projectConfig != null) {
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
    }

    // configure via all installed mcp contributors
    McpContributor.all().apply {
      object: McpContributor.McpContext {
        override val server: Server get() = server
      }.let { ctx ->
        forEach {
          it.contribute(ctx)
        }
      }
    }
    return server
  }

  /**
   * ## Configure an MCP Context
   *
   * @param configured Configured Elide project to use for MCP.
   * @param contextFactory Factory which can produce a polyglot execution context.
   * @param version Running Elide version.
   * @param debug Whether to enable debug mode.
   * @return MCP configuration instance.
   */
  public fun configure(
    configured: ElideConfiguredProject?,
    contextFactory: () -> Context,
    version: String,
    debug: Boolean = false,
  ): McpConfiguration = object: McpConfiguration {
    override val contextFactory: () -> Context = contextFactory
    override val project: () -> ElideConfiguredProject? = { configured }
    override val elideVersion: String = version
    override val debug: Boolean = debug
  }

  /**
   * ## Launch a Configured MCP Server
   *
   * @receiver [McpConfiguration] Configuration to use for the MCP server.
   * @return [McpServer] Running MCP server instance.
   */
  public suspend fun McpConfiguration.build(): McpServer = buildServer(configured = project()).let { active ->
    object: McpServer {
      override val server: Server get() = active
    }
  }
}
