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
import kotlin.wasm.unsafe.Pointer
import kotlin.wasm.unsafe.withScopedMemoryAllocator
import elide.wasm.WasiException

/**
 * Read command-line argument data.
 */
internal fun argsGet(): List<String> {
    val (numArgs, bufSize) = argsSizesGet()
    val byteArrayBuf = ByteArray(bufSize)
    withScopedMemoryAllocator { allocator ->
        val argv = allocator.allocate(numArgs * 4)
        val argvBuffer = allocator.allocate(bufSize)
        val ret = wasmArgsGet(argv.address.toInt(), argvBuffer.address.toInt())
        if (ret != 0) {
            throw WasiException(Errno.values()[ret].ordinal)
        }
        val result = mutableListOf<String>()
        repeat(numArgs) { idx ->
            val argvPtr = argv + idx * 4
            var ptr = Pointer(argvPtr.loadInt().toUInt())
            val endIndex = readZeroTerminatedByteArray(ptr, byteArrayBuf)
            val str = byteArrayBuf.decodeToString(endIndex = endIndex)
            result += str
        }
        return result
    }
}

internal fun readZeroTerminatedByteArray(ptr: Pointer, byteArray: ByteArray): Int {
    for (i in byteArray.indices) {
        val b = (ptr + i).loadByte()
        if (b.toInt() == 0)
            return i
        byteArray[i] = b
    }
    error("Zero-terminated array is out of bounds")
}

/** Return command-line argument data sizes. */
private fun argsSizesGet(): Pair<Size, Size> {
    withScopedMemoryAllocator { allocator ->
        val rp0 = allocator.allocate(4)
        val rp1 = allocator.allocate(4)
        val ret = wasmArgsSizesGet(rp0.address.toInt(), rp1.address.toInt())
        return if (ret == 0) {
            Pair(
                (Pointer(rp0.address.toInt().toUInt())).loadInt(),
                (Pointer(rp1.address.toInt().toUInt())).loadInt(),
            )
        } else {
            throw WasiException(Errno.values()[ret].ordinal)
        }
    }
}

@WasmImport("wasi_snapshot_preview1", "args_get")
private external fun wasmArgsGet(
    arg0: Int,
    arg1: Int,
): Int

@WasmImport("wasi_snapshot_preview1", "args_sizes_get")
private external fun wasmArgsSizesGet(
    arg0: Int,
    arg1: Int,
): Int
