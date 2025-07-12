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
package elide.tooling.project.agents

import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.TextResourceContents
import java.nio.charset.StandardCharsets
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import elide.tooling.project.mcp.McpContributor

// Bridges project advice to MCP.
internal class AdviceMcpContributor : McpContributor {
  override suspend fun contribute(context: McpContributor.McpContext): Unit = with(context.server) {
    when (val project = context.project()) {
      null -> {}

      else -> listOf(
        project.root.resolve(".dev").resolve("AGENT.md"),
        project.root.resolve(".dev").resolve("CLAUDE.md"),
        project.root.resolve("CLAUDE.md"),
      ).firstOrNull { it.exists() }?.let { advice ->
        advice.inputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
          advice to reader.readText()
        }
      }?.let { (path, adviceContent) ->
        val uri = "file://${path.absolutePathString()}"
        addResource(
          uri = uri,
          name = "Project Advice",
          description = "AI agents should load this file to learn more about this project.",
          mimeType = "text/markdown",
        ) { request ->
          ReadResourceResult(
            listOf(
              TextResourceContents(
                text = adviceContent,
                mimeType = "text/markdown",
                uri = uri,
              )
            ),
          )
        }
      }
    }
  }
}
