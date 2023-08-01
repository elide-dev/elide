package elide.model.token

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import elide.annotations.data.Sensitive

/** Wraps a secure token value in an inline class on each platform. */
@Sensitive @Serializable public actual data class TokenValue constructor (
  /** Sensitive inner value for this token. */
  @Contextual public actual val value: String,
)
