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
 * # Widget Base Class
 *
 * Foundation for all GUI widgets in Colide OS.
 *
 * ## Widget Hierarchy
 * ```
 * Widget (abstract)
 * ├── Container
 * │   ├── Window
 * │   └── Panel
 * ├── Label
 * ├── Button
 * ├── TextInput
 * └── Canvas
 * ```
 *
 * ## Coordinate System
 * - Origin (0,0) is top-left of screen
 * - X increases rightward, Y increases downward
 * - All coordinates are in pixels
 */
public abstract class Widget {
    /** X position relative to parent (or screen if root) */
    public var x: Int = 0
    
    /** Y position relative to parent (or screen if root) */
    public var y: Int = 0
    
    /** Widget width in pixels */
    public var width: Int = 100
    
    /** Widget height in pixels */
    public var height: Int = 30
    
    /** Whether widget is visible */
    public var visible: Boolean = true
    
    /** Whether widget can receive focus */
    public var focusable: Boolean = false
    
    /** Whether widget currently has focus */
    public var focused: Boolean = false
    
    /** Parent container (null if root) */
    public var parent: Container? = null
    
    /** Background color (ARGB) */
    public var backgroundColor: Int = Colors.BACKGROUND
    
    /** Foreground/text color (ARGB) */
    public var foregroundColor: Int = Colors.FOREGROUND
    
    /** Border color (ARGB) */
    public var borderColor: Int = Colors.BORDER
    
    /** Whether to draw border */
    public var showBorder: Boolean = false
    
    /** Border width in pixels */
    public var borderWidth: Int = 1
    
    /**
     * Get absolute X position on screen.
     */
    public val absoluteX: Int
        get() = (parent?.absoluteX ?: 0) + x
    
    /**
     * Get absolute Y position on screen.
     */
    public val absoluteY: Int
        get() = (parent?.absoluteY ?: 0) + y
    
    /**
     * Check if point (px, py) is inside this widget.
     */
    public fun contains(px: Int, py: Int): Boolean {
        val ax = absoluteX
        val ay = absoluteY
        return px >= ax && px < ax + width && py >= ay && py < ay + height
    }
    
    /**
     * Render the widget. Override in subclasses.
     */
    public abstract fun render()
    
    /**
     * Handle mouse click. Returns true if handled.
     */
    public open fun onMouseClick(mx: Int, my: Int, button: Int): Boolean = false
    
    /**
     * Handle mouse move.
     */
    public open fun onMouseMove(mx: Int, my: Int) {}
    
    /**
     * Handle key press. Returns true if handled.
     */
    public open fun onKeyPress(keyCode: Int): Boolean = false
    
    /**
     * Handle character input. Returns true if handled.
     */
    public open fun onCharInput(ch: Char): Boolean = false
    
    /**
     * Called when widget gains focus.
     */
    public open fun onFocus() {
        focused = true
    }
    
    /**
     * Called when widget loses focus.
     */
    public open fun onBlur() {
        focused = false
    }
    
    /**
     * Request redraw of this widget.
     */
    public fun invalidate() {
        if (visible) render()
    }
    
    /**
     * Fill the widget background.
     */
    protected fun fillBackground() {
        Vesa.fillRect(absoluteX, absoluteY, width, height, backgroundColor)
    }
    
    /**
     * Draw the widget border.
     */
    protected fun drawBorder() {
        if (!showBorder) return
        val ax = absoluteX
        val ay = absoluteY
        val color = if (focused) Colors.ACCENT else borderColor
        
        // Top
        Vesa.fillRect(ax, ay, width, borderWidth, color)
        // Bottom
        Vesa.fillRect(ax, ay + height - borderWidth, width, borderWidth, color)
        // Left
        Vesa.fillRect(ax, ay, borderWidth, height, color)
        // Right
        Vesa.fillRect(ax + width - borderWidth, ay, borderWidth, height, color)
    }
}

/**
 * Standard color palette for Colide GUI.
 */
public object Colors {
    /** Dark background */
    public const val BACKGROUND: Int = 0x001a1a2e
    
    /** Light foreground/text */
    public const val FOREGROUND: Int = 0x00eef5db
    
    /** Border color */
    public const val BORDER: Int = 0x004a4a6a
    
    /** Accent color (cyan) */
    public const val ACCENT: Int = 0x0016e0bd
    
    /** Button normal */
    public const val BUTTON: Int = 0x002d2d44
    
    /** Button hover */
    public const val BUTTON_HOVER: Int = 0x003d3d5c
    
    /** Button pressed */
    public const val BUTTON_PRESSED: Int = 0x001d1d34
    
    /** Error/warning */
    public const val ERROR: Int = 0x00e74c3c
    
    /** Success */
    public const val SUCCESS: Int = 0x0027ae60
    
    /** Input field background */
    public const val INPUT_BG: Int = 0x00121220
    
    /** Window title bar */
    public const val TITLEBAR: Int = 0x002a2a4a
    
    /** Transparent */
    public const val TRANSPARENT: Int = 0x00000000
}
