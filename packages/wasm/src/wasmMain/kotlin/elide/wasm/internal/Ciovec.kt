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

internal data class UnsafeCiovec(
    /** The address of the buffer to be written. */
    var buffer: Pointer,
    /** The length of the buffer to be written. */
    var bufferLength: Size,
)

internal fun storeUnsafeCiovec(ciovec: UnsafeCiovec, pointer: Pointer) {
    (pointer + 0).storeInt(ciovec.buffer.address.toInt())
    (pointer + 4).storeInt(ciovec.bufferLength)
}

internal typealias UnsafeCiovecArray = List<UnsafeCiovec>
