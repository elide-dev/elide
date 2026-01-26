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

package elide.colide.mcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * # MCP Client
 *
 * Model Context Protocol client for Colide OS. Enables AI agents to call tools
 * via JSON-RPC 2.0 over HTTP or stdio.
 *
 * ## Usage
 * ```kotlin
 * val client = McpClient("http://localhost:3000/mcp")
 * val tools = client.listTools()
 * val result = client.callTool("read_file", mapOf("path" to "/etc/hosts"))
 * ```
 *
 * ## Supported Transports
 * - HTTP (for hosted mode with network)
 * - Stdio (for subprocess MCP servers)
 */
public class McpClient(
    private val endpoint: String,
    private val transport: Transport = Transport.HTTP
) {
    private val requestId = AtomicInteger(0)
    private var process: Process? = null
    private var processWriter: OutputStreamWriter? = null
    private var processReader: BufferedReader? = null
    
    public enum class Transport {
        HTTP,
        STDIO
    }
    
    /**
     * Tool definition from MCP server.
     */
    public data class Tool(
        val name: String,
        val description: String,
        val inputSchema: Map<String, Any>
    )
    
    /**
     * Tool call result.
     */
    public sealed class ToolResult {
        public data class Success(val content: List<ContentBlock>) : ToolResult()
        public data class Error(val message: String, val code: Int = -1) : ToolResult()
    }
    
    /**
     * Content block in tool result.
     */
    public data class ContentBlock(
        val type: String,
        val text: String? = null,
        val data: String? = null,
        val mimeType: String? = null
    )
    
    /**
     * Initialize the MCP connection.
     */
    public fun initialize(): Boolean {
        return when (transport) {
            Transport.HTTP -> initializeHttp()
            Transport.STDIO -> initializeStdio()
        }
    }
    
    private fun initializeHttp(): Boolean {
        return try {
            val response = sendRequest("initialize", mapOf(
                "protocolVersion" to "2024-11-05",
                "capabilities" to mapOf<String, Any>(),
                "clientInfo" to mapOf(
                    "name" to "colide-mcp-client",
                    "version" to "1.0.0"
                )
            ))
            response != null && !response.containsKey("error")
        } catch (e: Exception) {
            System.err.println("MCP init failed: ${e.message}")
            false
        }
    }
    
    private fun initializeStdio(): Boolean {
        return try {
            val pb = ProcessBuilder(endpoint.split(" "))
            pb.redirectErrorStream(false)
            process = pb.start()
            processWriter = OutputStreamWriter(process!!.outputStream)
            processReader = BufferedReader(InputStreamReader(process!!.inputStream))
            
            val response = sendStdioRequest("initialize", mapOf(
                "protocolVersion" to "2024-11-05",
                "capabilities" to mapOf<String, Any>(),
                "clientInfo" to mapOf(
                    "name" to "colide-mcp-client",
                    "version" to "1.0.0"
                )
            ))
            response != null && !response.containsKey("error")
        } catch (e: Exception) {
            System.err.println("MCP stdio init failed: ${e.message}")
            false
        }
    }
    
    /**
     * List available tools from MCP server.
     */
    public fun listTools(): List<Tool> {
        val response = when (transport) {
            Transport.HTTP -> sendRequest("tools/list", emptyMap())
            Transport.STDIO -> sendStdioRequest("tools/list", emptyMap())
        } ?: return emptyList()
        
        @Suppress("UNCHECKED_CAST")
        val result = response["result"] as? Map<String, Any> ?: return emptyList()
        val tools = result["tools"] as? List<Map<String, Any>> ?: return emptyList()
        
        return tools.mapNotNull { tool ->
            val name = tool["name"] as? String ?: return@mapNotNull null
            val desc = tool["description"] as? String ?: ""
            @Suppress("UNCHECKED_CAST")
            val schema = tool["inputSchema"] as? Map<String, Any> ?: emptyMap()
            Tool(name, desc, schema)
        }
    }
    
    /**
     * Call a tool on the MCP server.
     */
    public fun callTool(name: String, arguments: Map<String, Any>): ToolResult {
        val response = when (transport) {
            Transport.HTTP -> sendRequest("tools/call", mapOf(
                "name" to name,
                "arguments" to arguments
            ))
            Transport.STDIO -> sendStdioRequest("tools/call", mapOf(
                "name" to name,
                "arguments" to arguments
            ))
        }
        
        if (response == null) {
            return ToolResult.Error("No response from MCP server")
        }
        
        val error = response["error"] as? Map<String, Any>
        if (error != null) {
            val message = error["message"] as? String ?: "Unknown error"
            val code = (error["code"] as? Number)?.toInt() ?: -1
            return ToolResult.Error(message, code)
        }
        
        @Suppress("UNCHECKED_CAST")
        val result = response["result"] as? Map<String, Any>
        if (result == null) {
            return ToolResult.Error("Invalid response format")
        }
        
        @Suppress("UNCHECKED_CAST")
        val content = result["content"] as? List<Map<String, Any>> ?: emptyList()
        val blocks = content.map { block ->
            ContentBlock(
                type = block["type"] as? String ?: "text",
                text = block["text"] as? String,
                data = block["data"] as? String,
                mimeType = block["mimeType"] as? String
            )
        }
        
        return ToolResult.Success(blocks)
    }
    
    /**
     * Send JSON-RPC request over HTTP.
     */
    private fun sendRequest(method: String, params: Map<String, Any>): Map<String, Any>? {
        return try {
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 30000
            
            val request = buildJsonRpcRequest(method, params)
            conn.outputStream.use { it.write(request.toByteArray()) }
            
            val responseCode = conn.responseCode
            if (responseCode != 200) {
                return mapOf("error" to mapOf("message" to "HTTP $responseCode", "code" to responseCode))
            }
            
            val response = conn.inputStream.bufferedReader().readText()
            parseJsonResponse(response)
        } catch (e: Exception) {
            mapOf("error" to mapOf("message" to e.message, "code" to -1))
        }
    }
    
    /**
     * Send JSON-RPC request over stdio.
     */
    private fun sendStdioRequest(method: String, params: Map<String, Any>): Map<String, Any>? {
        val writer = processWriter ?: return null
        val reader = processReader ?: return null
        
        return try {
            val request = buildJsonRpcRequest(method, params)
            writer.write(request)
            writer.write("\n")
            writer.flush()
            
            val response = reader.readLine() ?: return null
            parseJsonResponse(response)
        } catch (e: Exception) {
            mapOf("error" to mapOf("message" to e.message, "code" to -1))
        }
    }
    
    /**
     * Build JSON-RPC 2.0 request.
     */
    private fun buildJsonRpcRequest(method: String, params: Map<String, Any>): String {
        val id = requestId.incrementAndGet()
        val paramsJson = toJson(params)
        return """{"jsonrpc":"2.0","id":$id,"method":"$method","params":$paramsJson}"""
    }
    
    /**
     * Parse JSON response (simple parser).
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseJsonResponse(json: String): Map<String, Any>? {
        return try {
            parseJsonObject(json.trim())
        } catch (_: Exception) {
            null
        }
    }
    
    /**
     * Simple JSON object parser.
     */
    @Suppress("UNCHECKED_CAST")
    private fun parseJsonObject(json: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        if (!json.startsWith("{") || !json.endsWith("}")) return result
        
        var content = json.substring(1, json.length - 1).trim()
        while (content.isNotEmpty()) {
            val keyEnd = content.indexOf("\"", 1)
            if (keyEnd < 0) break
            val key = content.substring(1, keyEnd)
            content = content.substring(keyEnd + 1).trimStart()
            if (!content.startsWith(":")) break
            content = content.substring(1).trimStart()
            
            val (value, remaining) = parseJsonValue(content)
            result[key] = value
            content = remaining.trimStart()
            if (content.startsWith(",")) {
                content = content.substring(1).trimStart()
            }
        }
        return result
    }
    
    private fun parseJsonValue(json: String): Pair<Any, String> {
        return when {
            json.startsWith("\"") -> {
                val end = findStringEnd(json)
                val value = json.substring(1, end).replace("\\\"", "\"").replace("\\n", "\n")
                value to json.substring(end + 1)
            }
            json.startsWith("{") -> {
                val end = findObjectEnd(json)
                parseJsonObject(json.substring(0, end + 1)) to json.substring(end + 1)
            }
            json.startsWith("[") -> {
                val end = findArrayEnd(json)
                parseJsonArray(json.substring(0, end + 1)) to json.substring(end + 1)
            }
            json.startsWith("true") -> true to json.substring(4)
            json.startsWith("false") -> false to json.substring(5)
            json.startsWith("null") -> "" to json.substring(4)
            else -> {
                val end = json.indexOfFirst { it == ',' || it == '}' || it == ']' }
                val numStr = if (end < 0) json else json.substring(0, end)
                val num = numStr.trim().toDoubleOrNull() ?: 0.0
                num to (if (end < 0) "" else json.substring(end))
            }
        }
    }
    
    private fun parseJsonArray(json: String): List<Any> {
        val result = mutableListOf<Any>()
        var content = json.substring(1, json.length - 1).trim()
        while (content.isNotEmpty()) {
            val (value, remaining) = parseJsonValue(content)
            result.add(value)
            content = remaining.trimStart()
            if (content.startsWith(",")) {
                content = content.substring(1).trimStart()
            }
        }
        return result
    }
    
    private fun findStringEnd(json: String): Int {
        var i = 1
        while (i < json.length) {
            if (json[i] == '"' && json[i - 1] != '\\') return i
            i++
        }
        return json.length - 1
    }
    
    private fun findObjectEnd(json: String): Int {
        var depth = 0
        var inString = false
        for (i in json.indices) {
            when {
                json[i] == '"' && (i == 0 || json[i - 1] != '\\') -> inString = !inString
                !inString && json[i] == '{' -> depth++
                !inString && json[i] == '}' -> { depth--; if (depth == 0) return i }
            }
        }
        return json.length - 1
    }
    
    private fun findArrayEnd(json: String): Int {
        var depth = 0
        var inString = false
        for (i in json.indices) {
            when {
                json[i] == '"' && (i == 0 || json[i - 1] != '\\') -> inString = !inString
                !inString && json[i] == '[' -> depth++
                !inString && json[i] == ']' -> { depth--; if (depth == 0) return i }
            }
        }
        return json.length - 1
    }
    
    /**
     * Convert value to JSON string.
     */
    private fun toJson(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "\"${value.replace("\"", "\\\"").replace("\n", "\\n")}\""
            is Number -> value.toString()
            is Boolean -> value.toString()
            is Map<*, *> -> {
                val entries = value.entries.joinToString(",") { (k, v) ->
                    "\"$k\":${toJson(v)}"
                }
                "{$entries}"
            }
            is List<*> -> {
                val items = value.joinToString(",") { toJson(it) }
                "[$items]"
            }
            else -> "\"$value\""
        }
    }
    
    /**
     * Close the MCP connection.
     */
    public fun close() {
        processWriter?.close()
        processReader?.close()
        process?.destroy()
    }
    
    public companion object {
        /**
         * Create an HTTP MCP client.
         */
        @JvmStatic
        public fun http(endpoint: String): McpClient {
            return McpClient(endpoint, Transport.HTTP)
        }
        
        /**
         * Create a stdio MCP client (subprocess).
         */
        @JvmStatic
        public fun stdio(command: String): McpClient {
            return McpClient(command, Transport.STDIO)
        }
    }
}
