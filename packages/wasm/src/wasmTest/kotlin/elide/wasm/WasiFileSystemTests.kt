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

class WasiFileSystemTests {
    @Test fun testCreateDirectory() {
        Wasi.createDirectoryAt("testDir")
    }

    @Test fun testCreateWriteReadFile() {
        val descriptor = Wasi.openAt("testFile", OpenFlags(create = true), DescriptorFlags(read = true, write = true))
        Wasi.write(descriptor, "Hello".encodeToByteArray())
        Wasi.write(descriptor, ", world!".encodeToByteArray(), 5u)

        var result = Wasi.read(descriptor, 5u)
        assertEquals("Hello", result.data.decodeToString())
        assertFalse(result.eof)

        result = Wasi.read(descriptor, 13u)
        assertEquals("Hello, world!", result.data.decodeToString())
        assertFalse(result.eof)

        result = Wasi.read(descriptor, 14u)
        assertEquals("Hello, world!", result.data.decodeToString())
        assertTrue(result.eof)

        result = Wasi.read(descriptor, 8u, 5u)
        assertEquals(", world!", result.data.decodeToString())
        assertFalse(result.eof)
    }

    @Test fun testListDirectoryEntries() {
        Wasi.readDirectory(".").forEach(::println)
    }
}
