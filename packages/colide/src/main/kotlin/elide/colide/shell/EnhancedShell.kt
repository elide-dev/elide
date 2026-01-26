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

package elide.colide.shell

import elide.colide.ColideNative
import elide.colide.Keyboard
import elide.colide.fs.FileSystem
import elide.colide.mcp.McpClient
import elide.colide.mcp.McpTools
import elide.colide.tui.TuiRenderer

/**
 * # Enhanced Shell
 *
 * Full-featured shell for Colide OS with:
 * - Command history and editing
 * - Tab completion
 * - MCP tool integration
 * - AI assistant
 * - TUI rendering
 */
public class EnhancedShell {
    private val renderer = TuiRenderer(80, 25)
    private val history = mutableListOf<String>()
    private var historyIndex = -1
    private var running = true
    private var currentDir = "/zip"
    
    private val env = mutableMapOf(
        "HOME" to "/zip",
        "PATH" to "/zip/bin",
        "SHELL" to "colide",
        "USER" to "colide",
        "PWD" to "/zip"
    )
    
    private val aiAssistant = AiAssistant.getInstance()
    private var mcpClient: McpClient? = null
    
    private val isMetal = ColideNative.isAvailable && ColideNative.isMetal()
    
    private var outputLines = mutableListOf<OutputLine>()
    private var maxOutputLines = 20
    private var inputBuffer = StringBuilder()
    private var cursorPos = 0
    
    private data class OutputLine(
        val text: String,
        val color: TuiRenderer.Color = TuiRenderer.Color.DEFAULT
    )
    
    init {
        registerExtendedCommands()
    }
    
