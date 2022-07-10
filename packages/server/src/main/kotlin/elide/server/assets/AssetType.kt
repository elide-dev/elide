package elide.server.assets

import io.micronaut.http.MediaType

/**
 * Enumerates known kinds of registered application assets.
 *
 * @param mediaType Micronaut media type associated with this asset type.
 */
public enum class AssetType constructor(internal val mediaType: MediaType) {
  /** Generic assets which employ custom configuration. */
  GENERIC(mediaType = MediaType("application/octet-stream")),

  /** Plain text assets. */
  TEXT(mediaType = MediaType("text/plain", "txt")),

  /** JavaScript assets. */
  SCRIPT(mediaType = MediaType("application/javascript", "js")),

  /** Stylesheet assets. */
  STYLESHEET(mediaType = MediaType("text/css", "css")),
}
