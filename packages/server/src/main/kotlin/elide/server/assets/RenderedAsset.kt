package elide.server.assets

import io.micronaut.http.MediaType

/**
 *
 */
public data class RenderedAsset(
  val module: String,
  val type: MediaType,
)
