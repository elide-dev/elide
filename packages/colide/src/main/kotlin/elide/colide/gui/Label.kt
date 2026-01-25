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

/**
 * # Label Widget
 *
 * A simple text label for displaying static text.
 */
public class Label(
    public var text: String = "",
    public var align: TextAlign = TextAlign.LEFT
) : Widget() {
    
    init {
        focusable = false
        backgroundColor = Colors.TRANSPARENT
        height = Font.CHAR_HEIGHT + 4
    }
    
    /**
     * Auto-size the label to fit its text.
     */
    public fun autoSize() {
        width = Font.textWidth(text) + 8
        height = Font.CHAR_HEIGHT + 4
    }
    
    override fun render() {
        if (!visible) return
        
        if (backgroundColor != Colors.TRANSPARENT) {
            fillBackground()
        }
        
        val textX = when (align) {
            TextAlign.LEFT -> absoluteX + 4
            TextAlign.CENTER -> absoluteX + (width - Font.textWidth(text)) / 2
            TextAlign.RIGHT -> absoluteX + width - Font.textWidth(text) - 4
        }
        
        Font.drawText(textX, absoluteY + 2, text, foregroundColor)
    }
}

/**
 * Text alignment options.
 */
public enum class TextAlign {
    LEFT, CENTER, RIGHT
}
