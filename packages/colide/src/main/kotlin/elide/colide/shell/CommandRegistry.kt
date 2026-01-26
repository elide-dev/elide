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

/**
 * # Command Registry
 *
 * Extensible command registry for Colide Shell. Commands are registered with metadata
 * enabling help generation, tab completion, and categorization.
 *
 * ## Categories
 * - **system**: Core system commands (info, exit)
 * - **file**: File operations (ls, cat, cd)
 * - **ai**: AI assistant commands
 * - **gui**: GUI-related commands
 * - **dev**: Developer tools
 */
public object CommandRegistry {
    private val commands = mutableMapOf<String, Command>()
    private val aliases = mutableMapOf<String, String>()
    
    /**
     * Command metadata and handler.
     */
    public data class Command(
        val name: String,
        val description: String,
        val usage: String,
        val category: Category,
        val handler: (CommandContext) -> CommandResult
    )
    
    /**
     * Command categories.
     */
    public enum class Category(public val displayName: String) {
        SYSTEM("System"),
        FILE("Files"),
        AI("AI Assistant"),
        GUI("Interface"),
        DEV("Development"),
        CUSTOM("Custom")
    }
    
    /**
     * Command execution context.
     */
    public data class CommandContext(
        val args: List<String>,
        val rawInput: String,
        val shell: Any,  // Reference to shell for output
        val env: Map<String, String> = emptyMap()
    )
    
    /**
     * Command execution result.
     */
    public sealed class CommandResult {
        public data class Success(val output: String = "") : CommandResult()
        public data class Error(val message: String, val code: Int = 1) : CommandResult()
        public data class Output(val lines: List<String>) : CommandResult()
        public object Silent : CommandResult()
    }
    
    init {
        registerBuiltins()
    }
    
    /**
     * Register built-in commands.
     */
    private fun registerBuiltins() {
        register(Command(
            name = "help",
            description = "Show available commands",
            usage = "help [command]",
            category = Category.SYSTEM
        ) { ctx ->
            if (ctx.args.isNotEmpty()) {
                val cmd = get(ctx.args[0])
                if (cmd != null) {
                    CommandResult.Output(listOf(
                        "${cmd.name} - ${cmd.description}",
                        "Usage: ${cmd.usage}",
                        "Category: ${cmd.category.displayName}"
                    ))
                } else {
                    CommandResult.Error("Unknown command: ${ctx.args[0]}")
                }
            } else {
                val byCategory = commands.values.groupBy { it.category }
                val lines = mutableListOf<String>()
                lines.add("Colide Shell Commands")
                lines.add("─".repeat(40))
                for ((cat, cmds) in byCategory.toSortedMap(compareBy { it.ordinal })) {
                    lines.add("")
                    lines.add("${cat.displayName}:")
                    for (cmd in cmds.sortedBy { it.name }) {
                        lines.add("  ${cmd.name.padEnd(12)} ${cmd.description}")
                    }
                }
                CommandResult.Output(lines)
            }
        })
        
        register(Command(
            name = "clear",
            description = "Clear the screen",
            usage = "clear",
            category = Category.SYSTEM
        ) { _ ->
            CommandResult.Success("__CLEAR__")
        })
        
        register(Command(
            name = "exit",
            description = "Exit the shell",
            usage = "exit [code]",
            category = Category.SYSTEM
        ) { ctx ->
            val code = ctx.args.firstOrNull()?.toIntOrNull() ?: 0
            val exitCode = code
            CommandResult.Success("__EXIT:${exitCode}__")
        })
        
        register(Command(
            name = "echo",
            description = "Print arguments to output",
            usage = "echo [text...]",
            category = Category.SYSTEM
        ) { ctx ->
            CommandResult.Success(ctx.args.joinToString(" "))
        })
        
        register(Command(
            name = "env",
            description = "Show environment variables",
            usage = "env",
            category = Category.SYSTEM
        ) { ctx ->
            val lines = ctx.env.entries.map { "${it.key}=${it.value}" }.sorted()
            CommandResult.Output(lines.ifEmpty { listOf("No environment variables set") })
        })
        
        register(Command(
            name = "ls",
            description = "List directory contents",
            usage = "ls [path]",
            category = Category.FILE
        ) { ctx ->
            val path = ctx.args.firstOrNull() ?: "/zip"
            CommandResult.Output(listOf(
                "Contents of $path:",
                "  bin/",
                "  models/",
                "  (file listing requires native integration)"
            ))
        })
        
        register(Command(
            name = "cat",
            description = "Display file contents",
            usage = "cat <file>",
            category = Category.FILE
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                CommandResult.Error("Usage: cat <file>")
            } else {
                CommandResult.Output(listOf(
                    "File: ${ctx.args[0]}",
                    "(file reading requires native integration)"
                ))
            }
        })
        
