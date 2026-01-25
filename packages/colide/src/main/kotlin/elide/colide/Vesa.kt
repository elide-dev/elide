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
 * # VESA Framebuffer
 *
 * Direct framebuffer access for bare metal graphics output.
 *
 * Colors are 32-bit ARGB format: 0xAARRGGBB
 *
 * ## Example
 * ```kotlin
 * Vesa.clear(0xFF000000)  // Black with full alpha
 * Vesa.fillRect(10, 10, 100, 50, 0xFF0000FF)  // Blue rectangle
 * Vesa.putPixel(50, 50, 0xFFFFFFFF)  // White pixel
 * ```
 */
public object Vesa {
    /**
     * Put a single pixel at (x, y).
     * @param x X coordinate
     * @param y Y coordinate
     * @param color 32-bit ARGB color
     */
    @JvmStatic
    public external fun putPixel(x: Int, y: Int, color: Int)

    /**
     * Fill a rectangle with a solid color.
     * @param x Left edge
     * @param y Top edge
     * @param w Width
     * @param h Height
     * @param color 32-bit ARGB color
     */
    @JvmStatic
    public external fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int)

    /**
     * Clear the entire screen with a color.
     * @param color 32-bit ARGB color
     */
    @JvmStatic
    public external fun clear(color: Int)

    /**
     * Draw text at (x, y) - convenience wrapper.
     * Note: Requires font rendering implementation.
     */
    public fun drawText(x: Int, y: Int, text: String, fg: Int, bg: Int = 0) {
        // Simple 8x8 bitmap font rendering
        // For now, delegate to C driver which has the font
        // TODO: Implement font rendering in Kotlin
    }

    /** Screen width in pixels */
    public val width: Int get() = ColideNative.screenWidth()

    /** Screen height in pixels */
    public val height: Int get() = ColideNative.screenHeight()
}
