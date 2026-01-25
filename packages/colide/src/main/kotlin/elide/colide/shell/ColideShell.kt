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
import elide.colide.Vesa
import elide.colide.Ai

/**
 * # Colide Shell
 *
 * An Elide-powered shell for Colide OS. This shell:
 * - Uses native VESA for display output
 * - Uses native PS/2 keyboard for input
 * - Integrates with local AI for assistance
 * - Runs entirely on Elide runtime (no fork/exec)
 *
 * ## Architecture
 * ```
 * User Input → Keyboard (JNI) → ColideShell (Kotlin) → Command Execution
 *                                       ↓
 *                              Vesa (JNI) → Display
 * ```
 */
public class ColideShell {
    private val commands = mutableMapOf<String, CommandHandler>()
    private var running = true
    private var cursorX = 0
    private var cursorY = 0
    
    // Colors (ARGB format)
    private val bgColor = 0x001a1a2e  // Dark blue
    private val fgColor = 0x00eef5db  // Light cream
    private val promptColor = 0x0016e0bd  // Cyan
    private val errorColor = 0x00e74c3c  // Red
    
    // Font metrics (8x16 pixel font)
    private val charWidth = 8
    private val charHeight = 16
    
    init {
        registerBuiltins()
    }
    
    /**
     * Register built-in shell commands.
     */
    private fun registerBuiltins() {
        commands["help"] = CommandHandler { _ ->
            printLine("Colide Shell - Built on Elide Runtime")
            printLine("Commands:")
            printLine("  help     - Show this help")
            printLine("  clear    - Clear screen")
            printLine("  ai <msg> - Ask AI assistant")
            printLine("  info     - System information")
            printLine("  exit     - Exit shell")
            true
        }
        
        commands["clear"] = CommandHandler { _ ->
            clear()
            true
        }
        
        commands["info"] = CommandHandler { _ ->
            printLine("Colide OS - Elide-Powered Shell")
            printLine("Runtime: Elide (GraalVM)")
            printLine("Display: ${Vesa.width}x${Vesa.height}")
            printLine("Metal: ${if (ColideNative.isMetal()) "Yes (bare metal)" else "No (hosted)"}")
            true
        }
        
        commands["ai"] = CommandHandler { args ->
            if (args.isEmpty()) {
                printLine("Usage: ai <your question>", errorColor)
                return@CommandHandler false
            }
            val prompt = args.joinToString(" ")
            printLine("Thinking...", promptColor)
            val response = Ai.complete(prompt)
            if (response.isNotEmpty()) {
                printLine(response)
            } else {
                printLine("AI not available", errorColor)
            }
            true
        }
        
        commands["exit"] = CommandHandler { _ ->
            running = false
            printLine("Goodbye!")
            true
        }
    }
    
    /**
     * Initialize and start the shell.
     */
    public fun start() {
        if (!ColideNative.ensureInitialized()) {
            System.err.println("Failed to initialize Colide native drivers")
            return
        }
        
        clear()
        printBanner()
        prompt()
        
        val lineBuffer = StringBuilder()
        
        while (running) {
            if (Keyboard.available()) {
                val ch = Keyboard.getChar()
                when {
                    ch == '\n'.code || ch == '\r'.code -> {
                        printLine("")
                        val line = lineBuffer.toString().trim()
                        if (line.isNotEmpty()) {
                            executeCommand(line)
                        }
                        lineBuffer.clear()
                        if (running) prompt()
                    }
                    ch == 0x7F || ch == 0x08 -> { // Backspace
                        if (lineBuffer.isNotEmpty()) {
                            lineBuffer.deleteAt(lineBuffer.length - 1)
                            cursorX -= charWidth
                            Vesa.fillRect(cursorX, cursorY, charWidth, charHeight, bgColor)
                        }
                    }
                    ch in 0x20..0x7E -> { // Printable ASCII
                        lineBuffer.append(ch.toChar())
                        drawChar(ch.toChar(), cursorX, cursorY, fgColor)
                        cursorX += charWidth
                    }
                }
            }
        }
    }
    
    /**
     * Execute a shell command.
     */
    private fun executeCommand(line: String) {
        val parts = line.split(" ", limit = 2)
        val cmd = parts[0].lowercase()
        val args = if (parts.size > 1) parts[1].split(" ") else emptyList()
        
        val handler = commands[cmd]
        if (handler != null) {
            handler.execute(args)
        } else {
            printLine("Unknown command: $cmd", errorColor)
            printLine("Type 'help' for available commands")
        }
    }
    
    /**
     * Clear the screen.
     */
    private fun clear() {
        Vesa.clear(bgColor)
        cursorX = 0
        cursorY = 0
    }
    
    /**
     * Print the shell banner.
     */
    private fun printBanner() {
        printLine("╔══════════════════════════════════════════════════════════════╗", promptColor)
        printLine("║  COLIDE OS - Elide-Powered Shell                             ║", promptColor)
        printLine("║  Cosmo boots it, Elide runs it                               ║", promptColor)
        printLine("╚══════════════════════════════════════════════════════════════╝", promptColor)
        printLine("")
        printLine("Type 'help' for available commands.")
        printLine("")
    }
    
    /**
     * Print the command prompt.
     */
    private fun prompt() {
        printString("colide> ", promptColor)
    }
    
    /**
     * Print a line of text.
     */
    private fun printLine(text: String, color: Int = fgColor) {
        printString(text, color)
        cursorX = 0
        cursorY += charHeight
        
        // Scroll if needed
        if (cursorY + charHeight > Vesa.height) {
            // Simple scroll: just wrap to top for now
            cursorY = 0
            clear()
            printBanner()
        }
    }
    
    /**
     * Print a string without newline.
     */
    private fun printString(text: String, color: Int = fgColor) {
        for (ch in text) {
            if (cursorX + charWidth > Vesa.width) {
                cursorX = 0
                cursorY += charHeight
            }
            drawChar(ch, cursorX, cursorY, color)
            cursorX += charWidth
        }
    }
    
    /**
     * Draw a character at the given position.
     * This is a simple 8x16 bitmap font renderer.
     */
    private fun drawChar(ch: Char, x: Int, y: Int, color: Int) {
        // For now, use a filled rect as placeholder
        // Real implementation would use bitmap font
        if (ch != ' ') {
            Vesa.fillRect(x + 1, y + 2, charWidth - 2, charHeight - 4, color)
        }
    }
    
    /**
     * Command handler interface.
     */
    public fun interface CommandHandler {
        public fun execute(args: List<String>): Boolean
    }
    
    public companion object {
        /**
         * Main entry point for the Colide Shell.
         */
        @JvmStatic
        public fun main(args: Array<String>) {
            val shell = ColideShell()
            shell.start()
        }
    }
}
