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

package elide.colide

/**
 * # PS/2 Mouse Driver
 *
 * Dedicated mouse input for bare metal via Intel 8042 controller.
 * Separated from keyboard for cleaner architecture and proper event handling.
 *
 * ## Features
 * - Absolute position tracking with bounds
 * - Left, right, middle button support
 * - Scroll wheel (Intellimouse protocol)
 * - Click and drag detection
 *
 * ## Example
 * ```kotlin
 * // Initialize mouse
 * Mouse.init()
 * Mouse.setBounds(screenWidth, screenHeight)
 *
 * // Main loop
 * while (running) {
 *     Mouse.poll()
 *     
 *     val x = Mouse.getX()
 *     val y = Mouse.getY()
 *     
 *     if (Mouse.isLeftPressed()) {
 *         // Handle click at (x, y)
 *     }
 * }
 * ```
 */
public object Mouse {
    /** Button bit flags */
    public const val BUTTON_LEFT: Int = 0x01
    public const val BUTTON_RIGHT: Int = 0x02
    public const val BUTTON_MIDDLE: Int = 0x04
    
    private var lastButtons = 0
    private var clickX = 0
    private var clickY = 0
    
    /**
     * Initialize PS/2 mouse driver.
     * @return true if initialization succeeded
     */
    @JvmStatic
    public external fun init(): Boolean
    
    /**
     * Poll for mouse events.
     * Call this regularly in your main loop.
     * @return true if new data available
     */
    @JvmStatic
    public external fun poll(): Boolean
    
    /**
     * Get current X position.
     */
    @JvmStatic
    public external fun getX(): Int
    
    /**
     * Get current Y position.
     */
    @JvmStatic
    public external fun getY(): Int
    
    /**
     * Get button state bitmask.
     * @return Bitmask of BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE
     */
    @JvmStatic
    public external fun getButtons(): Int
    
    /**
     * Get scroll wheel delta since last call.
     * Positive = scroll up, negative = scroll down.
     */
    @JvmStatic
    public external fun getWheel(): Int
    
    /**
     * Set mouse movement bounds.
     * Mouse position will be clamped to these limits.
     */
    @JvmStatic
    public external fun setBounds(maxX: Int, maxY: Int)
    
    /**
     * Check if mouse driver is initialized.
     */
    @JvmStatic
    public external fun isInitialized(): Boolean
    
    /** Check if left button is pressed */
    public fun isLeftPressed(): Boolean = (getButtons() and BUTTON_LEFT) != 0
    
    /** Check if right button is pressed */
    public fun isRightPressed(): Boolean = (getButtons() and BUTTON_RIGHT) != 0
    
    /** Check if middle button is pressed */
    public fun isMiddlePressed(): Boolean = (getButtons() and BUTTON_MIDDLE) != 0
    
    /**
     * Check if left button was just clicked (press + release).
     * Call after poll() to detect clicks.
     */
    public fun wasLeftClicked(): Boolean {
        val buttons = getButtons()
        val wasPressed = (lastButtons and BUTTON_LEFT) != 0
        val isPressed = (buttons and BUTTON_LEFT) != 0
        
        if (!wasPressed && isPressed) {
            clickX = getX()
            clickY = getY()
        }
        
        lastButtons = buttons
        return wasPressed && !isPressed
    }
    
    /**
     * Check if right button was just clicked.
     */
    public fun wasRightClicked(): Boolean {
        val buttons = getButtons()
        val wasPressed = (lastButtons and BUTTON_RIGHT) != 0
        val isPressed = (buttons and BUTTON_RIGHT) != 0
        lastButtons = buttons
        return wasPressed && !isPressed
    }
    
    /**
     * Get position where last click started.
     */
    public fun getClickPosition(): Pair<Int, Int> = clickX to clickY
    
    /**
     * Check if mouse is being dragged (left button held while moving).
     */
    public fun isDragging(): Boolean {
        return isLeftPressed() && (getX() != clickX || getY() != clickY)
    }
    
    /**
     * Mouse event for callbacks.
     */
    public data class MouseEvent(
        val x: Int,
        val y: Int,
        val buttons: Int,
        val wheel: Int,
        val type: EventType
    ) {
        public enum class EventType {
            MOVE,
            BUTTON_DOWN,
            BUTTON_UP,
            WHEEL
        }
    }
    
    /**
     * Process mouse and generate events.
     * Useful for event-driven architectures.
     */
    public fun processEvents(handler: (MouseEvent) -> Unit) {
        if (!poll()) return
        
        val x = getX()
        val y = getY()
        val buttons = getButtons()
        val wheel = getWheel()
        
        // Check for button changes
        val changed = buttons xor lastButtons
        if (changed != 0) {
            for (btn in listOf(BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)) {
                if ((changed and btn) != 0) {
                    val type = if ((buttons and btn) != 0) {
                        MouseEvent.EventType.BUTTON_DOWN
                    } else {
                        MouseEvent.EventType.BUTTON_UP
                    }
                    handler(MouseEvent(x, y, buttons, 0, type))
                }
            }
        }
        
        // Check for wheel
        if (wheel != 0) {
            handler(MouseEvent(x, y, buttons, wheel, MouseEvent.EventType.WHEEL))
        }
        
        // Always report movement
        handler(MouseEvent(x, y, buttons, 0, MouseEvent.EventType.MOVE))
        
        lastButtons = buttons
    }
}
