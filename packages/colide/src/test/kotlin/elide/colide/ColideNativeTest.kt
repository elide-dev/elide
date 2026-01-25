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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Tests for the Colide native JNI bridge.
 * 
 * These tests verify:
 * - Native library loading works
 * - JNI functions are callable
 * - Hosted mode returns expected defaults
 */
class ColideNativeTest {
    
    @Test
    fun `native library loads without error`() {
        // In hosted mode, this should not throw - just check it's accessible
        val available = ColideNative.isAvailable
        // Native may not be available in test env, but access should not throw
        println("Native available: $available")
    }
    
    @Test
    fun `isMetal returns false in hosted mode`() {
        assumeTrue(ColideNative.isAvailable, "Native library not available")
        assertFalse(ColideNative.isMetal(), "isMetal should return false in hosted mode")
    }
    
    @Test
    fun `init succeeds in hosted mode`() {
        assumeTrue(ColideNative.isAvailable, "Native library not available")
        val result = ColideNative.init()
        assertTrue(result, "init should succeed in hosted mode")
    }
    
    @Test
    fun `screen dimensions are valid in hosted mode`() {
        assumeTrue(ColideNative.isAvailable, "Native library not available")
        ColideNative.init()
        val width = ColideNative.screenWidth()
        val height = ColideNative.screenHeight()
        assertTrue(width >= 0, "Screen width should be non-negative")
        assertTrue(height >= 0, "Screen height should be non-negative")
    }
}
