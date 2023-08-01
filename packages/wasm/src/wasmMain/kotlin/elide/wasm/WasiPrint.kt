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

import elide.wasm

import elide.wasm.internal.fdWrite

interface WasiPrint {
  /**
   * Prints the given [message] to the standard output.
   */
  fun print(message: Any)

  /**
   * Prints the given [message] and the line separator to the standard output.
   */
  fun println(message: Any)
}

object OutputWasiPrint: WasiPrint {
  override fun print(message: Any) {
    fdWrite(StandardDescriptor.STDOUT, listOf(message.toString().encodeToByteArray()))
  }

  override fun println(message: Any) {
    print(message.toString() + "\n")
  }
}

object ErrorWasiPrint: WasiPrint {
  override fun print(message: Any) {
    fdWrite(StandardDescriptor.STDERR, listOf(message.toString().encodeToByteArray()))
  }

  override fun println(message: Any) {
    print(message.toString() + "\n")
  }
}
