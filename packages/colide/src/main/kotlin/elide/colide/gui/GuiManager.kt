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

import elide.colide.ColideNative
import elide.colide.Keyboard
import elide.colide.Mouse
import elide.colide.Vesa

/**
 * # GUI Manager
 *
 * Central manager for the Colide GUI system.
 * Handles window management, input dispatch, and rendering.
 *
 * ## Usage
 * ```kotlin
 * val gui = GuiManager()
 * 
 * val window = Window("My App")
 * window.add(Label("Hello!"))
 * window.add(Button("Click") { println("Clicked!") })
 * 
 * gui.addWindow(window)
 * gui.run()
 * ```
 */
public class GuiManager {
    /** All managed windows */
    private val windows: MutableList<Window> = mutableListOf()
    
    /** Modal dialogs (rendered on top of windows) */
    private val dialogs: MutableList<Dialog> = mutableListOf()
    
    /** Currently focused/active window */
    private var activeWindow: Window? = null
    
    /** Desktop background color */
    public var backgroundColor: Int = 0x00102030
    
    /** Whether GUI is running */
    private var running: Boolean = false
    
    /** Mouse state */
    private var mouseX: Int = 0
    private var mouseY: Int = 0
    private var mouseButtons: Int = 0
    private var lastMouseButtons: Int = 0
    
    /**
     * Add a window to the GUI.
     */
    public fun addWindow(window: Window) {
        windows.add(window)
        activeWindow = window
    }
    
    /**
     * Remove a window from the GUI.
     */
    public fun removeWindow(window: Window) {
        windows.remove(window)
        if (activeWindow == window) {
            activeWindow = windows.lastOrNull()
        }
    }
    
    /**
     * Bring window to front.
     */
    public fun bringToFront(window: Window) {
        if (windows.remove(window)) {
            windows.add(window)
            activeWindow = window
        }
    }
    
    /**
     * Show a modal dialog.
     */
    public fun showDialog(dialog: Dialog) {
        dialogs.add(dialog)
        dialog.focused = true
    }
    
    /**
     * Remove a dialog.
     */
    public fun removeDialog(dialog: Dialog) {
        dialogs.remove(dialog)
    }
    
    /**
     * Check if any dialog is currently shown.
     */
    public fun hasActiveDialog(): Boolean = dialogs.isNotEmpty()
    
    /**
     * Initialize the GUI system.
     */
    public fun init(): Boolean {
        if (!ColideNative.ensureInitialized()) {
            System.err.println("Failed to initialize Colide native drivers")
            return false
        }
        
        mouseX = Vesa.width / 2
        mouseY = Vesa.height / 2
        
        return true
    }
    
    /**
     * Run the GUI main loop.
     */
    public fun run() {
        if (!init()) return
        running = true
        
        while (running) {
            processInput()
            render()
            Thread.sleep(16) // ~60 FPS
        }
    }
    
    /**
     * Stop the GUI.
     */
    public fun stop() {
        running = false
    }
    
    /**
     * Process input (keyboard + mouse).
     */
    private fun processInput() {
        // Keyboard input
        if (Keyboard.available()) {
            val key = Keyboard.getChar()
            
            // If dialog is active, send input to dialog first
            if (dialogs.isNotEmpty()) {
                val activeDialog = dialogs.last()
                if (activeDialog.onKeyPress(key)) return
            } else {
                if (key == 0x1B) { // Escape (only if no dialog)
                    stop()
                    return
                }
                activeWindow?.onKeyPress(key)
            }
        }
        
        // Mouse input (via native)
        updateMouse()
        
        // Mouse click detection
        val leftClick = (mouseButtons and 1) != 0 && (lastMouseButtons and 1) == 0
        val rightClick = (mouseButtons and 2) != 0 && (lastMouseButtons and 2) == 0
        
        if (leftClick) {
            // If dialog is active, send click to dialog first
            if (dialogs.isNotEmpty()) {
                val activeDialog = dialogs.last()
                if (activeDialog.onMouseClick(mouseX, mouseY, 1)) {
                    lastMouseButtons = mouseButtons
                    return
                }
            }
            handleMouseClick(mouseX, mouseY, 1)
        }
        if (rightClick) {
            handleMouseClick(mouseX, mouseY, 2)
        }
        
        // Mouse move
        windows.forEach { it.onMouseMove(mouseX, mouseY) }
        
        // Mouse release - stop window dragging
        if ((lastMouseButtons and 1) != 0 && (mouseButtons and 1) == 0) {
            windows.forEach { it.stopDrag() }
        }
        
        lastMouseButtons = mouseButtons
    }
    