    private fun registerExtendedCommands() {
        CommandRegistry.register(CommandRegistry.Command(
            name = "cd",
            description = "Change directory",
            usage = "cd <path>",
            category = CommandRegistry.Category.FILE
        ) { ctx ->
            val path = ctx.args.firstOrNull() ?: env["HOME"] ?: "/zip"
            val newPath = resolvePath(path)
            if (FileSystem.isDirectory(newPath)) {
                currentDir = newPath
                env["PWD"] = currentDir
                CommandRegistry.CommandResult.Silent
            } else {
                CommandRegistry.CommandResult.Error("Not a directory: $newPath")
            }
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "history",
            description = "Show command history",
            usage = "history [n]",
            category = CommandRegistry.Category.SYSTEM
        ) { ctx ->
            val n = ctx.args.firstOrNull()?.toIntOrNull() ?: history.size
            val lines = history.takeLast(n).mapIndexed { i, cmd ->
                "${(history.size - n + i + 1).toString().padStart(4)} $cmd"
            }
            CommandRegistry.CommandResult.Output(lines)
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "mcp",
            description = "MCP tool operations",
            usage = "mcp [list|call <tool> <args>|connect <url>]",
            category = CommandRegistry.Category.DEV
        ) { ctx ->
            when (ctx.args.firstOrNull()) {
                "list" -> {
                    val tools = McpTools.list()
                    val lines = tools.map { "  ${it.name.padEnd(16)} ${it.description}" }
                    CommandRegistry.CommandResult.Output(listOf("Available MCP tools:") + lines)
                }
                "call" -> {
                    if (ctx.args.size < 2) {
                        return@Command CommandRegistry.CommandResult.Error("Usage: mcp call <tool> [args...]")
                    }
                    val toolName = ctx.args[1]
                    val toolArgs = if (ctx.args.size > 2) parseArgs(ctx.args.drop(2)) else emptyMap()
                    when (val result = McpTools.execute(toolName, toolArgs)) {
                        is McpClient.ToolResult.Success -> {
                            val text = result.content.mapNotNull { it.text }.joinToString("\n")
                            CommandRegistry.CommandResult.Output(text.lines())
                        }
                        is McpClient.ToolResult.Error -> {
                            CommandRegistry.CommandResult.Error(result.message)
                        }
                    }
                }
                "connect" -> {
                    val url = ctx.args.getOrNull(1)
                        ?: return@Command CommandRegistry.CommandResult.Error("Usage: mcp connect <url>")
                    mcpClient = McpClient.http(url)
                    if (mcpClient?.initialize() == true) {
                        CommandRegistry.CommandResult.Success("Connected to MCP server at $url")
                    } else {
                        mcpClient = null
                        CommandRegistry.CommandResult.Error("Failed to connect to MCP server")
                    }
                }
                else -> CommandRegistry.CommandResult.Output(listOf(
                    "MCP Tool Commands:",
                    "  mcp list          - List local tools",
                    "  mcp call <tool>   - Call a local tool",
                    "  mcp connect <url> - Connect to remote MCP server"
                ))
            }
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "run",
            description = "Run an application",
            usage = "run <app> [args...]",
            category = CommandRegistry.Category.SYSTEM
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                return@Command CommandRegistry.CommandResult.Error("Usage: run <app> [args...]")
            }
            val app = ctx.args[0]
            CommandRegistry.CommandResult.Success("__RUN:$app:${ctx.args.drop(1).joinToString(":")}__")
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "export",
            description = "Set environment variable",
            usage = "export NAME=value",
            category = CommandRegistry.Category.SYSTEM
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                val lines = env.entries.map { "${it.key}=${it.value}" }.sorted()
                return@Command CommandRegistry.CommandResult.Output(lines)
            }
            for (arg in ctx.args) {
                val parts = arg.split("=", limit = 2)
                if (parts.size == 2) {
                    env[parts[0]] = parts[1]
                }
            }
            CommandRegistry.CommandResult.Silent
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "which",
            description = "Show command type",
            usage = "which <command>",
            category = CommandRegistry.Category.SYSTEM
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                return@Command CommandRegistry.CommandResult.Error("Usage: which <command>")
            }
            val cmd = ctx.args[0]
            val registered = CommandRegistry.get(cmd)
            if (registered != null) {
                CommandRegistry.CommandResult.Success("$cmd: shell builtin (${registered.category.displayName})")
            } else {
                CommandRegistry.CommandResult.Error("$cmd: not found")
            }
        })
        
        CommandRegistry.register(CommandRegistry.Command(
            name = "tool",
            description = "Execute MCP tool directly",
            usage = "tool <name> [key=value...]",
            category = CommandRegistry.Category.DEV
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                return@Command CommandRegistry.CommandResult.Error("Usage: tool <name> [key=value...]")
            }
            val toolName = ctx.args[0]
            val toolArgs = parseArgs(ctx.args.drop(1))
            when (val result = McpTools.execute(toolName, toolArgs)) {
                is McpClient.ToolResult.Success -> {
                    val text = result.content.mapNotNull { it.text }.joinToString("\n")
                    CommandRegistry.CommandResult.Output(text.lines())
                }
                is McpClient.ToolResult.Error -> {
                    CommandRegistry.CommandResult.Error(result.message)
                }
            }
        })
        
        CommandRegistry.alias("ll", "ls")
        CommandRegistry.alias("la", "ls")
    }
    
    private fun parseArgs(args: List<String>): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        for (arg in args) {
            val parts = arg.split("=", limit = 2)
            if (parts.size == 2) {
                result[parts[0]] = parts[1]
            } else {
                result["_${result.size}"] = arg
            }
        }
        return result
    }
    
    private fun resolvePath(path: String): String {
        return when {
            path.startsWith("/") -> path
            path == "~" -> env["HOME"] ?: "/zip"
            path.startsWith("~/") -> (env["HOME"] ?: "/zip") + path.drop(1)
            path == ".." -> {
                val parent = java.io.File(currentDir).parent
                parent ?: currentDir
            }
            path == "." -> currentDir
            else -> "$currentDir/$path"
        }
    }
    
    /**
     * Start the enhanced shell.
     */
    public fun start() {
        printBanner()
        prompt()
        
        while (running) {
            val key = readKey()
            if (key == 0) continue
            
            when {
                key == '\n'.code || key == '\r'.code -> handleEnter()
                key == 0x7F || key == 0x08 -> handleBackspace()
                key == '\t'.code -> handleTab()
                key == 0x1B -> handleEscape()
                key in 0x20..0x7E -> handleChar(key.toChar())
            }
            
            render()
        }
    }
    
    private fun readKey(): Int {
        return if (isMetal) {
            if (Keyboard.available()) Keyboard.getChar() else 0
        } else {
            try {
                if (System.`in`.available() > 0) {
                    System.`in`.read()
                } else {
                    Thread.sleep(16)
                    0
                }
            } catch (_: Exception) {
                0
            }
        }
    }
    
    private fun handleEnter() {
        val line = inputBuffer.toString().trim()
        outputLines.add(OutputLine("colide> $line", TuiRenderer.Color.PROMPT))
        
        if (line.isNotEmpty()) {
            history.add(line)
            historyIndex = history.size
            executeCommand(line)
        }
        
        inputBuffer.clear()
        cursorPos = 0
        prompt()
    }
    
    private fun handleBackspace() {
        if (cursorPos > 0) {
            inputBuffer.deleteAt(cursorPos - 1)
            cursorPos--
        }
    }
    
    private fun handleTab() {
        val partial = inputBuffer.toString()
        val completions = CommandRegistry.completions(partial)
        when {
            completions.isEmpty() -> {}
            completions.size == 1 -> {
                inputBuffer.clear()
                inputBuffer.append(completions[0])
                cursorPos = inputBuffer.length
            }
            else -> {
                outputLines.add(OutputLine(completions.joinToString("  "), TuiRenderer.Color.INFO))
            }
        }
    }
    
    private fun handleEscape() {
        val seq1 = readKey()
        if (seq1 == '['.code) {
            val seq2 = readKey()
            when (seq2) {
                'A'.code -> historyUp()
                'B'.code -> historyDown()
                'C'.code -> if (cursorPos < inputBuffer.length) cursorPos++
                'D'.code -> if (cursorPos > 0) cursorPos--
            }
        }
    }
    
    private fun historyUp() {
        if (historyIndex > 0) {
            historyIndex--
            inputBuffer.clear()
            inputBuffer.append(history[historyIndex])
            cursorPos = inputBuffer.length
        }
    }
    
    private fun historyDown() {
        if (historyIndex < history.size - 1) {
            historyIndex++
            inputBuffer.clear()
            inputBuffer.append(history[historyIndex])
            cursorPos = inputBuffer.length
        } else {
            historyIndex = history.size
            inputBuffer.clear()
            cursorPos = 0
        }
    }
    
    private fun handleChar(ch: Char) {
        inputBuffer.insert(cursorPos, ch)
        cursorPos++
    }
    
    private fun executeCommand(line: String) {
        val parts = line.split(" ", limit = 2)
        val cmd = parts[0]
        val args = if (parts.size > 1) parts[1].split(" ") else emptyList()
        
        val context = CommandRegistry.CommandContext(
            args = args,
            rawInput = line,
            shell = this,
            env = env.toMap()
        )
        
        when (val result = CommandRegistry.execute(cmd, context)) {
            is CommandRegistry.CommandResult.Success -> {
                val output = result.output
                when {
                    output == "__CLEAR__" -> {
                        outputLines.clear()
                    }
                    output.startsWith("__EXIT") -> {
                        running = false
                        printLine("Goodbye!", TuiRenderer.Color.INFO)
                    }
                    output.startsWith("__AI:") -> {
                        val query = output.removePrefix("__AI:")
                        printLine("Thinking...", TuiRenderer.Color.PROMPT)
                        val response = aiAssistant.ask(query)
                        printLine(response)
                    }
                    output.startsWith("__CHAT__") -> {
                        printLine("Starting AI chat mode. Type 'exit' to return.", TuiRenderer.Color.INFO)
                    }
                    output.startsWith("__GUI__") -> {
                        printLine("GUI mode not available in TUI shell", TuiRenderer.Color.WARNING)
                    }
                    output.startsWith("__RUN:") -> {
                        val appInfo = output.removePrefix("__RUN:").split(":")
                        printLine("Running app: ${appInfo[0]}", TuiRenderer.Color.INFO)
                    }
                    output.isNotEmpty() -> printLine(output)
                }
            }
            is CommandRegistry.CommandResult.Error -> {
                printLine("Error: ${result.message}", TuiRenderer.Color.ERROR)
            }
            is CommandRegistry.CommandResult.Output -> {
                for (line in result.lines) {
                    printLine(line)
                }
            }
            is CommandRegistry.CommandResult.Silent -> {}
        }
    }
    
    private fun printBanner() {
        val banner = listOf(
            "╔══════════════════════════════════════════════════════════════╗",
            "║  COLIDE OS - Enhanced Shell                                  ║",
            "║  Elide Runtime • MCP Tools • AI Assistant                    ║",
            "╚══════════════════════════════════════════════════════════════╝",
            "",
            "Type 'help' for commands, 'mcp list' for tools, 'ai <question>' for AI."
        )
        for (line in banner) {
            outputLines.add(OutputLine(line, TuiRenderer.Color.PROMPT))
        }
    }
    
    private fun prompt() {
        // Prompt is rendered inline with input
    }
    
    private fun printLine(text: String, color: TuiRenderer.Color = TuiRenderer.Color.DEFAULT) {
        for (line in text.lines()) {
            outputLines.add(OutputLine(line, color))
        }
        while (outputLines.size > maxOutputLines) {
            outputLines.removeAt(0)
        }
    }
    
    private fun render() {
        renderer.clear()
        
        val startY = 0
        for ((i, line) in outputLines.withIndex()) {
            if (i >= maxOutputLines) break
            renderer.putStringAt(0, startY + i, line.text.take(80), line.color)
        }
        
        val promptY = outputLines.size.coerceAtMost(maxOutputLines)
        renderer.putStringAt(0, promptY, "colide> ", TuiRenderer.Color.PROMPT)
        renderer.putStringAt(8, promptY, inputBuffer.toString(), TuiRenderer.Color.DEFAULT)
        
        val statusY = renderer.getHeight() - 1
        val statusText = " $currentDir | ${history.size} cmds | ${if (aiAssistant.isAvailable()) "AI:ON" else "AI:OFF"} "
        renderer.fillRect(0, statusY, 80, 1, ' ', TuiRenderer.Color.HIGHLIGHT)
        renderer.putStringAt(0, statusY, statusText, TuiRenderer.Color.HIGHLIGHT)
        
        renderer.render()
    }
    
    public companion object {
        @JvmStatic
        public fun main(args: Array<String>) {
            val shell = EnhancedShell()
            shell.start()
        }
    }
}
