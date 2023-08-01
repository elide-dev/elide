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

package elide.wasm.internal

import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import elide.wasm.WasiException

internal fun randomGetInt(): Int {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(Int.SIZE_BYTES)
        val returnCode = rawRandomGet(pointer.address.toInt(), Int.SIZE_BYTES)
        return if (returnCode == 0) {
            pointer.loadInt()
        } else {
            throw WasiException(Errno.values()[returnCode].ordinal)
        }
    }
}

internal fun randomGetLong(): Long {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(Long.SIZE_BYTES)
        val returnCode = rawRandomGet(pointer.address.toInt(), Long.SIZE_BYTES)
        return if (returnCode == 0) {
            pointer.loadLong()
        } else {
            throw WasiException(Errno.values()[returnCode].ordinal)
        }
    }
}

/**
 * Write high-quality random data into a buffer. This function blocks when the implementation is
 * unable to immediately provide sufficient high-quality random data. This function may execute
 * slowly, so when large mounts of random data are required, it's advisable to use this function to
 * seed a pseudo-random number generator, rather than to provide the random data directly.
 */
@WasmImport("wasi_snapshot_preview1", "random_get")
private external fun rawRandomGet(
    arg0: Int,
    arg1: Int,
): Int
