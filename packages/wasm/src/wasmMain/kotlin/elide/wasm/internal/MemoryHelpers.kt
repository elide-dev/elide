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

import kotlin.wasm.unsafe.*

internal fun MemoryAllocator.writeToLinearMemory(array: ByteArray): Pointer {
    val pointer = allocate(array.size)
    var currentPointer = pointer
    array.forEach {
        currentPointer.storeByte(it)
        currentPointer += 1
    }
    return pointer
}

internal fun MemoryAllocator.writeToLinearMemory(array: UnsafeCiovecArray): Pointer {
    val pointer = allocate(array.size * 8)
    var currentPointer = pointer
    array.forEach {
        storeUnsafeCiovec(it, currentPointer)
        currentPointer += 8
    }
    return pointer
}

internal fun MemoryAllocator.writeToLinearMemory(value: String): Pointer =
    writeToLinearMemory(value.encodeToByteArray())

internal fun loadByteArray(addr: Pointer, size: Int): ByteArray =
    ByteArray(size) { i -> (addr + i).loadByte() }

internal fun loadString(addr: Pointer, size: Int): String {
    val bytes = loadByteArray(addr, size)
    val endIndex =
        if (size != 0 && bytes[size - 1] == 0.toByte())
            size - 1  // skip last 0 for 0-terminated strings
        else
            size

    return bytes.decodeToString(endIndex = endIndex)
}
