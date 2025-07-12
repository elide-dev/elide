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
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlin.io.path.readText
import elide.tooling.project.manifest.ElidePackageManifest
import elide.tooling.project.mcp.McpContributor

// Contributes declarations to MCP from the project's manifest.
internal class ProjectMcpConfigContributor : McpContributor {
  override suspend fun enabled(context: McpContributor.McpContext): Boolean = context.project() != null

  // Register declared resources with the MCP server.
  private suspend fun McpContributor.McpContext.registerResources(resources: List<ElidePackageManifest.McpResource>) {
    resources.forEach { resource ->
      val absPath = requireNotNull(project()).root.resolve(resource.path).toAbsolutePath()
      val mime = resource.mimeType ?: Files.probeContentType(absPath) ?: "unknown"

      server.addResource(
        name = resource.name,
        description = resource.description,
        uri = absPath.let { "file://$it" },
        mimeType = mime,
      ) { request ->
        ReadResourceResult(
          contents = listOf(
            TextResourceContents(
              uri = request.uri,
              mimeType = mime,
              text = kotlinx.coroutines.withContext(Dispatchers.IO) {
                absPath.readText(StandardCharsets.UTF_8)
              },
            )
          )
        )
      }
    }
  }

  override suspend fun contribute(context: McpContributor.McpContext) = with(requireNotNull(context.project())) {
    manifest.dev?.mcp?.resources?.takeIf { it.isNotEmpty() }?.let { context.registerResources(it) }
    Unit
  }
}
