package elide.model.token

import elide.annotations.data.Sensitive
import kotlinx.serialization.Serializable

/** Describes a sensitive token value. */
@Serializable public actual data class Token constructor (
  /** Type of token. */
  public actual val type: TokenType,

  /** Inner token value. */
  @Sensitive public actual val value: TokenValue,
)
