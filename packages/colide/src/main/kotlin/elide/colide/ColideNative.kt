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

import elide.runtime.core.lib.NativeLibraries

/**
 * # Colide Native Bridge
 *
 * Main entry point for Colide OS native drivers. Provides detection of bare metal
 * environment and initialization of VESA, keyboard, and AI subsystems.
 *
 * ## Usage from Kotlin/JS
 * ```kotlin
 * if (ColideNative.isMetal()) {
 *     ColideNative.init()
 *     Vesa.clear(0x000000)  // Black screen
 *     val key = Keyboard.getChar()  // Wait for input
 * }
 * ```
 */
public object ColideNative {
    private var initialized = false

    init {
        NativeLibraries.resolve("colide") { loaded ->
            if (!loaded) {
                System.err.println("Warning: Colide native library not available")
            }
        }
    }

    /**
     * Initialize all Colide native drivers.
     * @return true if initialization succeeded
     */
    @JvmStatic
    public external fun init(): Boolean

    /**
     * Check if running on bare metal (Cosmopolitan).
     * @return true if running on bare metal, false if hosted
     */
    @JvmStatic
    public external fun isMetal(): Boolean

    /**
     * Get screen width in pixels.
     */
    @JvmStatic
    public external fun screenWidth(): Int

    /**
     * Get screen height in pixels.
     */
    @JvmStatic
    public external fun screenHeight(): Int

    /**
     * Ensure drivers are initialized before use.
     */
    public fun ensureInitialized(): Boolean {
        if (!initialized) {
            initialized = init()
        }
        return initialized
    }
}
