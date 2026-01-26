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
 * # Status Bar
 *
 * Displays editor metadata: line/column position, file encoding,
 * language mode, and other status indicators.
 *
 * ## Usage
 * ```kotlin
 * val statusBar = StatusBar()
 * statusBar.line = 42
 * statusBar.column = 15
 * statusBar.language = "Kotlin"
 * statusBar.encoding = "UTF-8"
 * ```
 */
public class StatusBar : Widget() {
    
    public var line: Int = 1
    public var column: Int = 1
    public var language: String = "Plain Text"
    public var encoding: String = "UTF-8"
    public var lineEnding: String = "LF"
    public var tabSize: Int = 4
    public var modified: Boolean = false
    public var readOnly: Boolean = false
    
    public var onLanguageClick: (() -> Unit)? = null
    public var onEncodingClick: (() -> Unit)? = null
    public var onLineEndingClick: (() -> Unit)? = null
    
    init {
        height = BAR_HEIGHT
        backgroundColor = BG_COLOR
        foregroundColor = FG_COLOR
    }
    
    override fun render() {
        Vesa.fillRect(x, y, width, height, backgroundColor)
        
        Vesa.fillRect(x, y, width, 1, BORDER_COLOR)
        
        var drawX = x + PADDING
        
        // Line:Column indicator
        val posText = "Ln $line, Col $column"
        Font.drawText(drawX, y + 4, posText, foregroundColor)
        drawX += posText.length * Font.CHAR_WIDTH + SECTION_GAP
        
        // Separator
        Vesa.fillRect(drawX, y + 2, 1, height - 4, SEPARATOR_COLOR)
        drawX += SECTION_GAP
        
        // Tab size
        val tabText = "Spaces: $tabSize"
        Font.drawText(drawX, y + 4, tabText, DIMMED_COLOR)
        drawX += tabText.length * Font.CHAR_WIDTH + SECTION_GAP
        
        // Separator
        Vesa.fillRect(drawX, y + 2, 1, height - 4, SEPARATOR_COLOR)
        drawX += SECTION_GAP
        
        // Line ending
        Font.drawText(drawX, y + 4, lineEnding, DIMMED_COLOR)
        drawX += lineEnding.length * Font.CHAR_WIDTH + SECTION_GAP
        
        // Separator
        Vesa.fillRect(drawX, y + 2, 1, height - 4, SEPARATOR_COLOR)
        drawX += SECTION_GAP
        
        // Encoding
        Font.drawText(drawX, y + 4, encoding, DIMMED_COLOR)
        drawX += encoding.length * Font.CHAR_WIDTH + SECTION_GAP
        
        // Right-aligned items
        var rightX = x + width - PADDING
        
        // Language mode (right-aligned)
        val langWidth = language.length * Font.CHAR_WIDTH
        rightX -= langWidth
        Font.drawText(rightX, y + 4, language, ACCENT_COLOR)
        rightX -= SECTION_GAP
        
        // Separator
        Vesa.fillRect(rightX, y + 2, 1, height - 4, SEPARATOR_COLOR)
        rightX -= SECTION_GAP
        
        // Modified indicator
        if (modified) {
            val modText = "Modified"
            rightX -= modText.length * Font.CHAR_WIDTH
            Font.drawText(rightX, y + 4, modText, WARNING_COLOR)
            rightX -= SECTION_GAP
        }
        
        // Read-only indicator
        if (readOnly) {
            val roText = "Read-Only"
            rightX -= roText.length * Font.CHAR_WIDTH
            Font.drawText(rightX, y + 4, roText, DIMMED_COLOR)
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        // Calculate click regions for interactive elements
        // For now, just return false (not handled)
        return false
    }
    
    /**
     * Update position from editor cursor.
     */
    public fun updatePosition(line: Int, column: Int) {
        this.line = line
        this.column = column
    }
    
    /**
     * Update language from file extension.
     */
    public fun updateLanguage(extension: String) {
        language = when (extension.lowercase()) {
            "kt", "kts" -> "Kotlin"
            "js" -> "JavaScript"
            "ts" -> "TypeScript"
            "jsx", "tsx" -> "React"
            "py" -> "Python"
            "rs" -> "Rust"
            "c", "h" -> "C"
            "cpp", "hpp", "cc" -> "C++"
            "java" -> "Java"
            "json" -> "JSON"
            "md", "markdown" -> "Markdown"
            "xml" -> "XML"
            "html", "htm" -> "HTML"
            "css" -> "CSS"
            "sh", "bash" -> "Shell"
            "yaml", "yml" -> "YAML"
            "toml" -> "TOML"
            "sql" -> "SQL"
            else -> "Plain Text"
        }
    }
    
    public companion object {
        public const val BAR_HEIGHT: Int = 22
        
        private const val PADDING = 8
        private const val SECTION_GAP = 12
        
        private const val BG_COLOR = 0x00181825
        private const val FG_COLOR = 0x00cdd6f4
        private const val DIMMED_COLOR = 0x006c7086
        private const val ACCENT_COLOR = 0x0089b4fa
        private const val WARNING_COLOR = 0x00f9e2af
        private const val BORDER_COLOR = 0x00313244
        private const val SEPARATOR_COLOR = 0x00313244
    }
}
