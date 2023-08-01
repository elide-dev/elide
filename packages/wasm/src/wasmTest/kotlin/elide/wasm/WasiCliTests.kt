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

class WasiCliTests {
    @Test fun testArgs() {
        assertEquals(listOf("argument1", "argument2"), Wasi.args)
    }

    @Test fun testEnvVars() {
        assertEquals(listOf(Pair("PATH", "/usr/local/bin:/usr/bin"), Pair("HOME", "/home/seb")), Wasi.envVars)
    }
}
