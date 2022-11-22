package elide.model.token

import kotlinx.serialization.Serializable

/** Specifies supported types of tokens. */
@Serializable public enum class TokenType {
  /** JSON Web Token. */
  JWT,

  /** OAuth2 code. */
  OAUTH2_CODE,

  /** OAuth2 opaque access token. */
  OAUTH2_ACCESS_TOKEN,

  /** OAuth2 refresh token. */
  OAUTH2_REFRESH_TOKEN,

  /** Custom token type. */
  CUSTOM,
}
