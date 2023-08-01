package elide.model.token

import kotlinx.serialization.Serializable
import elide.annotations.data.Sensitive

/** Describes a sensitive token value. */
@Serializable public actual data class Token(
  /** Type of token. */
  public actual val type: TokenType,

  /** Inner token value. */
  @Sensitive public actual val value: TokenValue,
)
