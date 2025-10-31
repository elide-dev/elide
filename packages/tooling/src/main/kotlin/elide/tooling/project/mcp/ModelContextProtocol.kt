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

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.util.collections.ConcurrentMap
import io.ktor.utils.io.streams.asInput
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.SseServerTransport
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.shared.AbstractTransport
import org.graalvm.polyglot.Context
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlin.io.path.readText
import elide.runtime.Logging
import elide.tooling.project.ElideConfiguredProject

/**
 * # Model Context Protocol (MCP)
 *
 * Public API for the configuration, execution, and operation of Model Context Protocol (MCP) servers for configured
 * Elide projects. MCP provides a standardized way for AI agents to interact with software projects. This API allows
 * for the configuration and execution of an MCP server, which an AI agent can connect to and use to perform various
 * tasks within the context of the project.
 *
 * ## Supported Features
 *
 * Elide's MCP implementation supports a few features out of the box:
 *
 * - **Project Configuration**: The MCP server can read and expose the project's configuration file (e.g., `elide.pkl`).
 * - **Project Advice**: Elide can render Markdown advice about a project, and expose this via the MCP server.
 * - **Self-Tool Usage**: Elide can register itself as a tool for use by the AI agent.
 *
 * ## Configuration & Usage
 *
 * MCP services can be used directly from these methods, either via Standard I/O or HTTP. Within the context of an Elide
 * project, the `mcp { ... }` block can be used to configure MCP services. This implementation respects such options.
 */
public object ModelContextProtocol {
  /**
   * ## MCP Serving Mode
   *
   * Enumerates different serving modes for the MCP server.
   */
  public enum class McpServingMode {
    /** Operate over Standard I/O. */
    Stdio,

    /** Operate over HTTP. */
    Http,
  }

  /**
   * ## MCP Server Configuration
   *
   * Specifies types of server configurations.
   */
  public sealed interface McpServerConfig {
    /** Operating mode for the MCP server. */
    public val mode: McpServingMode
  }

  /**
   * ## MCP Server Configuration: Over Stdio
   *
   * This configuration uses standard I/O for the MCP server.
   */
  public data object McpOverStdio : McpServerConfig {
    override val mode: McpServingMode get() = McpServingMode.Stdio
  }

  /**
   * ## MCP Server Configuration: Over HTTP
   *
   * This configuration uses HTTP for the MCP server.
   *
   * @property host Host to bind the MCP server to.
   * @property port Port to bind the MCP server to.
   */
  @JvmRecord public data class McpOverHttp internal constructor (
    public val host: String,
    public val port: UShort,
  ): McpServerConfig {
    override val mode: McpServingMode get() = McpServingMode.Http

    /** Obtains instances of [McpOverHttp]. */
    public companion object {
      /** Default port to run MCP services on. */
      public const val DEFAULT_MCP_HOST: String = "localhost"

      /** Default port to run MCP services on. */
      public const val DEFAULT_MCP_PORT: UShort = 8125u

      /** @return [McpOverHttp] settings with the provided parameters. */
      @JvmStatic public fun of(host: String? = null, port: UShort? = null): McpOverHttp {
        return McpOverHttp(host ?: DEFAULT_MCP_HOST, port ?: DEFAULT_MCP_PORT)
      }
    }
  }

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
    public val serverFactory: () -> Server

    /**
     * Server configuration to use.
     */
    public val config: McpServerConfig

    /**
     * Launch and Wait for MCP Server
     *
     * @param awaitClose Whether to keep the server running until interrupted.
     * @param connect Whether to connect the server to a transport.
     * @param transport Transport to use for the MCP server.
     */
    public suspend fun launchAndWait(awaitClose: Boolean, connect: Boolean, transport: AbstractTransport) {
      try {
        if (connect) {
          serverFactory().connect(transport)
        }
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

    private fun httpServer(
      config: McpOverHttp,
      start: Boolean = true,
      awaitClose: Boolean = true,
    ): EmbeddedServer<*, *> {
      val logging = Logging.of(McpServer::class)
      val servers = ConcurrentMap<String, Pair<Server, Int>>()
      val transports = ConcurrentMap<Int, SseServerTransport>()
      val transportOracle = AtomicInteger(0)

      return embeddedServer(CIO, host = config.host, port = config.port.toInt()) {
        install(SSE)

        routing {
          sse("/sse") {
            val transport = SseServerTransport("/message", this)
            val transportId = transportOracle.incrementAndGet()
            transports[transportId] = transport

            val server = serverFactory()
            servers[transport.sessionId] = (server to transportId)

            server.onClose {
              logging.info("Server closed")
            }
            server.connect(transport)
          }

          post("/message") {
            val sessionId: String? = call.request.queryParameters["sessionId"]
            if (sessionId == null) {
              call.respond(HttpStatusCode.BadRequest, "Missing sessionId query parameter")
              return@post
            }
            val serverBundle = servers[sessionId]
            if (serverBundle == null) {
              call.respond(HttpStatusCode.NotFound, "Session not found")
              return@post
            }
            val (_, transportId) = serverBundle
            val transport = transports[transportId]
            if (transport == null) {
              call.respond(HttpStatusCode.NotFound, "Transport not found")
              return@post
            }
            transport.handlePostMessage(call)
          }
        }
      }.apply {
        if (start) {
          logging.info("Starting MCP services over HTTP at ${config.host}:${config.port}")
          start(wait = awaitClose)
        }
      }
    }

    /**
     * Launch and Wait for MCP Server using standard I/O.
     *
     * @param awaitClose Whether to keep the server running until interrupted.
     */
    public suspend fun launchAndWait(config: McpServerConfig, awaitClose: Boolean = true) {
       when (config) {
        is McpOverStdio -> awaitClose to StdioServerTransport(
          inputStream = System.`in`.asInput().buffered(),
          outputStream = System.out.asSink().buffered(),
        )
        is McpOverHttp -> {
          httpServer(config, start = awaitClose, awaitClose = awaitClose)
          return
        }
      }.also { (shouldAwait, transport) ->
         launchAndWait(shouldAwait, awaitClose, transport)
       }
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
        uri = "file://${projectConfig.toAbsolutePath()}",
        name = "Elide Project Manifest",
        description = "Project configuration file for an Elide project",
        mimeType = "application/x-pkl",
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
    McpContributor.all().toList().apply {
      object: McpContributor.McpContext {
        override val server: Server get() = server
        override suspend fun project(): ElideConfiguredProject? = configured
      }.let { ctx ->
        filter {
          it.enabled(ctx)
        }.forEach {
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
      override val serverFactory: () -> Server get() = { active }
      override val config: McpServerConfig get() = McpOverStdio
    }
  }
}
