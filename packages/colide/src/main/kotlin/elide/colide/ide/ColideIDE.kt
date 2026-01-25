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

package elide.colide.ide

import elide.colide.ColideNative
import elide.colide.Vesa
import elide.colide.gui.*
import elide.colide.shell.AiAssistant
import elide.colide.shell.CommandRegistry

/**
 * # Colide IDE
 *
 * The integrated development environment for Colide OS.
 * Combines the shell, file browser, code editor, and AI assistant
 * into a complete IDE experience running on Elide.
 *
 * ## Layout
 * ```
 * ┌─────────────────────────────────────────────────────────┐
 * │                      Menu Bar                            │
 * ├──────────────┬──────────────────────────────────────────┤
 * │              │                                          │
 * │  File        │           Code Editor                    │
 * │  Browser     │                                          │
 * │              │                                          │
 * │              ├──────────────────────────────────────────┤
 * │              │           Terminal / AI Chat             │
 * └──────────────┴──────────────────────────────────────────┘
 * ```
 */
public class ColideIDE {
    private val guiManager = GuiManager()
    private var fileBrowser: FileBrowser? = null
    private var codeEditor: CodeEditor? = null
    private var terminal: Terminal? = null
    private var aiAssistant = AiAssistant.getInstance()
    
    private var running = true
    private var screenWidth = 800
    private var screenHeight = 600
    
    /**
     * Initialize and start the IDE.
     */
    public fun start() {
        if (!ColideNative.ensureInitialized()) {
            System.err.println("Failed to initialize Colide native drivers")
            return
        }
        
        screenWidth = ColideNative.screenWidth()
        screenHeight = ColideNative.screenHeight()
        
        setupLayout()
        
        guiManager.run()
    }
    
    /**
     * Set up the IDE window layout.
     */
    private fun setupLayout() {
        val mainWindow = Window().apply {
            x = 0
            y = 0
            width = screenWidth
            height = screenHeight
            title = "Colide IDE - Elide-Powered Development"
            closable = false
            minimizable = false
        }
        
        val sidebarWidth = 200
        val bottomHeight = 150
        val menuHeight = 24
        val contentY = menuHeight
        val editorHeight = screenHeight - menuHeight - bottomHeight - 30
        
        fileBrowser = FileBrowser().apply {
            x = 0
            y = contentY
            width = sidebarWidth
            height = screenHeight - menuHeight - 30
        }
        mainWindow.add(fileBrowser!!)
        
        codeEditor = CodeEditor().apply {
            x = sidebarWidth + 2
            y = contentY
            width = screenWidth - sidebarWidth - 4
            height = editorHeight
        }
        mainWindow.add(codeEditor!!)
        
        terminal = Terminal().apply {
            x = sidebarWidth + 2
            y = contentY + editorHeight + 2
            width = screenWidth - sidebarWidth - 4
            height = bottomHeight - 4
            prompt = "colide> "
            onCommand = { cmd -> executeCommand(cmd) }
        }
        mainWindow.add(terminal!!)
        
        fileBrowser?.onFileOpened = { entry ->
            if (!entry.isDirectory) {
                openFile(entry.path)
            }
        }
        
        guiManager.addWindow(mainWindow)
        
        terminal?.println("Colide IDE - Elide-Powered Development Environment")
        terminal?.println("Type 'help' for available commands")
        terminal?.println("")
    }
    
    /**
     * Execute a shell command.
     */
    private fun executeCommand(input: String) {
        val parts = input.trim().split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1].split(" ") else emptyList()
        
        val context = CommandRegistry.CommandContext(
            args = args,
            rawInput = input,
            shell = this
        )
        
        when (val result = CommandRegistry.execute(cmd, context)) {
            is CommandRegistry.CommandResult.Success -> {
                val output = result.output
                when {
                    output == "__CLEAR__" -> terminal?.clear()
                    output.startsWith("__EXIT:") -> {
                        terminal?.println("Goodbye!")
                        running = false
                    }
                    output.startsWith("__AI:") -> {
                        val question = output.removePrefix("__AI:")
                        terminal?.println("Thinking...", 0x0016e0bd)
                        val response = aiAssistant.ask(question.removeSuffix("__"))
                        terminal?.println(response)
                    }
                    output == "__GUI__" -> terminal?.println("Already in GUI mode!")
                    output.startsWith("__WINDOW:") -> {
                        val title = output.removePrefix("__WINDOW:").removeSuffix("__")
                        createNewWindow(title)
                    }
                    output == "__CHAT__" -> startAiChat()
                    output.isNotEmpty() -> terminal?.println(output)
                }
            }
            is CommandRegistry.CommandResult.Error -> {
                terminal?.printError(result.message)
            }
            is CommandRegistry.CommandResult.Output -> {
                for (line in result.lines) {
                    terminal?.println(line)
                }
            }
            is CommandRegistry.CommandResult.Silent -> {}
        }
    }
    
    /**
     * Open a file in the editor.
     */
    private fun openFile(path: String) {
        val success = codeEditor?.loadFile(path) ?: false
        if (success) {
            terminal?.printSuccess("Opened: $path")
        } else {
            terminal?.printError("Failed to open: $path")
        }
    }
    
    /**
     * Create a new window.
     */
    private fun createNewWindow(title: String) {
        val newWindow = Window().apply {
            x = 100
            y = 100
            width = 400
            height = 300
            this.title = title
        }
        
        val label = Label().apply {
            x = 10
            y = 40
            text = "New window: $title"
        }
        newWindow.add(label)
        
        guiManager.addWindow(newWindow)
        terminal?.printSuccess("Created window: $title")
    }
    
    /**
     * Start interactive AI chat mode.
     */
    private fun startAiChat() {
        terminal?.println("AI Chat Mode - Type 'exit' to return to shell")
        terminal?.prompt = "ai> "
    }
    
    public companion object {
        /**
         * Main entry point for the Colide IDE.
         */
        @JvmStatic
        public fun main(args: Array<String>) {
            val ide = ColideIDE()
            ide.start()
        }
    }
}
