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
 * # PS/2 Keyboard Driver
 *
 * Direct keyboard input for bare metal via Intel 8042 controller.
 *
 * ## Example
 * ```kotlin
 * // Blocking read
 * val key = Keyboard.getChar()
 *
 * // Non-blocking check
 * if (Keyboard.available()) {
 *     val key = Keyboard.getChar()
 * }
 *
 * // Check modifiers
 * if (Keyboard.isShiftPressed()) {
 *     // Handle shift
 * }
 * ```
 */
public object Keyboard {
    /** Modifier key bit flags */
    public const val MOD_SHIFT: Int = 0x01
    public const val MOD_CTRL: Int = 0x02
    public const val MOD_ALT: Int = 0x04

    /**
     * Get a character from keyboard (blocking).
     * @return ASCII character code, or -1 if no input
     */
    @JvmStatic
    public external fun getChar(): Int

    /**
     * Check if keyboard input is available (non-blocking).
     * @return true if a key is ready to be read
     */
    @JvmStatic
    public external fun available(): Boolean

    /**
     * Get modifier key state.
     * @return Bitmask of MOD_SHIFT, MOD_CTRL, MOD_ALT
     */
    @JvmStatic
    public external fun getModifiers(): Int

    /** Check if Shift is pressed */
    public fun isShiftPressed(): Boolean = (getModifiers() and MOD_SHIFT) != 0

    /** Check if Ctrl is pressed */
    public fun isCtrlPressed(): Boolean = (getModifiers() and MOD_CTRL) != 0

    /** Check if Alt is pressed */
    public fun isAltPressed(): Boolean = (getModifiers() and MOD_ALT) != 0

    /**
     * Read a line of text (blocking, with echo).
     * @param maxLen Maximum characters to read
     * @return The line entered by the user
     */
    public fun readLine(maxLen: Int = 256): String {
        val sb = StringBuilder()
        while (sb.length < maxLen) {
            val ch = getChar()
            when {
                ch == 13 || ch == 10 -> break  // Enter
                ch == 8 || ch == 127 -> {  // Backspace
                    if (sb.isNotEmpty()) sb.deleteCharAt(sb.length - 1)
                }
                ch in 32..126 -> sb.append(ch.toChar())  // Printable
            }
        }
        return sb.toString()
    }
}
