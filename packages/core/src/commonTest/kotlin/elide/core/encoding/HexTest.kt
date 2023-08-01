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

package elide.core.encoding

import kotlin.test.Test
import kotlin.test.assertEquals
import elide.core.encoding.hex.DefaultHex

/** Tests for built-in [DefaultHex] encoding tools. */
class HexTest : AbstractEncoderTest<DefaultHex>() {
  override fun encoding(): Encoding = Encoding.HEX
  override fun encoder(): DefaultHex = DefaultHex

  @Test fun testEncodeHexFromByteArray() {
    val value = "abc123123"
    val hex = DefaultHex.bytesToHex(value.encodeToByteArray())
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromString() {
    val value = "abc123123"
    val hex = DefaultHex.encodeToString(value)
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromByteArrayExtension() {
    val value = "abc123123".encodeToByteArray()
    val hex = value.toHexString()
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
  }

  @Test fun testEncodeHexFromStringExtension() {
    val value = "abc123123"
    val hex = value.toHexString()
    assertEquals(
      "616263313233313233",
      hex,
      "hex encoding a basic value should produce expected result"
    )
    val hex2 = value.toHex().string
    assertEquals(
      "616263313233313233",
      hex2,
      "hex encoding a basic value should produce expected result"
    )
  }
}