    /**
     * Update mouse position from native driver.
     */
    private fun updateMouse() {
        // Use dedicated Mouse driver
        if (Mouse.isInitialized() || Mouse.init()) {
            Mouse.poll()
            mouseX = Mouse.getX()
            mouseY = Mouse.getY()
            mouseButtons = Mouse.getButtons()
        } else if (ColideNative.isMetal()) {
            // Fallback to ColideNative mouse methods
            mouseX = ColideNative.mouseX()
            mouseY = ColideNative.mouseY()
            mouseButtons = ColideNative.mouseButtons()
        }
    }
    
    /**
     * Handle mouse click.
     */
    private fun handleMouseClick(x: Int, y: Int, button: Int) {
        // Find window under cursor (top-most first)
        for (i in windows.indices.reversed()) {
            val window = windows[i]
            if (window.visible && window.contains(x, y)) {
                bringToFront(window)
                window.onMouseClick(x, y, button)
                return
            }
        }
    }
    
    /**
     * Render all windows and cursor.
     */
    private fun render() {
        // Clear background
        Vesa.clear(backgroundColor)
        
        // Render windows (bottom to top)
        windows.forEach { window ->
            if (window.visible) {
                window.render()
            }
        }
        
        // Render dialogs on top of windows (modal overlay)
        if (dialogs.isNotEmpty()) {
            // Optional: dim background for modal effect
            dialogs.forEach { dialog ->
                if (dialog.visible) {
                    dialog.render()
                }
            }
        }
        
        // Render mouse cursor
        renderCursor(mouseX, mouseY)
    }
    
    /**
     * Render mouse cursor.
     */
    private fun renderCursor(x: Int, y: Int) {
        // Simple arrow cursor
        val cursorData = listOf(
            "X",
            "XX",
            "XXX",
            "XXXX",
            "XXXXX",
            "XXXXXX",
            "XXXXXXX",
            "XXXXXXXX",
            "XXXXX",
            "XX XX",
            "X  XX",
            "    XX",
            "    XX",
            "     X"
        )
        
        for ((dy, row) in cursorData.withIndex()) {
            for ((dx, ch) in row.withIndex()) {
                if (ch == 'X') {
                    val px = x + dx
                    val py = y + dy
                    if (px in 0 until Vesa.width && py in 0 until Vesa.height) {
                        Vesa.putPixel(px, py, Colors.FOREGROUND)
                    }
                }
            }
        }
    }
    
    public companion object {
        /**
         * Create and run a simple demo GUI.
         */
        @JvmStatic
        public fun demo() {
            val gui = GuiManager()
            
            val window = Window("Colide Demo")
            window.width = 320
            window.height = 240
            window.center()
            
            val label = Label("Welcome to Colide GUI!")
            label.x = 20
            label.y = 20
            window.add(label)
            
            val input = TextInput("Type here...")
            input.x = 20
            input.y = 50
            input.width = 200
            window.add(input)
            
            val button = Button("Click Me") {
                println("Button clicked! Text: ${input.text}")
            }
            button.x = 20
            button.y = 90
            button.autoSize()
            window.add(button)
            
            val closeBtn = Button("Close") {
                gui.stop()
            }
            closeBtn.x = 20
            closeBtn.y = 130
            closeBtn.autoSize()
            window.add(closeBtn)
            
            gui.addWindow(window)
            gui.run()
        }
    }
}
