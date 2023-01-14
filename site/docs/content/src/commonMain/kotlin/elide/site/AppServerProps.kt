package elide.site

import kotlinx.serialization.Serializable

/** Top-level application properties. */
@Serializable data class AppServerProps(
  val page: String,
)
