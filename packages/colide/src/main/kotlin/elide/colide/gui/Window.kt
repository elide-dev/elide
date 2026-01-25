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
 * # Window Widget
 *
 * A draggable window with title bar, close button, and content area.
 *
 * ## Features
 * - Title bar with drag support
 * - Close button (optional)
 * - Minimize button (optional)
 * - Resizable (optional)
 * - Drop shadow effect
 */
public class Window(
    public var title: String = "Window"
) : Container() {
    
    /** Title bar height */
    public val titleBarHeight: Int = 24
    
    /** Whether window can be closed */
    public var closable: Boolean = true
    
    /** Whether window can be minimized */
    public var minimizable: Boolean = false
    
    /** Whether window is minimized */
    public var minimized: Boolean = false
        private set
    
    /** Whether window is being dragged */
    private var dragging: Boolean = false
    private var dragOffsetX: Int = 0
    private var dragOffsetY: Int = 0
    
    /** Close handler */
    private var onClose: (() -> Unit)? = null
    
    init {
        backgroundColor = Colors.BACKGROUND
        showBorder = true
        borderColor = Colors.BORDER
        width = 300
        height = 200
    }
    
    /**
     * Get content area Y offset (below title bar).
     */
    public val contentY: Int get() = titleBarHeight
    
    /**
     * Set close handler.
     */
    public fun setOnClose(handler: () -> Unit) {
        onClose = handler
    }
    
    /**
     * Close the window.
     */
    public fun close() {
        visible = false
        onClose?.invoke()
    }
    
    /**
     * Minimize the window.
     */
    public fun minimize() {
        minimized = true
    }
    
    /**
     * Restore minimized window.
     */
    public fun restore() {
        minimized = false
    }
    
    /**
     * Center the window on screen.
     */
    public fun center() {
        x = (Vesa.width - width) / 2
        y = (Vesa.height - height) / 2
    }
    
    override fun render() {
        if (!visible) return
        
        val ax = absoluteX
        val ay = absoluteY
        
        if (minimized) {
            Vesa.fillRect(ax, ay, width, titleBarHeight, Colors.TITLEBAR)
            Vesa.fillRect(ax, ay, width, 1, Colors.BORDER)
            Vesa.fillRect(ax, ay + titleBarHeight - 1, width, 1, Colors.BORDER)
            Vesa.fillRect(ax, ay, 1, titleBarHeight, Colors.BORDER)
            Vesa.fillRect(ax + width - 1, ay, 1, titleBarHeight, Colors.BORDER)
            Font.drawText(ax + 8, ay + 4, title, Colors.FOREGROUND)
            return
        }
        
        // Draw shadow
        Vesa.fillRect(ax + 4, ay + 4, width, height, 0x00101020)
        
        // Draw window background
        fillBackground()
        
        // Draw title bar
        Vesa.fillRect(ax, ay, width, titleBarHeight, Colors.TITLEBAR)
        
        // Title text
        Font.drawText(ax + 8, ay + 4, title, Colors.FOREGROUND)
        
        // Close button
        if (closable) {
            val btnX = ax + width - 20
            val btnY = ay + 4
            Vesa.fillRect(btnX, btnY, 16, 16, Colors.ERROR)
            Font.drawText(btnX + 4, btnY, "X", Colors.FOREGROUND)
        }
        
        // Minimize button
        if (minimizable) {
            val btnX = ax + width - (if (closable) 40 else 20)
            val btnY = ay + 4
            Vesa.fillRect(btnX, btnY, 16, 16, Colors.BUTTON)
            Font.drawText(btnX + 4, btnY, "_", Colors.FOREGROUND)
        }
        
        // Draw border
        drawBorder()
        
        // Draw children (in content area)
        children.forEach { child ->
            if (child.visible) child.render()
        }
    }
    
    override fun onMouseClick(mx: Int, my: Int, button: Int): Boolean {
        val ax = absoluteX
        val ay = absoluteY
        
        // Check title bar clicks
        if (my >= ay && my < ay + titleBarHeight) {
            // Close button
            if (closable && mx >= ax + width - 20 && mx < ax + width - 4) {
                close()
                return true
            }
            
            // Minimize button
            if (minimizable) {
                val btnX = ax + width - (if (closable) 40 else 20)
                if (mx >= btnX && mx < btnX + 16) {
                    if (minimized) restore() else minimize()
                    return true
                }
            }
            
            // Start dragging
            dragging = true
            dragOffsetX = mx - ax
            dragOffsetY = my - ay
            return true
        }
        
        // Delegate to content
        if (!minimized) {
            return super.onMouseClick(mx, my, button)
        }
        
        return false
    }
    
    override fun onMouseMove(mx: Int, my: Int) {
        if (dragging) {
            x = mx - dragOffsetX
            y = my - dragOffsetY
            
            // Clamp to screen bounds
            if (x < 0) x = 0
            if (y < 0) y = 0
            if (x + width > Vesa.width) x = Vesa.width - width
            if (y + height > Vesa.height) y = Vesa.height - height
        } else if (!minimized) {
            super.onMouseMove(mx, my)
        }
    }
    
    /**
     * Stop dragging (called on mouse release).
     */
    public fun stopDrag() {
        dragging = false
    }
    
    /**
     * Add a widget to the window's content area.
     * Y position is automatically offset by title bar height.
     */
    public fun addContent(child: Widget) {
        child.y += titleBarHeight
        add(child)
    }
}
