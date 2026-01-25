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
import elide.colide.fs.FileSystem

/**
 * # Code Editor Widget
 *
 * A simple code editor for Colide IDE. Supports basic text editing,
 * line numbers, syntax highlighting placeholders, and cursor navigation.
 *
 * ## Features
 * - Line numbers with gutter
 * - Basic cursor movement (arrows, home, end)
 * - Text insertion and deletion
 * - Simple scrolling
 * - Modified indicator
 */
public class CodeEditor : Widget() {
    
    private val lines = mutableListOf("")
    private var cursorLine = 0
    private var cursorCol = 0
    private var scrollY = 0
    private var scrollX = 0
    private var modified = false
    private var highlighter: SyntaxHighlighter = SyntaxHighlighter.forLanguage(SyntaxHighlighter.Language.PLAIN)
    
    public var filePath: String? = null
        set(value) {
            field = value
            val ext = value?.substringAfterLast('.', "") ?: ""
            highlighter = SyntaxHighlighter.forExtension(ext)
        }
    
    public var syntaxHighlightingEnabled: Boolean = true
    public var onModified: ((Boolean) -> Unit)? = null
    
    private val visibleLines: Int get() = (height - HEADER_HEIGHT) / Font.CHAR_HEIGHT
    private val visibleCols: Int get() = (width - GUTTER_WIDTH - PADDING) / Font.CHAR_WIDTH
    
    init {
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
        focusable = true
        width = 500
        height = 400
    }
    
    override fun render() {
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        Vesa.fillRect(x, y, width, HEADER_HEIGHT, HEADER_BG)
        val title = filePath?.substringAfterLast('/') ?: "Untitled"
        val displayTitle = if (modified) "$title *" else title
        Font.drawText(x + PADDING, y + 4, displayTitle, HEADER_FG)
        
        Vesa.fillRect(x, y + HEADER_HEIGHT, GUTTER_WIDTH, height - HEADER_HEIGHT, GUTTER_BG)
        
        val startLine = scrollY
        val endLine = (scrollY + visibleLines).coerceAtMost(lines.size)
        
        for (lineIdx in startLine until endLine) {
            val screenY = y + HEADER_HEIGHT + (lineIdx - scrollY) * Font.CHAR_HEIGHT
            
            val lineNum = (lineIdx + 1).toString().padStart(4)
            val numColor = if (lineIdx == cursorLine) LINE_NUM_ACTIVE else LINE_NUM_COLOR
            Font.drawText(x + 2, screenY, lineNum, numColor)
            
            val line = lines[lineIdx]
            
            if (syntaxHighlightingEnabled && line.isNotEmpty()) {
                renderHighlightedLine(line, screenY)
            } else {
                val displayStart = scrollX.coerceAtMost(line.length)
                val displayEnd = (scrollX + visibleCols).coerceAtMost(line.length)
                val displayText = if (displayStart < displayEnd) {
                    line.substring(displayStart, displayEnd)
                } else ""
                Font.drawText(x + GUTTER_WIDTH + PADDING, screenY, displayText, foregroundColor)
            }
            
            if (lineIdx == cursorLine && focused) {
                val cursorScreenX = x + GUTTER_WIDTH + PADDING + (cursorCol - scrollX) * Font.CHAR_WIDTH
                if (cursorCol >= scrollX && cursorCol <= scrollX + visibleCols) {
                    Vesa.fillRect(cursorScreenX, screenY, 2, Font.CHAR_HEIGHT, CURSOR_COLOR)
                }
            }
        }
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x101 -> { // Up
                if (cursorLine > 0) {
                    cursorLine--
                    cursorCol = cursorCol.coerceAtMost(lines[cursorLine].length)
                    ensureCursorVisible()
                }
                return true
            }
            0x102 -> { // Down
                if (cursorLine < lines.lastIndex) {
                    cursorLine++
                    cursorCol = cursorCol.coerceAtMost(lines[cursorLine].length)
                    ensureCursorVisible()
                }
                return true
            }
            0x103 -> { // Left
                if (cursorCol > 0) {
                    cursorCol--
                } else if (cursorLine > 0) {
                    cursorLine--
                    cursorCol = lines[cursorLine].length
                }
                ensureCursorVisible()
                return true
            }
            0x104 -> { // Right
                if (cursorCol < lines[cursorLine].length) {
                    cursorCol++
                } else if (cursorLine < lines.lastIndex) {
                    cursorLine++
                    cursorCol = 0
                }
                ensureCursorVisible()
                return true
            }
            0x105 -> { // Home
                cursorCol = 0
                ensureCursorVisible()
                return true
            }
            0x106 -> { // End
                cursorCol = lines[cursorLine].length
                ensureCursorVisible()
                return true
            }
            '\n'.code, '\r'.code -> {
                val currentLine = lines[cursorLine]
                val beforeCursor = currentLine.substring(0, cursorCol)
                val afterCursor = currentLine.substring(cursorCol)
                lines[cursorLine] = beforeCursor
                lines.add(cursorLine + 1, afterCursor)
                cursorLine++
                cursorCol = 0
                setModified(true)
                ensureCursorVisible()
                return true
            }
            0x7F, 0x08 -> { // Backspace
                if (cursorCol > 0) {
                    val line = lines[cursorLine]
                    lines[cursorLine] = line.substring(0, cursorCol - 1) + line.substring(cursorCol)
                    cursorCol--
                    setModified(true)
                } else if (cursorLine > 0) {
                    val currentLine = lines.removeAt(cursorLine)
                    cursorLine--
                    cursorCol = lines[cursorLine].length
                    lines[cursorLine] = lines[cursorLine] + currentLine
                    setModified(true)
                }
                ensureCursorVisible()
                return true
            }
            0x7E -> { // Delete
                val line = lines[cursorLine]
                if (cursorCol < line.length) {
                    lines[cursorLine] = line.substring(0, cursorCol) + line.substring(cursorCol + 1)
                    setModified(true)
                } else if (cursorLine < lines.lastIndex) {
                    lines[cursorLine] = line + lines.removeAt(cursorLine + 1)
                    setModified(true)
                }
                return true
            }
            '\t'.code -> {
                insertText("    ")
                return true
            }
            in 0x20..0x7E -> {
                insertText(keyCode.toChar().toString())
                return true
            }
        }
        return false
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        if (my < y + HEADER_HEIGHT) return false
        
