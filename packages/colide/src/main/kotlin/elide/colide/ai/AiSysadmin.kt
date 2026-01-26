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

package elide.colide.ai

import elide.colide.Ai
import elide.colide.mcp.McpClient
import elide.colide.mcp.McpTools

/**
 * # AI Sysadmin
 *
 * Autonomous AI system administrator for Colide OS.
 * Uses MCP tools to perform system tasks based on natural language instructions.
 *
 * ## Capabilities
 * - File operations (read, write, search)
 * - System diagnostics
 * - Configuration management
 * - Automated troubleshooting
 */
public class AiSysadmin {
    
    private val conversationHistory = mutableListOf<Message>()
    private var maxIterations = 10
    
    public data class Message(
        val role: String,
        val content: String
    )
    
    public data class TaskResult(
        val success: Boolean,
        val summary: String,
        val steps: List<String>,
        val error: String? = null
    )
    
    /**
     * Execute a task described in natural language.
     */
    public fun executeTask(instruction: String): TaskResult {
        val steps = mutableListOf<String>()
        conversationHistory.clear()
        
        conversationHistory.add(Message("system", SYSTEM_PROMPT))
        conversationHistory.add(Message("user", instruction))
        
        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            
            val prompt = buildPrompt()
            val response = Ai.complete(prompt)
            
            if (response.isEmpty()) {
                return TaskResult(
                    success = false,
                    summary = "AI not available",
                    steps = steps,
                    error = "Could not get response from AI"
                )
            }
            
            conversationHistory.add(Message("assistant", response))
            
            val toolCall = parseToolCall(response)
            if (toolCall != null) {
                steps.add("Tool: ${toolCall.first}(${toolCall.second})")
                val result = executeTool(toolCall.first, toolCall.second)
                conversationHistory.add(Message("tool", result))
                steps.add("Result: ${result.take(100)}...")
            }
            
            if (response.contains("TASK_COMPLETE") || response.contains("Task complete")) {
                val summary = extractSummary(response)
                return TaskResult(
                    success = true,
                    summary = summary,
                    steps = steps
                )
            }
            
            if (response.contains("TASK_FAILED") || response.contains("cannot complete")) {
                val error = extractError(response)
                return TaskResult(
                    success = false,
                    summary = "Task failed",
                    steps = steps,
                    error = error
                )
            }
        }
        
        return TaskResult(
            success = false,
            summary = "Max iterations reached",
            steps = steps,
            error = "Task did not complete within $maxIterations iterations"
        )
    }
    
    /**
     * Ask for system information or diagnostics.
     */
    public fun diagnose(query: String): String {
        val prompt = """
            |You are a system administrator for Colide OS.
            |The user asks: $query
            |
            |Available tools: ${McpTools.list().joinToString { it.name }}
            |
            |Provide a diagnostic response. If you need to use a tool, format as:
            |TOOL: <tool_name>
            |ARGS: key1=value1, key2=value2
        """.trimMargin()
        
        return Ai.complete(prompt).ifEmpty { "Diagnostics unavailable" }
    }
    
    /**
     * Get system health status.
     */
    public fun healthCheck(): Map<String, String> {
        val status = mutableMapOf<String, String>()
        
        status["memory"] = try {
            val runtime = Runtime.getRuntime()
            val used = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val max = runtime.maxMemory() / 1024 / 1024
            "OK (${used}MB / ${max}MB)"
        } catch (_: Exception) {
            "UNKNOWN"
        }
        
        status["cpu"] = try {
            val processors = Runtime.getRuntime().availableProcessors()
            "OK ($processors cores)"
        } catch (_: Exception) {
            "UNKNOWN"
        }
        
        status["ai"] = try {
            if (Ai.complete("test").isNotEmpty()) "AVAILABLE" else "UNAVAILABLE"
        } catch (_: Exception) {
            "UNAVAILABLE"
        }
        
        status["tools"] = try {
            val count = McpTools.list().size
            "OK ($count tools)"
        } catch (_: Exception) {
            "ERROR"
        }
        
        return status
    }
    
    private fun buildPrompt(): String {
        val sb = StringBuilder()
        for (msg in conversationHistory) {
            when (msg.role) {
                "system" -> sb.appendLine("System: ${msg.content}")
                "user" -> sb.appendLine("User: ${msg.content}")
                "assistant" -> sb.appendLine("Assistant: ${msg.content}")
                "tool" -> sb.appendLine("Tool Result: ${msg.content}")
            }
        }
        sb.appendLine("Assistant:")
        return sb.toString()
    }
    
    private fun parseToolCall(response: String): Pair<String, Map<String, Any>>? {
        val toolMatch = Regex("TOOL:\\s*(\\w+)").find(response)
        val argsMatch = Regex("ARGS:\\s*(.+)").find(response)
        
        if (toolMatch == null) return null
        
        val toolName = toolMatch.groupValues[1]
        val args = mutableMapOf<String, Any>()
        
        if (argsMatch != null) {
            val argsStr = argsMatch.groupValues[1]
            for (pair in argsStr.split(",")) {
                val parts = pair.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    args[parts[0].trim()] = parts[1].trim()
                }
            }
        }
        
        return toolName to args
    }
    
    private fun executeTool(name: String, args: Map<String, Any>): String {
        return when (val result = McpTools.execute(name, args)) {
            is McpClient.ToolResult.Success -> {
                result.content.mapNotNull { it.text }.joinToString("\n")
            }
            is McpClient.ToolResult.Error -> {
                "Error: ${result.message}"
            }
        }
    }
    
    private fun extractSummary(response: String): String {
        val summaryMatch = Regex("SUMMARY:\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(response)
        return summaryMatch?.groupValues?.get(1)?.trim()?.take(200) ?: "Task completed successfully"
    }
    
    private fun extractError(response: String): String {
        val errorMatch = Regex("ERROR:\\s*(.+)", RegexOption.DOT_MATCHES_ALL).find(response)
        return errorMatch?.groupValues?.get(1)?.trim()?.take(200) ?: "Unknown error"
    }
    
    public companion object {
        private val SYSTEM_PROMPT = """
            |You are an AI system administrator for Colide OS, a unikernel operating system.
            |You have access to the following tools:
            |
            |- read_file: Read file contents (args: path)
            |- write_file: Write to file (args: path, content)
            |- list_directory: List directory (args: path)
            |- run_command: Execute command (args: command, cwd)
            |- search_files: Search for files (args: pattern, path)
            |- grep_search: Search in files (args: query, path)
            |- get_system_info: Get system information
            |
            |When you need to use a tool, respond with:
            |TOOL: <tool_name>
            |ARGS: key1=value1, key2=value2
            |
            |After completing the task, respond with:
            |TASK_COMPLETE
            |SUMMARY: <what was done>
            |
            |If you cannot complete the task, respond with:
            |TASK_FAILED
            |ERROR: <reason>
            |
            |Be concise and efficient. Use minimal tool calls.
        """.trimMargin()
        
        private var instance: AiSysadmin? = null
        
        @JvmStatic
        public fun getInstance(): AiSysadmin {
            if (instance == null) {
                instance = AiSysadmin()
            }
            return instance!!
        }
    }
}