        register(Command(
            name = "pwd",
            description = "Print working directory",
            usage = "pwd",
            category = Category.FILE
        ) { _ ->
            CommandResult.Success("/zip")
        })
        
        register(Command(
            name = "model",
            description = "Load/manage AI models",
            usage = "model <load|info|list> [path]",
            category = Category.AI
        ) { ctx ->
            when (ctx.args.firstOrNull()?.lowercase()) {
                "load" -> {
                    val path = ctx.args.drop(1).joinToString(" ")
                    if (path.isEmpty()) {
                        CommandResult.Error("Usage: model load <path-or-hf:repo>")
                    } else {
                        CommandResult.Success("__MODEL_LOAD:${path}__")
                    }
                }
                "info" -> CommandResult.Success("__MODEL_INFO__")
                "list" -> CommandResult.Output(listOf(
                    "Available models:",
                    "  /zip/share/tinyllama.gguf  (bundled, 1.1B)",
                    "",
                    "Load any GGUF model:",
                    "  model load /path/to/model.gguf",
                    "",
                    "In hosted mode, also supports HuggingFace:",
                    "  model load hf:openai/gpt-oss-20b",
                    "  model load hf:TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF"
                ))
                null, "help" -> CommandResult.Output(listOf(
                    "model load <path>   - Load GGUF model from path",
                    "model load hf:<id>  - Load from HuggingFace (hosted mode)",
                    "model info          - Show loaded model info",
                    "model list          - Show available models"
                ))
                else -> CommandResult.Error("Unknown subcommand: ${ctx.args[0]}")
            }
        })
        
        register(Command(
            name = "ai",
            description = "Ask the AI assistant",
            usage = "ai <question>",
            category = Category.AI
        ) { ctx ->
            if (ctx.args.isEmpty()) {
                CommandResult.Error("Usage: ai <your question>")
            } else {
                CommandResult.Success("__AI:${ctx.args.joinToString(" ")}__")
            }
        })
        
        register(Command(
            name = "chat",
            description = "Start interactive AI chat",
            usage = "chat",
            category = Category.AI
        ) { _ ->
            CommandResult.Success("__CHAT__")
        })
        
        register(Command(
            name = "gui",
            description = "Launch graphical interface",
            usage = "gui",
            category = Category.GUI
        ) { _ ->
            CommandResult.Success("__GUI__")
        })
        
        register(Command(
            name = "window",
            description = "Open a window",
            usage = "window <title>",
            category = Category.GUI
        ) { ctx ->
            val title = ctx.args.joinToString(" ").ifEmpty { "Untitled" }
            CommandResult.Success("__WINDOW:${title}__")
        })
        
        register(Command(
            name = "info",
            description = "System information",
            usage = "info",
            category = Category.SYSTEM
        ) { _ ->
            CommandResult.Output(listOf(
                "Colide OS - Elide-Powered Shell",
                "Runtime: Elide (GraalVM Polyglot)",
                "Architecture: True Unikernel (APE)",
                "Boot: Cosmopolitan Libc → UEFI/BIOS",
                "Display: VESA Framebuffer",
                "Input: PS/2 Keyboard + Mouse"
            ))
        })
        
        register(Command(
            name = "version",
            description = "Show version information",
            usage = "version",
            category = Category.SYSTEM
        ) { _ ->
            CommandResult.Output(listOf(
                "Colide OS v0.1.0",
                "Elide Runtime v1.0.0-alpha",
                "Built: Jan 2026"
            ))
        })
        
        alias("?", "help")
        alias("cls", "clear")
        alias("quit", "exit")
        alias("q", "exit")
        alias("dir", "ls")
        alias("type", "cat")
        alias("sysinfo", "info")
    }
    
    /**
     * Register a command.
     */
    public fun register(command: Command) {
        commands[command.name.lowercase()] = command
    }
    
    /**
     * Register an alias for a command.
     */
    public fun alias(alias: String, target: String) {
        aliases[alias.lowercase()] = target.lowercase()
    }
    
    /**
     * Get a command by name (resolves aliases).
     */
    public fun get(name: String): Command? {
        val key = name.lowercase()
        val resolved = aliases[key] ?: key
        return commands[resolved]
    }
    
    /**
     * Execute a command by name.
     */
    public fun execute(name: String, context: CommandContext): CommandResult {
        val cmd = get(name)
        return cmd?.handler?.invoke(context) ?: CommandResult.Error("Unknown command: $name")
    }
    
    /**
     * Get all registered commands.
     */
    public fun all(): Collection<Command> = commands.values
    
    /**
     * Get commands matching a prefix (for tab completion).
     */
    public fun completions(prefix: String): List<String> {
        val p = prefix.lowercase()
        val cmdMatches = commands.keys.filter { it.startsWith(p) }
        val aliasMatches = aliases.keys.filter { it.startsWith(p) }
        return (cmdMatches + aliasMatches).distinct().sorted()
    }
}
