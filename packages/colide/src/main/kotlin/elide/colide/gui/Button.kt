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
 * # Button Widget
 *
 * A clickable button with text label.
 *
 * ## Example
 * ```kotlin
 * val btn = Button("Click Me") {
 *     println("Button clicked!")
 * }
 * container.add(btn)
 * ```
 */
public class Button(
    public var text: String = "Button",
    private var onClick: (() -> Unit)? = null
) : Widget() {
    
    /** Whether mouse is hovering over button */
    public var hovered: Boolean = false
        private set
    
    /** Whether button is currently pressed */
    public var pressed: Boolean = false
        private set
    
    init {
        focusable = true
        backgroundColor = Colors.BUTTON
        showBorder = true
        borderColor = Colors.BORDER
        height = Font.CHAR_HEIGHT + 12
        width = 100
    }
    
    /**
     * Set click handler.
     */
    public fun setOnClick(handler: () -> Unit) {
        onClick = handler
    }
    
    /**
     * Auto-size button to fit text.
     */
    public fun autoSize() {
        width = Font.textWidth(text) + 24
    }
    
    override fun render() {
        if (!visible) return
        
        val bg = when {
            pressed -> Colors.BUTTON_PRESSED
            hovered || focused -> Colors.BUTTON_HOVER
            else -> Colors.BUTTON
        }
        
        backgroundColor = bg
        fillBackground()
        drawBorder()
        
        val textX = absoluteX + (width - Font.textWidth(text)) / 2
        val textY = absoluteY + (height - Font.CHAR_HEIGHT) / 2
        Font.drawText(textX, textY, text, foregroundColor)
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        if (button == 1) { // Left click
            pressed = true
            invalidate()
            onClick?.invoke()
            pressed = false
            invalidate()
            return true
        }
        return false
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        val wasHovered = hovered
        hovered = contains(mx, my)
        if (wasHovered != hovered) invalidate()
    }
    
    override fun onKeyPress(keyCode: Int): Boolean {
        if (keyCode == 0x0D || keyCode == 0x20) { // Enter or Space
            onClick?.invoke()
            return true
        }
        return false
    }
}
