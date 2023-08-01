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

import elide.wasm.internal.randomGetInt
import elide.wasm.internal.randomGetLong
import kotlin.random.Random

/**
 * Provide a pseudo random generator seeded by a `Long` value generated via WASI.
 */
fun SeededWasiRandom() = Random(randomGetLong())

/**
 * Provide a secure random generator implemented via WASI.
 */
class SecureWasiRandom : Random() {
  override fun nextBits(bitCount: Int) =
    randomGetInt().takeUpperBits(bitCount)

  override fun nextInt() =
    randomGetInt()

  override fun nextLong() =
    randomGetLong()

  private fun Int.takeUpperBits(bitCount: Int): Int =
    this.ushr(32 - bitCount) and (-bitCount).shr(31)
}
