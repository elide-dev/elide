package elide.model.token

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * ## Tokens: Token Value
 *
 * Holds a raw sensitive token value, usually wrapped in a [Token] payload; the string representation of this object is
 * never printed literally, but is instead masked.
 */
@Serializable public expect class TokenValue {
  @Contextual public val value: String
}
