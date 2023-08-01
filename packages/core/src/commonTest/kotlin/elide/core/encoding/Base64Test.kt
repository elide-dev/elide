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
import kotlin.test.assertNotNull
import elide.core.encoding.base64.DefaultBase64

/** Tests for built in [DefaultBase64] tools. */
class Base64Test : AbstractEncoderTest<DefaultBase64>() {
  override fun encoding(): Encoding = Encoding.BASE64
  override fun encoder(): DefaultBase64 = DefaultBase64

  @Test fun testEncodeBase64() {
    val input = "abc123123"
    assertEquals(
      "YWJjMTIzMTIz",
      DefaultBase64.encodeToString(input),
      "should get expected output from Base64 encode",
    )
    val inputPadded = "abc1231231"
    assertEquals(
      "YWJjMTIzMTIzMQ==",
      DefaultBase64.encodeToString(inputPadded),
      "should get expected output from non-websafe Base64 encode",
    )
    assertEquals(
      "YWJjMTIzMTIzMQ",
      DefaultBase64.encodeWebSafe(inputPadded),
      "should get expected output from websafe Base64 encode",
    )
  }

  @Test fun testEncodeBase64Extension() {
    val input = "abc123123"
    assertEquals(
      "YWJjMTIzMTIz",
      input.toBase64String(),
      "should get expected output from Base64 encode",
    )
  }

  @Test fun testDecodeBase64() {
    val input = "YWJjMTIzMTIz"
    assertEquals(
      "abc123123",
      DefaultBase64.decodeToString(input),
      "should get expected output from Base64 decode",
    )
  }

  @Test fun testEncodeDecodeBase64() {
    val subjects = listOf(
      "kljbhgyuiojklmnjbhgvyhuioj;klmnbhgjvygouioj;klm",
      "here is a sample bla bla",
    )
    subjects.forEach { sample ->
      val encoded = DefaultBase64.encodeToString(
        sample,
      )
      assertNotNull(
        encoded,
        "should get non-null output from base64 encoder",
      )
      val decoded = DefaultBase64.decodeToString(
        encoded,
      )
      assertEquals(
        sample,
        decoded,
        "sample should decode properly from encoded base64 string",
      )
    }
  }
}
