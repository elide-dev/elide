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

package elide.model

import kotlin.test.Test
import kotlin.test.assertEquals
import elide.model.token.Token
import elide.model.token.TokenType
import elide.model.token.TokenValue

/** Tests for redaction during model formatting, using the Redakt plugin. */
class ModelRedactionTest {
  data class SampleNonSensitive(
    val a: Int,
    val b: String,
  )

  /** Formatting a data class should produce a sensible string representation. */
  @Test fun testFormatNonSensitive() {
    val sample = SampleNonSensitive(1, "hello")
    val formatted = sample.toString()
    assertEquals(
      "SampleNonSensitive(a=1, b=hello)",
      formatted,
      "Expected formatted string to match expected value",
    )
  }

  /** Formatting a data class with a redacted (sensitive) property should redact it in the string representation. */
  @Test fun testFormatSensitiveProperty() {
    val sample = Token(TokenType.JWT, TokenValue("testing"))
    val formatted = sample.toString()
    assertEquals(
      "Token(type=JWT, value=●●●●)",
      formatted,
      "Expected formatted string to be property-redacted",
    )
  }

  /** Formatting a data class which is wholly sensitive should result in complete redaction. */
  @Test fun testFormatSensitiveClass() {
    val sample = TokenValue("hello")
    val formatted = sample.toString()
    assertEquals(
      "TokenValue(●●●●)",
      formatted,
      "Expected formatted string to be wholly redacted",
    )
  }
}
