package elide.model

import elide.model.token.Token
import elide.model.token.TokenType
import elide.model.token.TokenValue
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

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
