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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * ## Model Context Protocol - Project Configuration
 *
 * Describes the structure of a local `.mcp.json` file, which informs AI assistants about Elide's MCP capabilities.
 *
 * @property mcpServers Defined MCP servers.
 */
@Serializable public data class McpProjectConfig(
  public val mcpServers: Map<String, McpServerType>,
) {
  public companion object {
    public const val MCP_CONFIG_FILE: String = ".mcp.json"

    @JvmStatic public fun createForElide(): McpProjectConfig = McpProjectConfig(
      mcpServers = mapOf(
        "elide" to Command(
          command = "elide",
          args = listOf("mcp"),
        )
      )
    )

    private val json by lazy {
      Json {
        prettyPrint = true
      }
    }

    @JvmStatic public fun createForElideJson(): String = json.encodeToString(
      serializer(),
      createForElide()
    )
  }

  /**
   * ### MCP Server Type
   *
   * Defines the root of the type hierarchy for MCP server definitions.
   */
  @Serializable public sealed interface McpServerType

  /**
   * ## MCP Server Command
   *
   * Describes a command-line-style MCP server.
   *
   * @property command The command to execute.
   * @property args Optional arguments to pass to the command.
   * @property env Optional environment variables to set for the command.
   */
  @Serializable public data class Command(
    public val command: String,
    public val args: List<String> = emptyList(),
    public val env: Map<String, String> = emptyMap(),
  ) : McpServerType
}
