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

package elide.colide.gui

import elide.colide.Vesa

/**
 * # Terminal Widget
 *
 * A terminal emulator widget for running the Colide shell in graphical mode.
 * Features scrollback buffer, ANSI color support, and keyboard input handling.
 *
 * ## Usage
 * ```kotlin
 * val terminal = Terminal(0, 0, 640, 400)
 * terminal.println("Welcome to Colide OS")
 * terminal.prompt = "colide> "
 * terminal.onCommand = { cmd -> executeCommand(cmd) }
 * ```
 */
public class Terminal : Widget() {
    
    private val buffer = mutableListOf<Line>()
    private val inputBuffer = StringBuilder()
    private var scrollOffset = 0
    private var cursorVisible = true
    private var cursorBlink = 0L
    private var cursorPos = 0
    
    private val commandHistory = mutableListOf<String>()
    private var historyIndex = -1
    private var historyFile: String? = null
    
    public var prompt: String = "$ "
    public var maxLines: Int = 1000
    public var maxHistory: Int = 500
    public var onCommand: ((String) -> Unit)? = null
    public var onTabComplete: ((String) -> List<String>)? = null
    
    private val cols: Int get() = (width - PADDING * 2) / Font.CHAR_WIDTH
    private val rows: Int get() = (height - PADDING * 2) / Font.CHAR_HEIGHT
    
    /**
     * Span of styled text within a line.
     */
    private data class Span(
        val text: String,
        val fg: Int = DEFAULT_FG,
        val bg: Int = BG_COLOR,
        val bold: Boolean = false
    )
    
    /**
     * Line in the terminal buffer with multiple styled spans.
     */
    private data class Line(
        val spans: List<Span>,
        val isInput: Boolean = false
    ) {
        constructor(text: String, color: Int = DEFAULT_FG, isInput: Boolean = false) : 
            this(listOf(Span(text, color)), isInput)
        
        val text: String get() = spans.joinToString("") { it.text }
    }
    
    init {
        backgroundColor = BG_COLOR
        foregroundColor = DEFAULT_FG
        focusable = true
    }
    