        val clickLine = (my - y - HEADER_HEIGHT) / Font.CHAR_HEIGHT + scrollY
        val clickCol = (mx - x - GUTTER_WIDTH - PADDING) / Font.CHAR_WIDTH + scrollX
        
        if (clickLine in lines.indices) {
            cursorLine = clickLine
            cursorCol = clickCol.coerceIn(0, lines[cursorLine].length)
        }
        
        return true
    }
    
    /**
     * Insert text at cursor position.
     */
    public fun insertText(text: String) {
        val line = lines[cursorLine]
        lines[cursorLine] = line.substring(0, cursorCol) + text + line.substring(cursorCol)
        cursorCol += text.length
        setModified(true)
        ensureCursorVisible()
    }
    
    /**
     * Set editor content.
     */
    public fun setText(content: String) {
        lines.clear()
        lines.addAll(content.split("\n"))
        if (lines.isEmpty()) lines.add("")
        cursorLine = 0
        cursorCol = 0
        scrollY = 0
        scrollX = 0
        setModified(false)
    }
    
    /**
     * Get editor content.
     */
    public fun getText(): String = lines.joinToString("\n")
    
    /**
     * Get current line count.
     */
    public fun getLineCount(): Int = lines.size
    
    /**
     * Get cursor position.
     */
    public fun getCursorPosition(): Pair<Int, Int> = cursorLine to cursorCol
    
    /**
     * Set cursor position.
     */
    public fun setCursorPosition(line: Int, col: Int) {
        cursorLine = line.coerceIn(0, lines.lastIndex)
        cursorCol = col.coerceIn(0, lines[cursorLine].length)
        ensureCursorVisible()
    }
    
    /**
     * Go to a specific line.
     */
    public fun gotoLine(line: Int) {
        setCursorPosition(line - 1, 0)
    }
    
    /**
     * Load file from path using FileSystem.
     */
    public fun loadFile(path: String): Boolean {
        val content = FileSystem.readText(path) ?: return false
        setText(content)
        filePath = path
        setModified(false)
        return true
    }
    
    /**
     * Save file to path using FileSystem.
     */
    public fun saveFile(path: String? = filePath): Boolean {
        val targetPath = path ?: return false
        val success = FileSystem.writeText(targetPath, getText())
        if (success) {
            filePath = targetPath
            setModified(false)
        }
        return success
    }
    
    /**
     * Check if content has been modified.
     */
    public fun isModified(): Boolean = modified
    
    private fun setModified(value: Boolean) {
        if (modified != value) {
            modified = value
            onModified?.invoke(value)
        }
    }
    
    private fun ensureCursorVisible() {
        if (cursorLine < scrollY) {
            scrollY = cursorLine
        } else if (cursorLine >= scrollY + visibleLines) {
            scrollY = cursorLine - visibleLines + 1
        }
        
        if (cursorCol < scrollX) {
            scrollX = cursorCol
        } else if (cursorCol >= scrollX + visibleCols) {
            scrollX = cursorCol - visibleCols + 1
        }
    }
    
    private fun renderHighlightedLine(line: String, screenY: Int) {
        val tokens = highlighter.tokenizeLine(line)
        var drawX = x + GUTTER_WIDTH + PADDING
        
        for (token in tokens) {
            if (token.end <= scrollX) continue
            if (token.start >= scrollX + visibleCols) break
            
            val visStart = (token.start - scrollX).coerceAtLeast(0)
            val visEnd = (token.end - scrollX).coerceAtMost(visibleCols)
            
            if (visStart < visEnd) {
                val textStart = if (token.start < scrollX) scrollX - token.start else 0
                val textEnd = textStart + (visEnd - visStart)
                val visibleText = token.text.substring(
                    textStart.coerceAtMost(token.text.length),
                    textEnd.coerceAtMost(token.text.length)
                )
                
                if (visibleText.isNotEmpty()) {
                    val tokenX = x + GUTTER_WIDTH + PADDING + visStart * Font.CHAR_WIDTH
                    Font.drawText(tokenX, screenY, visibleText, token.color)
                }
            }
        }
    }
    
    public companion object {
        private const val PADDING = 4
        private const val GUTTER_WIDTH = 40
        private const val HEADER_HEIGHT = 20
        private const val BG_COLOR = 0x001e1e2e
        private const val FG_COLOR = 0x00cdd6f4
        private const val HEADER_BG = 0x00313244
        private const val HEADER_FG = 0x00cdd6f4
        private const val GUTTER_BG = 0x00181825
        private const val LINE_NUM_COLOR = 0x006c7086
        private const val LINE_NUM_ACTIVE = 0x00cdd6f4
        private const val CURSOR_COLOR = 0x00f5e0dc
    }
}
