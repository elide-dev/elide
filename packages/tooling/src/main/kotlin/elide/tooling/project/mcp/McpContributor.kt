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

import elide.tooling.project.ElideConfiguredProject
import io.modelcontextprotocol.kotlin.sdk.server.Server

/**
 * ## Model Context Protocol (MCP) Contributor
 *
 * Describes a contributor implementation which adds Model Context Protocol (MCP) support for some aspect of an Elide
 * project; contributors are loaded by the Elide tooling framework and consulted during construction of MCP servers and
 * integrations.
 */
public interface McpContributor {
  /**
   * ### MCP Contributor Context
   *
   * Describes the calling context (receiver) for a given MCP contributor.
   */
  public interface McpContext {
    /**
     * MCP server which is under configuration.
     */
    public val server: Server

    /**
     * Configured Elide project which we are working with.
     *
     * @return The configured Elide project, if available; `null` if no project is available.
     */
    public suspend fun project(): ElideConfiguredProject?
  }

  /**
   * Contribute to the MCP server context under construction.
   *
   * @param context Context for the MCP server being constructed, which may be used to contribute capabilities,
   *   extensions, or other features to the server.
   */
  public suspend fun contribute(context: McpContext)

  /** Resolvers for MCP contributors. */
  public companion object {
    /** @return All visible MCP contributors. */
    @JvmStatic public fun all(): Sequence<McpContributor> = sequence {
      // Load all contributors from the classpath
      val serviceLoader = java.util.ServiceLoader.load(McpContributor::class.java)
      for (contributor in serviceLoader) {
        yield(contributor)
      }
    }
  }
}