    override fun render() {
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        val visibleLines = buffer.drop(scrollOffset).take(rows - 1)
        var lineY = y + PADDING
        
        for (line in visibleLines) {
            var spanX = x + PADDING
            for (span in line.spans) {
                if (span.bg != BG_COLOR) {
                    Vesa.fillRect(spanX, lineY, span.text.length * Font.CHAR_WIDTH, Font.CHAR_HEIGHT, span.bg)
                }
                Font.drawText(spanX, lineY, span.text, span.fg)
                spanX += span.text.length * Font.CHAR_WIDTH
            }
            lineY += Font.CHAR_HEIGHT
        }
        
        val inputY = y + height - PADDING - Font.CHAR_HEIGHT
        val inputLine = prompt + inputBuffer.toString()
        Font.drawText(x + PADDING, inputY, inputLine, PROMPT_COLOR)
        
        if (cursorVisible && System.currentTimeMillis() - cursorBlink < 500) {
            val cursorX = x + PADDING + (prompt.length + cursorPos) * Font.CHAR_WIDTH
            Vesa.fillRect(cursorX, inputY, 2, Font.CHAR_HEIGHT, CURSOR_COLOR)
        } else if (System.currentTimeMillis() - cursorBlink >= 1000) {
            cursorBlink = System.currentTimeMillis()
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        val key = keyCode
        when {
            key == '\n'.code || key == '\r'.code -> {
                val command = inputBuffer.toString()
                println("$prompt$command", PROMPT_COLOR)
                inputBuffer.clear()
                cursorPos = 0
                if (command.isNotBlank()) {
                    addToHistory(command)
                    onCommand?.invoke(command)
                }
                historyIndex = -1
                return true
            }
            key == 0x7F || key == 0x08 -> { // Backspace
                if (cursorPos > 0) {
                    inputBuffer.deleteAt(cursorPos - 1)
                    cursorPos--
                }
                return true
            }
            key == 0x7E -> { // Delete
                if (cursorPos < inputBuffer.length) {
                    inputBuffer.deleteAt(cursorPos)
                }
                return true
            }
            key == 0x1B -> { // Escape
                inputBuffer.clear()
                cursorPos = 0
                return true
            }
            key == 0x09 -> { // Tab
                handleTabComplete()
                return true
            }
            key == 0x26 -> { // Up arrow - history
                navigateHistory(-1)
                return true
            }
            key == 0x28 -> { // Down arrow - history
                navigateHistory(1)
                return true
            }
            key == 0x25 -> { // Left arrow
                if (cursorPos > 0) cursorPos--
                return true
            }
            key == 0x27 -> { // Right arrow
                if (cursorPos < inputBuffer.length) cursorPos++
                return true
            }
            key == 0x24 -> { // Home
                cursorPos = 0
                return true
            }
            key == 0x23 -> { // End
                cursorPos = inputBuffer.length
                return true
            }
            key == 0x103 -> { // Page Up
                scrollUp()
                return true
            }
            key == 0x102 -> { // Page Down
                scrollDown()
                return true
            }
            key in 0x20..0x7E -> {
                inputBuffer.insert(cursorPos, key.toChar())
                cursorPos++
                return true
            }
        }
        return false
    }
    
    private fun navigateHistory(direction: Int) {
        if (commandHistory.isEmpty()) return
        
        val newIndex = historyIndex + direction
        if (newIndex < -1) return
        if (newIndex >= commandHistory.size) return
        
        historyIndex = newIndex
        inputBuffer.clear()
        if (historyIndex >= 0) {
            inputBuffer.append(commandHistory[commandHistory.size - 1 - historyIndex])
        }
        cursorPos = inputBuffer.length
    }
    
    private fun addToHistory(command: String) {
        if (commandHistory.lastOrNull() != command) {
            commandHistory.add(command)
            if (commandHistory.size > maxHistory) {
                commandHistory.removeAt(0)
            }
        }
    }
    
    private fun handleTabComplete() {
        val completions = onTabComplete?.invoke(inputBuffer.toString()) ?: return
        if (completions.isEmpty()) return
        
        if (completions.size == 1) {
            inputBuffer.clear()
            inputBuffer.append(completions[0])
            cursorPos = inputBuffer.length
        } else {
            // Show completions
            println("")
            val joined = completions.joinToString("  ")
            println(joined, INFO_COLOR)
            // Find common prefix
            val prefix = completions.reduce { acc, s ->
                acc.commonPrefixWith(s)
            }
            if (prefix.length > inputBuffer.length) {
                inputBuffer.clear()
                inputBuffer.append(prefix)
                cursorPos = inputBuffer.length
            }
        }
    }
    
    /**
     * Print a line to the terminal with ANSI escape code parsing.
     */
    public fun println(text: String, color: Int = foregroundColor) {
        val spans = parseAnsiCodes(text, color)
        val lineText = spans.joinToString("") { it.text }
        val wrapped = wrapText(lineText, cols)
        
        if (wrapped.size == 1) {
            buffer.add(Line(spans))
        } else {
            // For wrapped text, distribute spans across lines
            for (line in wrapped) {
                buffer.add(Line(line, color))
            }
        }
        trimBuffer()
        scrollToBottom()
    }
    
    /**
     * Parse ANSI escape codes and return styled spans.
     */
    private fun parseAnsiCodes(text: String, defaultColor: Int): List<Span> {
        val spans = mutableListOf<Span>()
        var currentFg = defaultColor
        var currentBg = BG_COLOR
        var currentBold = false
        var i = 0
        val currentText = StringBuilder()
        
        while (i < text.length) {
            if (text[i] == '\u001B' && i + 1 < text.length && text[i + 1] == '[') {
                // Flush current text
                if (currentText.isNotEmpty()) {
                    spans.add(Span(currentText.toString(), currentFg, currentBg, currentBold))
                    currentText.clear()
                }
                
                // Parse escape sequence
                val endIdx = text.indexOf('m', i + 2)
                if (endIdx > i + 2) {
                    val codes = text.substring(i + 2, endIdx).split(';')
                    for (code in codes) {
                        when (code.toIntOrNull()) {
                            0 -> { currentFg = defaultColor; currentBg = BG_COLOR; currentBold = false }
                            1 -> currentBold = true
                            30 -> currentFg = ANSI_BLACK
                            31 -> currentFg = ANSI_RED
                            32 -> currentFg = ANSI_GREEN
                            33 -> currentFg = ANSI_YELLOW
                            34 -> currentFg = ANSI_BLUE
                            35 -> currentFg = ANSI_MAGENTA
                            36 -> currentFg = ANSI_CYAN
                            37 -> currentFg = ANSI_WHITE
                            90 -> currentFg = ANSI_BRIGHT_BLACK
                            91 -> currentFg = ANSI_BRIGHT_RED
                            92 -> currentFg = ANSI_BRIGHT_GREEN
                            93 -> currentFg = ANSI_BRIGHT_YELLOW
                            94 -> currentFg = ANSI_BRIGHT_BLUE
                            95 -> currentFg = ANSI_BRIGHT_MAGENTA
                            96 -> currentFg = ANSI_BRIGHT_CYAN
                            97 -> currentFg = ANSI_BRIGHT_WHITE
                            40 -> currentBg = ANSI_BLACK
                            41 -> currentBg = ANSI_RED
                            42 -> currentBg = ANSI_GREEN
                            43 -> currentBg = ANSI_YELLOW
                            44 -> currentBg = ANSI_BLUE
                            45 -> currentBg = ANSI_MAGENTA
                            46 -> currentBg = ANSI_CYAN
                            47 -> currentBg = ANSI_WHITE
                        }
                    }
                    i = endIdx + 1
                    continue
                }
            }
            currentText.append(text[i])
            i++
        }
        
        if (currentText.isNotEmpty()) {
            spans.add(Span(currentText.toString(), currentFg, currentBg, currentBold))
        }
        
        return if (spans.isEmpty()) listOf(Span(text, defaultColor)) else spans
    }
    
    /**
     * Print text without newline.
     */
    public fun print(text: String, color: Int = foregroundColor) {
        if (buffer.isEmpty()) {
            buffer.add(Line(text, color))
        } else {
            val last = buffer.removeAt(buffer.lastIndex)
            buffer.add(Line(last.text + text, color))
        }
    }
    
    /**
     * Print an error message.
     */
    public fun printError(text: String) {
        println(text, ERROR_COLOR)
    }
    
    /**
     * Print a success message.
     */
    public fun printSuccess(text: String) {
        println(text, SUCCESS_COLOR)
    }
    
    /**
     * Print info message.
     */
    public fun printInfo(text: String) {
        println(text, INFO_COLOR)
    }
    
    /**
     * Clear the terminal.
     */
    public fun clear() {
        buffer.clear()
        scrollOffset = 0
        inputBuffer.clear()
    }
    
    /**
     * Scroll up by one line.
     */
    public fun scrollUp() {
        if (scrollOffset > 0) {
            scrollOffset--
        }
    }
    
    /**
     * Scroll down by one line.
     */
    public fun scrollDown() {
        val maxScroll = (buffer.size - rows + 1).coerceAtLeast(0)
        if (scrollOffset < maxScroll) {
            scrollOffset++
        }
    }
    
    /**
     * Scroll to bottom of buffer.
     */
    public fun scrollToBottom() {
        scrollOffset = (buffer.size - rows + 1).coerceAtLeast(0)
    }
    
    /**
     * Set input text programmatically.
     */
    public fun setInput(text: String) {
        inputBuffer.clear()
        inputBuffer.append(text)
    }
    
    /**
     * Get current input text.
     */
    public fun getInput(): String = inputBuffer.toString()
    
    private fun wrapText(text: String, maxWidth: Int): List<String> {
        if (text.length <= maxWidth) return listOf(text)
        
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxWidth) {
            val breakPoint = remaining.lastIndexOf(' ', maxWidth)
            val idx = if (breakPoint > 0) breakPoint else maxWidth
            lines.add(remaining.substring(0, idx))
            remaining = remaining.substring(idx).trimStart()
        }
        if (remaining.isNotEmpty()) {
            lines.add(remaining)
        }
        return lines
    }
    
