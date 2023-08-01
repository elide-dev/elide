/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.wasm

import kotlin.test.Test
import kotlin.test.assertTrue

class WasiRandomTests {
    @Test fun testSeededGenerator() {
        val generator = Wasi.seededRandom()
        assertTrue(generator.nextFloat() != generator.nextFloat())
        assertTrue(generator.nextDouble() != generator.nextDouble())
        assertTrue(generator.nextInt() != generator.nextInt())
        assertTrue(generator.nextLong() != generator.nextLong())
    }

    @Test fun testSecureGenerator() {
        val generator = Wasi.secureRandom()
        assertTrue(generator.nextFloat() != generator.nextFloat())
        assertTrue(generator.nextDouble() != generator.nextDouble())
        assertTrue(generator.nextInt() != generator.nextInt())
        assertTrue(generator.nextLong() != generator.nextLong())
        for (i in 0..Int.SIZE_BITS) {
            assertTrue(generator.nextBits(i).countLeadingZeroBits() >= (Int.SIZE_BITS - i))
        }
    }
}
