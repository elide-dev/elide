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

import kotlin.random.Random

/**
 * Provides access to WASI APIs.
 */
object Wasi : WasiFileSystem by DefaultWasiFilesystem, WasiCli by DefaultWasiCli {

    /** The standard output. */
    val out : WasiPrint = OutputWasiPrint

    /** The standard error. */
    val err : WasiPrint = ErrorWasiPrint

    /** Default monotonic clock, suitable for general-purpose application needs. */
    val monotonicClock: MonotonicClock = DefaultMonotonicClock

    /**
     * Default wall clock, suitable for general-purpose application needs.
     */
    val wallClock: WallClock = DefaultWallClock

    /**
     * Provide a pseudo random generator seeded by a `Long` value generated via WASI.
     */
    fun seededRandom(): Random = SeededWasiRandom()

    /**
     * Provide a secure random generator implemented via WASI.
     */
    fun secureRandom(): Random = SecureWasiRandom()
}
