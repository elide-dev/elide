package elide.model.token

import kotlinx.serialization.Serializable
import elide.annotations.data.Sensitive

/**
 * # Models: Token
 *
 */
@Serializable public expect class Token {
  /** Specifies the type of token. */
  public val type: TokenType

  /** Specifies the value held by this token payload. */
  @Sensitive public val value: TokenValue
}
