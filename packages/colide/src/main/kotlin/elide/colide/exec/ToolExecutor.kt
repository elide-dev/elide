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

package elide.colide.exec

import elide.colide.ColideNative
import elide.colide.fs.FileSystem

/**
 * # Tool Executor
 *
 * Executes embedded tools from /zip/bin/ within Colide OS.
 * Supports both native execution (bare metal) and JVM-based execution (hosted).
 *
 * ## Embedded Tools
 * Tools in /zip/bin/ are statically linked executables that can run on bare metal:
 * - llamafile - AI inference
 * - busybox - Unix utilities
 * - Custom tools - Colide-specific utilities
 *
 * ## Architecture
 * ```
 * Shell Command → ToolExecutor → /zip/bin/tool → Native Execution
 *                                     ↓
 *                              Output Capture → Shell
 * ```
 */
public object ToolExecutor {
    
    private const val ZIP_BIN = "/zip/bin"
    private val toolCache = mutableMapOf<String, ToolInfo>()
    
    /**
     * Information about an embedded tool.
     */
    public data class ToolInfo(
        val name: String,
        val path: String,
        val size: Long,
        val description: String = "",
        val available: Boolean = true
    )
    
    /**
     * Result of tool execution.
     */
    public sealed class ExecutionResult {
        public data class Success(
            val output: String,
            val exitCode: Int = 0
        ) : ExecutionResult()
        
        public data class Error(
            val message: String,
            val exitCode: Int = 1
        ) : ExecutionResult()
        
        public data class NotFound(
            val tool: String
        ) : ExecutionResult()
    }
    
    init {
        scanTools()
    }
    
    /**
     * Scan /zip/bin for available tools.
     */
    private fun scanTools() {
        toolCache.clear()
        
        if (!FileSystem.exists(ZIP_BIN)) {
            return
        }
        
        val entries = FileSystem.listDirectory(ZIP_BIN)
        for (entry in entries) {
            if (!entry.isDirectory && entry.executable) {
                toolCache[entry.name] = ToolInfo(
                    name = entry.name,
                    path = entry.path,
                    size = entry.size,
                    description = getToolDescription(entry.name)
                )
            }
        }
    }
    
    /**
     * Get description for known tools.
     */
    private fun getToolDescription(name: String): String {
        return when (name.lowercase()) {
            "llamafile", "llama" -> "AI inference engine"
            "busybox" -> "Unix utilities collection"
            "sh", "bash" -> "Shell interpreter"
            "cat" -> "Concatenate and print files"
            "ls" -> "List directory contents"
            "grep" -> "Search text patterns"
            "sed" -> "Stream editor"
            "awk" -> "Pattern scanning"
            "vi", "vim" -> "Text editor"
            else -> ""
        }
    }
    
    /**
     * List all available tools.
     */
    public fun listTools(): List<ToolInfo> {
        if (toolCache.isEmpty()) scanTools()
        return toolCache.values.toList().sortedBy { it.name }
    }
    
    /**
     * Check if a tool exists.
     */
    public fun exists(name: String): Boolean {
        if (toolCache.isEmpty()) scanTools()
        return toolCache.containsKey(name) || toolCache.containsKey(name.lowercase())
    }
    
    /**
     * Get tool info.
     */
    public fun getInfo(name: String): ToolInfo? {
        if (toolCache.isEmpty()) scanTools()
        return toolCache[name] ?: toolCache[name.lowercase()]
    }
    
    /**
     * Execute a tool with arguments.
     */
    public fun execute(name: String, args: List<String> = emptyList()): ExecutionResult {
        val tool = getInfo(name)
            ?: return ExecutionResult.NotFound(name)
        
        return if (ColideNative.isAvailable && ColideNative.isMetal()) {
            executeNative(tool, args)
        } else {
            executeHosted(tool, args)
        }
    }
    
    /**
     * Execute tool on bare metal via native driver.
     */
    private fun executeNative(tool: ToolInfo, args: List<String>): ExecutionResult {
        return try {
            val output = nativeExecute(tool.path, args.toTypedArray())
            val exitCode = nativeGetExitCode()
            
            if (exitCode == 0) {
                ExecutionResult.Success(output ?: "", exitCode)
            } else {
                ExecutionResult.Error(output ?: "Execution failed", exitCode)
            }
        } catch (e: Exception) {
            ExecutionResult.Error("Native execution failed: ${e.message}", -1)
        }
    }
    
    /**
     * Execute tool in hosted mode via ProcessBuilder.
     */
    private fun executeHosted(tool: ToolInfo, args: List<String>): ExecutionResult {
        return try {
            val cmdLine = listOf(tool.path) + args
            val process = ProcessBuilder(cmdLine)
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                ExecutionResult.Success(output, exitCode)
            } else {
                ExecutionResult.Error(output.ifEmpty { "Exit code: $exitCode" }, exitCode)
            }
        } catch (e: Exception) {
            ExecutionResult.Error("Execution failed: ${e.message}", -1)
        }
    }
    
    /**
     * Execute a shell command (may use busybox or native shell).
     */
    public fun shell(command: String): ExecutionResult {
        val shell = getInfo("sh") ?: getInfo("busybox")
        
        return if (shell != null) {
            if (shell.name == "busybox") {
                execute("busybox", listOf("sh", "-c", command))
            } else {
                execute("sh", listOf("-c", command))
            }
        } else {
            ExecutionResult.Error("No shell available", -1)
        }
    }
    
    /**
     * Run llamafile for AI inference.
     */
    public fun runLlamafile(prompt: String, maxTokens: Int = 256): ExecutionResult {
        val llama = getInfo("llamafile") ?: getInfo("llama")
            ?: return ExecutionResult.NotFound("llamafile")
        
        return execute(llama.name, listOf(
            "--prompt", prompt,
            "-n", maxTokens.toString(),
            "--temp", "0.7"
        ))
    }
    
    /**
     * Get the /zip/bin path.
     */
    public fun getBinPath(): String = ZIP_BIN
    
    /**
     * Refresh tool cache.
     */
    public fun refresh() {
        scanTools()
    }
    
    @JvmStatic
    private external fun nativeExecute(path: String, args: Array<String>): String?
    
    @JvmStatic
    private external fun nativeGetExitCode(): Int
}
