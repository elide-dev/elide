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
 * # TextInput Widget
 *
 * A single-line text input field.
 *
 * ## Features
 * - Text cursor with blinking
 * - Basic text editing (backspace, delete)
 * - Placeholder text
 * - Password mode (mask characters)
 */
public class TextInput(
    public var placeholder: String = ""
) : Widget() {
    
    /** Current text value */
    public var text: String = ""
        private set
    
    /** Cursor position in text */
    private var cursorPos: Int = 0
    
    /** Whether to mask input (password mode) */
    public var password: Boolean = false
    
    /** Maximum text length (0 = unlimited) */
    public var maxLength: Int = 0
    
    /** Called when text changes */
    private var onChange: ((String) -> Unit)? = null
    
    /** Called when Enter is pressed */
    private var onSubmit: ((String) -> Unit)? = null
    
    /** Cursor blink state */
    private var cursorVisible: Boolean = true
    
    init {
        focusable = true
        backgroundColor = Colors.INPUT_BG
        showBorder = true
        borderColor = Colors.BORDER
        height = Font.CHAR_HEIGHT + 8
        width = 200
    }
    
    /**
     * Set the text value.
     */
    public fun setText(value: String) {
        text = if (maxLength > 0) value.take(maxLength) else value
        cursorPos = text.length
        onChange?.invoke(text)
    }
    
    /**
     * Clear the text.
     */
    public fun clear() {
        text = ""
        cursorPos = 0
        onChange?.invoke(text)
    }
    
    /**
     * Set change handler.
     */
    public fun setOnChange(handler: (String) -> Unit) {
        onChange = handler
    }
    
    /**
     * Set submit handler.
     */
    public fun setOnSubmit(handler: (String) -> Unit) {
        onSubmit = handler
    }
    
    override fun render() {
        if (!visible) return
        
        fillBackground()
        drawBorder()
        
        val ax = absoluteX
        val ay = absoluteY
        val textY = ay + (height - Font.CHAR_HEIGHT) / 2
        
        val displayText = when {
            text.isEmpty() -> placeholder
            password -> "*".repeat(text.length)
            else -> text
        }
        
        val textColor = if (text.isEmpty()) Colors.BORDER else foregroundColor
        Font.drawText(ax + 4, textY, displayText, textColor)
        
        if (focused && cursorVisible) {
            val cursorX = ax + 4 + cursorPos * Font.CHAR_WIDTH
            Vesa.fillRect(cursorX, textY, 2, Font.CHAR_HEIGHT, Colors.ACCENT)
        }
    }
    
    override fun onCharInput(ch: Char): Boolean {
        if (ch.code in 0x20..0x7E) {
            if (maxLength == 0 || text.length < maxLength) {
                text = text.substring(0, cursorPos) + ch + text.substring(cursorPos)
                cursorPos++
                onChange?.invoke(text)
                invalidate()
                return true
            }
        }
        return false
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        when (keyCode) {
            0x08, 0x7F -> { // Backspace
                if (cursorPos > 0) {
                    text = text.substring(0, cursorPos - 1) + text.substring(cursorPos)
                    cursorPos--
                    onChange?.invoke(text)
                    invalidate()
                    return true
                }
            }
            0x7F -> { // Delete
                if (cursorPos < text.length) {
                    text = text.substring(0, cursorPos) + text.substring(cursorPos + 1)
                    onChange?.invoke(text)
                    invalidate()
                    return true
                }
            }
            0x25 -> { // Left arrow
                if (cursorPos > 0) {
                    cursorPos--
                    invalidate()
                    return true
                }
            }
            0x27 -> { // Right arrow
                if (cursorPos < text.length) {
                    cursorPos++
                    invalidate()
                    return true
                }
            }
            0x24 -> { // Home
                cursorPos = 0
                invalidate()
                return true
            }
            0x23 -> { // End
                cursorPos = text.length
                invalidate()
                return true
            }
            0x0D -> { // Enter
                onSubmit?.invoke(text)
                return true
            }
        }
        return false
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        val relX = mx - absoluteX - 4
        cursorPos = (relX / Font.CHAR_WIDTH).coerceIn(0, text.length)
        invalidate()
        return true
    }
    
    override fun onFocus() {
        super.onFocus()
        cursorVisible = true
    }
}
