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

package elide.core.encoding.hex

import elide.core.encoding.Codec
import elide.core.encoding.Encoding

/**
 * # Hex
 *
 * Provides cross-platform utilities for encoding values into hex, or decoding values from hex. Available on any target
 * platform supported by Elide/Kotlin, including native platforms.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") public expect object Hex : Codec<HexData> {
    override fun encoding(): Encoding

    override fun encode(data: ByteArray): HexData

    override fun decodeBytes(data: ByteArray): ByteArray
}
