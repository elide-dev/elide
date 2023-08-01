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

import kotlin.test.*

class WasiClocksTests {
    @Test fun testWallClockNow() {
        val dateTime = Wasi.wallClock.now()
        assertTrue(dateTime.seconds > (2023 - 1970) * 365 * 24 * 60 * 60)
    }

    @Test fun testWallClockResolution() {
        val dateTime = Wasi.wallClock.resolution()
        assertEquals(0L, dateTime.seconds)
        assertTrue(dateTime.nanoseconds > 0)
    }

    @Test fun testMonotonicClockNow() {
        val instant1 = Wasi.monotonicClock.now()
        val instant2 = Wasi.monotonicClock.now()
        assertTrue(instant2 >= instant1)
    }

    @Test fun testMonotonicClockResolution() {
        val instant = Wasi.monotonicClock.resolution()
        assertTrue(instant > 0)
    }
}
