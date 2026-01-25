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
    
    public var prompt: String = "$ "
    public var maxLines: Int = 1000
    public var onCommand: ((String) -> Unit)? = null
    
    private val cols: Int get() = (width - PADDING * 2) / Font.CHAR_WIDTH
    private val rows: Int get() = (height - PADDING * 2) / Font.CHAR_HEIGHT
    
    /**
     * Line in the terminal buffer.
     */
    private data class Line(
        val text: String,
        val color: Int = DEFAULT_FG,
        val isInput: Boolean = false
    )
    
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
            Font.drawText(x + PADDING, lineY, line.text, line.color)
            lineY += Font.CHAR_HEIGHT
        }
        
        val inputY = y + height - PADDING - Font.CHAR_HEIGHT
        val inputLine = prompt + inputBuffer.toString()
        Font.drawText(x + PADDING, inputY, inputLine, PROMPT_COLOR)
        
        if (cursorVisible && System.currentTimeMillis() - cursorBlink < 500) {
            val cursorX = x + PADDING + (prompt.length + inputBuffer.length) * Font.CHAR_WIDTH
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
                if (command.isNotBlank()) {
                    onCommand?.invoke(command)
                }
                return true
            }
            key == 0x7F || key == 0x08 -> {
                if (inputBuffer.isNotEmpty()) {
                    inputBuffer.deleteAt(inputBuffer.length - 1)
                }
                return true
            }
            key == 0x1B -> {
                inputBuffer.clear()
                return true
            }
            key in 0x20..0x7E -> {
                inputBuffer.append(key.toChar())
                return true
            }
            key == 0x103 -> {
                scrollUp()
                return true
            }
            key == 0x102 -> {
                scrollDown()
                return true
            }
        }
        return false
    }
    
    /**
     * Print a line to the terminal.
     */
    public fun println(text: String, color: Int = foregroundColor) {
        val wrapped = wrapText(text, cols)
        for (line in wrapped) {
            buffer.add(Line(line, color))
        }
        trimBuffer()
        scrollToBottom()
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
    
    public companion object {
        private const val PADDING = 4
        private const val BG_COLOR = 0x001a1a2e
        private const val DEFAULT_FG = 0x00eef5db
        private const val PROMPT_COLOR = 0x0016e0bd
        private const val ERROR_COLOR = 0x00e74c3c
        private const val SUCCESS_COLOR = 0x0027ae60
        private const val INFO_COLOR = 0x003498db
        private const val CURSOR_COLOR = 0x00ffffff
    }
}
