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

package elide.colide.tui

import elide.colide.ColideNative
import elide.colide.Vesa

/**
 * # TUI Renderer
 *
 * Text User Interface renderer for Colide OS. Supports both VESA framebuffer
 * (bare metal) and ANSI terminal (hosted mode).
 *
 * ## Features
 * - Box drawing characters for borders
 * - Color support (16-color ANSI + 24-bit VESA)
 * - Scrolling regions
 * - Cursor management
 */
public class TuiRenderer(
    private val width: Int = 80,
    private val height: Int = 25
) {
    private val buffer = Array(height) { CharArray(width) { ' ' } }
    private val colors = Array(height) { IntArray(width) { Color.DEFAULT.toInt() } }
    private var cursorX = 0
    private var cursorY = 0
    private var scrollTop = 0
    private var scrollBottom = height - 1
    
    private val isMetal = ColideNative.isAvailable && ColideNative.isMetal()
    
    /**
     * Color representation (fg + bg).
     */
    public data class Color(val fg: Int, val bg: Int) {
        public fun toInt(): Int = (bg shl 4) or fg
        
        public companion object {
            public val DEFAULT: Color = Color(7, 0)
            public val ERROR: Color = Color(1, 0)
            public val SUCCESS: Color = Color(2, 0)
            public val WARNING: Color = Color(3, 0)
            public val INFO: Color = Color(6, 0)
            public val HIGHLIGHT: Color = Color(0, 7)
            public val PROMPT: Color = Color(6, 0)
            
            public fun fromInt(value: Int): Color = Color(value and 0xF, (value shr 4) and 0xF)
        }
    }
    
    /**
     * Box drawing style.
     */
    public enum class BoxStyle(
        public val tl: Char,
        public val tr: Char,
        public val bl: Char,
        public val br: Char,
        public val h: Char,
        public val v: Char
    ) {
        SINGLE('┌', '┐', '└', '┘', '─', '│'),
        DOUBLE('╔', '╗', '╚', '╝', '═', '║'),
        ROUNDED('╭', '╮', '╰', '╯', '─', '│'),
        ASCII('+', '+', '+', '+', '-', '|')
    }
    
    /**
     * Clear the entire screen.
     */
    public fun clear(color: Color = Color.DEFAULT) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                buffer[y][x] = ' '
                colors[y][x] = color.toInt()
            }
        }
        cursorX = 0
        cursorY = 0
    }
    
    /**
     * Move cursor to position.
     */
    public fun moveTo(x: Int, y: Int) {
        cursorX = x.coerceIn(0, width - 1)
        cursorY = y.coerceIn(0, height - 1)
    }
    
    /**
     * Write a character at cursor position.
     */
    public fun putChar(ch: Char, color: Color = Color.DEFAULT) {
        if (cursorX in 0 until width && cursorY in 0 until height) {
            buffer[cursorY][cursorX] = ch
            colors[cursorY][cursorX] = color.toInt()
        }
        cursorX++
        if (cursorX >= width) {
            cursorX = 0
            cursorY++
            if (cursorY > scrollBottom) {
                scroll()
                cursorY = scrollBottom
            }
        }
    }
    
    /**
     * Write a string at cursor position.
     */
    public fun putString(text: String, color: Color = Color.DEFAULT) {
        for (ch in text) {
            when (ch) {
                '\n' -> {
                    cursorX = 0
                    cursorY++
                    if (cursorY > scrollBottom) {
                        scroll()
                        cursorY = scrollBottom
                    }
                }
                '\r' -> cursorX = 0
                '\t' -> {
                    val spaces = 4 - (cursorX % 4)
                    repeat(spaces) { putChar(' ', color) }
                }
                else -> putChar(ch, color)
            }
        }
    }
    
    /**
     * Write a string at specific position.
     */
    public fun putStringAt(x: Int, y: Int, text: String, color: Color = Color.DEFAULT) {
        moveTo(x, y)
        putString(text, color)
    }
    
    /**
     * Draw a box.
     */
    public fun drawBox(x: Int, y: Int, w: Int, h: Int, style: BoxStyle = BoxStyle.SINGLE, color: Color = Color.DEFAULT) {
        if (w < 2 || h < 2) return
        
        putStringAt(x, y, style.tl.toString(), color)
        repeat(w - 2) { putChar(style.h, color) }
        putChar(style.tr, color)
        
        for (row in 1 until h - 1) {
            putStringAt(x, y + row, style.v.toString(), color)
            moveTo(x + w - 1, y + row)
            putChar(style.v, color)
        }
        
        putStringAt(x, y + h - 1, style.bl.toString(), color)
        repeat(w - 2) { putChar(style.h, color) }
        putChar(style.br, color)
    }
    
    /**
     * Fill a rectangular area.
     */
    public fun fillRect(x: Int, y: Int, w: Int, h: Int, ch: Char = ' ', color: Color = Color.DEFAULT) {
        for (row in y until (y + h).coerceAtMost(height)) {
            for (col in x until (x + w).coerceAtMost(width)) {
                if (row in 0 until height && col in 0 until width) {
                    buffer[row][col] = ch
                    colors[row][col] = color.toInt()
                }
            }
        }
    }
    
    /**
     * Draw a horizontal line.
     */
    public fun hLine(x: Int, y: Int, length: Int, ch: Char = '─', color: Color = Color.DEFAULT) {
        moveTo(x, y)
        repeat(length.coerceAtMost(width - x)) {
            putChar(ch, color)
        }
    }
    
    /**
     * Draw a vertical line.
     */
    public fun vLine(x: Int, y: Int, length: Int, ch: Char = '│', color: Color = Color.DEFAULT) {
        for (i in 0 until length.coerceAtMost(height - y)) {
            putStringAt(x, y + i, ch.toString(), color)
        }
    }
    
    /**
     * Set scroll region.
     */
    public fun setScrollRegion(top: Int, bottom: Int) {
        scrollTop = top.coerceIn(0, height - 1)
        scrollBottom = bottom.coerceIn(scrollTop, height - 1)
    }
    
    /**
     * Scroll the content up by one line.
     */
    public fun scroll() {
        for (y in scrollTop until scrollBottom) {
            buffer[y] = buffer[y + 1].copyOf()
            colors[y] = colors[y + 1].copyOf()
        }
        buffer[scrollBottom].fill(' ')
        colors[scrollBottom].fill(Color.DEFAULT.toInt())
    }
    
    /**
     * Render to output (VESA or ANSI terminal).
     */
    public fun render() {
        if (isMetal) {
            renderVesa()
        } else {
            renderAnsi()
        }
    }
    
    private fun renderVesa() {
        val charW = 8
        val charH = 16
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val ch = buffer[y][x]
                val colorVal = colors[y][x]
                val color = Color.fromInt(colorVal)
                val fgArgb = ansiToArgb(color.fg)
                val bgArgb = ansiToArgb(color.bg)
                
                val px = x * charW
                val py = y * charH
                
                Vesa.fillRect(px, py, charW, charH, bgArgb)
                if (ch != ' ') {
                    drawCharVesa(ch, px, py, fgArgb)
                }
            }
        }
    }
    
    private fun drawCharVesa(ch: Char, x: Int, y: Int, color: Int) {
        Vesa.fillRect(x + 1, y + 2, 6, 12, color)
    }
    
    private fun renderAnsi() {
        val sb = StringBuilder()
        sb.append("\u001b[H")
        
        var lastColor = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val colorVal = colors[y][x]
                if (colorVal != lastColor) {
                    val c = Color.fromInt(colorVal)
                    sb.append("\u001b[${30 + c.fg};${40 + c.bg}m")
                    lastColor = colorVal
                }
                sb.append(buffer[y][x])
            }
            if (y < height - 1) sb.append("\n")
        }
        sb.append("\u001b[0m")
        
        print(sb.toString())
    }
    
    /**
     * Convert ANSI color index to ARGB.
     */
    private fun ansiToArgb(ansi: Int): Int {
        return when (ansi) {
            0 -> 0x00000000
            1 -> 0x00CC0000
            2 -> 0x0000CC00
            3 -> 0x00CCCC00
            4 -> 0x000000CC
            5 -> 0x00CC00CC
            6 -> 0x0000CCCC
            7 -> 0x00CCCCCC
            8 -> 0x00666666
            9 -> 0x00FF0000
            10 -> 0x0000FF00
            11 -> 0x00FFFF00
            12 -> 0x000000FF
            13 -> 0x00FF00FF
            14 -> 0x0000FFFF
            15 -> 0x00FFFFFF
            else -> 0x00CCCCCC
        }
    }
    
    /**
     * Get character at position.
     */
    public fun getChar(x: Int, y: Int): Char {
        return if (x in 0 until width && y in 0 until height) buffer[y][x] else ' '
    }
    
    /**
     * Get buffer dimensions.
     */
    public fun getWidth(): Int = width
    public fun getHeight(): Int = height
    public fun getCursorX(): Int = cursorX
    public fun getCursorY(): Int = cursorY
}
