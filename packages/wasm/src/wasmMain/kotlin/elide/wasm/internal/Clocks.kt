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

import elide.wasm.*
import kotlin.wasm.unsafe.*
import kotlin.wasm.WasmImport

internal enum class ClockId {
    /** The clock measuring real time. Time value zero corresponds with 1970-01-01T00:00:00Z. */
    REALTIME,
    /**
     * The store-wide monotonic clock, which is defined as a clock measuring real time, whose value
     * cannot be adjusted and which cannot have negative clock jumps. The epoch of this clock is
     * undefined. The absolute time value of this clock therefore has no meaning.
     */
    MONOTONIC,
    /** The CPU-time clock associated with the current process. */
    PROCESS_CPUTIME_ID,
    /** The CPU-time clock associated with the current thread. */
    THREAD_CPUTIME_ID,
}

internal fun clockResGet(id: ClockId): Timestamp {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(8)
        val returnCode = rawClockResGet(id.ordinal, pointer.address.toInt())
        return if (returnCode == 0) {
            (Pointer(pointer.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiException(Errno.values()[returnCode].ordinal)
        }
    }
}

internal fun clockTimeGet(id: ClockId, precision: Timestamp): Timestamp {
    withScopedMemoryAllocator { allocator ->
        val pointer = allocator.allocate(8)
        val returnCode = rawWasmClockTimeGet(id.ordinal, precision, pointer.address.toInt())
        return if (returnCode == 0) {
            (Pointer(pointer.address.toInt().toUInt())).loadLong()
        } else {
            throw WasiException(Errno.values()[returnCode].ordinal)
        }
    }
}

/**
 * Return the resolution of a clock. Implementations are required to provide a non-zero value for
 * supported clocks. For unsupported clocks, return `errno::inval`. Note: This is similar to
 * `clock_getres` in POSIX.
 */
@WasmImport("wasi_snapshot_preview1", "clock_res_get")
private external fun rawClockResGet(
    arg0: Int,
    arg1: Int,
): Int

/** Return the time value of a clock. Note: This is similar to `clock_gettime` in POSIX. */
@WasmImport("wasi_snapshot_preview1", "clock_time_get")
private external fun rawWasmClockTimeGet(
    arg0: Int,
    arg1: Long,
    arg2: Int,
): Int
