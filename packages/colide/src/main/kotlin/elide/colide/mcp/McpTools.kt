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

import elide.colide.fs.FileSystem

/**
 * # MCP Tools
 *
 * Local MCP tool implementations for Colide OS. These tools can be used by
 * the AI assistant to interact with the system.
 *
 * ## Available Tools
 * - read_file: Read file contents
 * - write_file: Write file contents
 * - list_directory: List directory contents
 * - run_command: Execute shell command
 * - search_files: Search for files by pattern
 */
public object McpTools {
    
    private val tools = mutableMapOf<String, ToolHandler>()
    
    /**
     * Tool handler interface.
     */
    public fun interface ToolHandler {
        public fun execute(args: Map<String, Any>): McpClient.ToolResult
    }
    
    init {
        registerBuiltins()
    }
    
    private fun registerBuiltins() {
        register("read_file", "Read the contents of a file") { args ->
            val path = args["path"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'path' parameter")
            
            val content = FileSystem.readText(path)
                ?: return@register McpClient.ToolResult.Error("Could not read file: $path")
            
            McpClient.ToolResult.Success(listOf(
                McpClient.ContentBlock(type = "text", text = content)
            ))
        }
        
        register("write_file", "Write content to a file") { args ->
            val path = args["path"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'path' parameter")
            val content = args["content"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'content' parameter")
            
            if (FileSystem.isEmbedded(path)) {
                return@register McpClient.ToolResult.Error("Cannot write to embedded /zip/ filesystem")
            }
            
            val success = FileSystem.writeText(path, content)
            if (success) {
                McpClient.ToolResult.Success(listOf(
                    McpClient.ContentBlock(type = "text", text = "Wrote ${content.length} bytes to $path")
                ))
            } else {
                McpClient.ToolResult.Error("Failed to write file: $path")
            }
        }
        
        register("list_directory", "List contents of a directory") { args ->
            val path = args["path"] as? String ?: "."
            
            val entries = FileSystem.listDirectory(path)
            if (entries.isEmpty()) {
                return@register McpClient.ToolResult.Error("Directory empty or not found: $path")
            }
            
            val text = entries.joinToString("\n") { entry ->
                val prefix = if (entry.isDirectory) "d" else "-"
                val size = if (entry.isDirectory) "<DIR>" else entry.size.toString().padStart(10)
                "$prefix $size ${entry.name}"
            }
            
            McpClient.ToolResult.Success(listOf(
                McpClient.ContentBlock(type = "text", text = text)
            ))
        }
        
        register("run_command", "Execute a shell command") { args ->
            val command = args["command"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'command' parameter")
            val cwd = args["cwd"] as? String
            
            try {
                val pb = ProcessBuilder("sh", "-c", command)
                if (cwd != null) {
                    pb.directory(java.io.File(cwd))
                }
                pb.redirectErrorStream(true)
                
                val process = pb.start()
                val output = process.inputStream.bufferedReader().readText()
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    McpClient.ToolResult.Success(listOf(
                        McpClient.ContentBlock(type = "text", text = output)
                    ))
                } else {
                    McpClient.ToolResult.Error("Command failed with exit code $exitCode:\n$output", exitCode)
                }
            } catch (e: Exception) {
                McpClient.ToolResult.Error("Failed to execute command: ${e.message}")
            }
        }
        
        register("search_files", "Search for files by name pattern") { args ->
            val pattern = args["pattern"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'pattern' parameter")
            val path = args["path"] as? String ?: "."
            
            try {
                val pb = ProcessBuilder("find", path, "-name", pattern, "-type", "f")
                pb.redirectErrorStream(true)
                val process = pb.start()
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                
                McpClient.ToolResult.Success(listOf(
                    McpClient.ContentBlock(type = "text", text = output.ifEmpty { "No files found" })
                ))
            } catch (e: Exception) {
                McpClient.ToolResult.Error("Search failed: ${e.message}")
            }
        }
        
        register("grep_search", "Search for pattern in files") { args ->
            val query = args["query"] as? String
                ?: return@register McpClient.ToolResult.Error("Missing 'query' parameter")
            val path = args["path"] as? String ?: "."
            
            try {
                val pb = ProcessBuilder("grep", "-rn", "--include=*", query, path)
                pb.redirectErrorStream(true)
                val process = pb.start()
                val output = process.inputStream.bufferedReader().use { 
                    it.readText().take(5000)
                }
                process.waitFor()
                
                McpClient.ToolResult.Success(listOf(
                    McpClient.ContentBlock(type = "text", text = output.ifEmpty { "No matches found" })
                ))
            } catch (e: Exception) {
                McpClient.ToolResult.Error("Grep failed: ${e.message}")
            }
        }
        
        register("get_system_info", "Get system information") { _ ->
            val info = buildString {
                appendLine("Colide OS System Information")
                appendLine("━".repeat(40))
                appendLine("Runtime: Elide (GraalVM Polyglot)")
                appendLine("Architecture: ${System.getProperty("os.arch")}")
                appendLine("OS: ${System.getProperty("os.name")}")
                appendLine("Java Version: ${System.getProperty("java.version")}")
                appendLine("Available Processors: ${Runtime.getRuntime().availableProcessors()}")
                appendLine("Max Memory: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
                appendLine("Free Memory: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
            }
            McpClient.ToolResult.Success(listOf(
                McpClient.ContentBlock(type = "text", text = info)
            ))
        }
    }
    
    /**
     * Register a tool.
     */
    public fun register(name: String, description: String, handler: ToolHandler) {
        tools[name] = handler
    }
    
    /**
     * Execute a tool by name.
     */
    public fun execute(name: String, args: Map<String, Any>): McpClient.ToolResult {
        val handler = tools[name]
            ?: return McpClient.ToolResult.Error("Unknown tool: $name")
        return handler.execute(args)
    }
    
    /**
     * Get all registered tools.
     */
    public fun list(): List<McpClient.Tool> {
        return tools.keys.map { name ->
            McpClient.Tool(
                name = name,
                description = getDescription(name),
                inputSchema = getSchema(name)
            )
        }
    }
    
    private fun getDescription(name: String): String {
        return when (name) {
            "read_file" -> "Read the contents of a file"
            "write_file" -> "Write content to a file"
            "list_directory" -> "List contents of a directory"
            "run_command" -> "Execute a shell command"
            "search_files" -> "Search for files by name pattern"
            "grep_search" -> "Search for pattern in files"
            "get_system_info" -> "Get system information"
            else -> "No description"
        }
    }
    
    private fun getSchema(name: String): Map<String, Any> {
        return when (name) {
            "read_file" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "File path to read")
                ),
                "required" to listOf("path")
            )
            "write_file" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "File path to write"),
                    "content" to mapOf("type" to "string", "description" to "Content to write")
                ),
                "required" to listOf("path", "content")
            )
            "list_directory" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "path" to mapOf("type" to "string", "description" to "Directory path")
                )
            )
            "run_command" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "command" to mapOf("type" to "string", "description" to "Command to execute"),
                    "cwd" to mapOf("type" to "string", "description" to "Working directory")
                ),
                "required" to listOf("command")
            )
            "search_files" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "pattern" to mapOf("type" to "string", "description" to "File name pattern (glob)"),
                    "path" to mapOf("type" to "string", "description" to "Search root path")
                ),
                "required" to listOf("pattern")
            )
            "grep_search" -> mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf("type" to "string", "description" to "Search query"),
                    "path" to mapOf("type" to "string", "description" to "Search path")
                ),
                "required" to listOf("query")
            )
            "get_system_info" -> mapOf(
                "type" to "object",
                "properties" to emptyMap<String, Any>()
            )
            else -> emptyMap()
        }
    }
}