    private fun trimBuffer() {
        while (buffer.size > maxLines) {
            buffer.removeAt(0)
            if (scrollOffset > 0) scrollOffset--
        }
    }
    
    /**
     * Load command history from file.
     */
    public fun loadHistory(path: String) {
        historyFile = path
        try {
            val content = elide.colide.fs.FileSystem.readText(path)
            content?.lines()?.filter { it.isNotBlank() }?.forEach {
                commandHistory.add(it)
            }
            while (commandHistory.size > maxHistory) {
                commandHistory.removeAt(0)
            }
        } catch (_: Exception) {
            // History file doesn't exist yet
        }
    }
    
    /**
     * Save command history to file.
     */
    public fun saveHistory() {
        historyFile?.let { path ->
            try {
                val content = commandHistory.takeLast(maxHistory).joinToString("\n")
                elide.colide.fs.FileSystem.writeText(path, content)
            } catch (_: Exception) {
                // Ignore save errors
            }
        }
    }
    
    /**
     * Get command history.
     */
    public fun getHistory(): List<String> = commandHistory.toList()
    
    public companion object {
        private const val PADDING = 4
        private const val BG_COLOR = 0x001a1a2e
        private const val DEFAULT_FG = 0x00eef5db
        private const val PROMPT_COLOR = 0x0016e0bd
        private const val ERROR_COLOR = 0x00e74c3c
        private const val SUCCESS_COLOR = 0x0027ae60
        private const val INFO_COLOR = 0x003498db
        private const val CURSOR_COLOR = 0x00ffffff
        
        // ANSI colors (Catppuccin Mocha palette)
        private const val ANSI_BLACK = 0x0045475a
        private const val ANSI_RED = 0x00f38ba8
        private const val ANSI_GREEN = 0x00a6e3a1
        private const val ANSI_YELLOW = 0x00f9e2af
        private const val ANSI_BLUE = 0x0089b4fa
        private const val ANSI_MAGENTA = 0x00cba6f7
        private const val ANSI_CYAN = 0x0094e2d5
        private const val ANSI_WHITE = 0x00bac2de
        
        private const val ANSI_BRIGHT_BLACK = 0x00585b70
        private const val ANSI_BRIGHT_RED = 0x00f38ba8
        private const val ANSI_BRIGHT_GREEN = 0x00a6e3a1
        private const val ANSI_BRIGHT_YELLOW = 0x00f9e2af
        private const val ANSI_BRIGHT_BLUE = 0x0089b4fa
        private const val ANSI_BRIGHT_MAGENTA = 0x00cba6f7
        private const val ANSI_BRIGHT_CYAN = 0x0094e2d5
        private const val ANSI_BRIGHT_WHITE = 0x00a6adc8
    }
}
