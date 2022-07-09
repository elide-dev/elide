package elide.server.assets

import io.micronaut.http.MediaType

/** Enumerates known kinds of registered application assets. */
public enum class AssetType constructor(internal val mediaType: MediaType? = null) {
  /** Generic assets which employ custom configuration. */
  GENERIC,

  /** Plain text assets. */
  TEXT(mediaType = MediaType("text/plain", "txt")),

  /** JavaScript assets. */
  SCRIPT(mediaType = MediaType("application/javascript", "js")),

  /** Stylesheet assets. */
  STYLESHEET(mediaType = MediaType("text/css", "css")),
}
