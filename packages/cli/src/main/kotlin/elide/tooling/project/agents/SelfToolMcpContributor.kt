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

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import org.graalvm.nativeimage.ImageInfo
import java.io.File
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonObject
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import elide.tooling.Environment
import elide.tooling.cli.Statics
import elide.tooling.project.mcp.McpContributor

// Contributes Elide itself as a tool to the MCP.
internal class SelfToolMcpContributor : McpContributor {
  override suspend fun enabled(context: McpContributor.McpContext): Boolean =
    context.project()?.manifest?.dev?.mcp?.registerElide != false

  override suspend fun contribute(context: McpContributor.McpContext) = with(context.server) {
    addTool(
      name = "elide",
      description = buildString {
        append("Polyruntime and application toolchain; see project advice or run `elide help`")
        append(" or `elide project advice`. This tool runs Elide as a CLI subprocess.")
      },
      inputSchema = Tool.Input(
        properties = buildJsonObject {
          putJsonObject("args") {
            put("type", JsonPrimitive("array"))
            put("description", JsonPrimitive("Arguments to pass to Elide"))
            put("items", buildJsonObject {
              put("type", JsonPrimitive("string"))
            })
          }
          putJsonObject("env") {
            put("type", JsonPrimitive("object"))
            put("description", JsonPrimitive("Environment variables to set for Elide"))
            put("additionalProperties", buildJsonObject {
              put("type", JsonPrimitive("string"))
            })
          }
        }
      ),
    ) { request ->
      val args = request.arguments["args"]?.jsonArray?.map {
        it.jsonPrimitive.content
      } ?: emptyList()

      val env = request.arguments["env"]?.jsonObject?.map {
        it.key to it.value.jsonPrimitive.content
      }?.toMap()?.let { Environment.from(it) } ?: Environment.empty()

      val binpath = when (ImageInfo.inImageRuntimeCode()) {
        true -> Statics.binPath
        false -> (System.getenv("PATH") ?: "").split(File.pathSeparator)
          .mapNotNull { it.trim().takeIf { it.isNotEmpty() } }
          .map { java.nio.file.Paths.get(it, "elide") }
          .firstOrNull { it.exists() && it.isExecutable() }
          ?: error("Elide binary not found in PATH")
      }
      when {
        !binpath.exists() -> error("Binary path doesn't exist")
        !binpath.isExecutable() -> error("Elide binary isn't executable")
        else -> {}
      }

      // run and capture output
      @Suppress("SpreadOperator")
      val proc = ProcessBuilder().apply {
        command(binpath.absolutePathString(), *args.toTypedArray())
        env.map { environment().put(it.key, it.value) }
        redirectOutput(ProcessBuilder.Redirect.PIPE)
        redirectError(ProcessBuilder.Redirect.PIPE)
        redirectErrorStream(true) // Redirect stderr to stdout
      }

      // launch the process, capturing the exit code and output. don't error if elide fails.
      runCatching {
        val process = proc.start()
        val output = process.inputStream.bufferedReader().readText()
        val err = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        // return the output and exit code
        (exitCode != 0) to (exitCode to (output to err))
      }.getOrElse { error ->
        // if we fail, just return the error as output
        true to (-1 to (null to error.message))
      }.let { (isError, toolResponse) ->
        val (exitCode, outputPair) = toolResponse
        val (output, err) = outputPair

        CallToolResult(
          isError = isError,
          content = listOf(
            TextContent(text = buildString {
              append("Exit code: $exitCode")
            }),
            TextContent(text = buildString {
              appendLine("stdout:")
              append(output ?: "No output")
            }),
            TextContent(text = buildString {
              appendLine("stderr:")
              append(err ?: "No error output")
            }),
          )
        )
      }
    }
  }
}
