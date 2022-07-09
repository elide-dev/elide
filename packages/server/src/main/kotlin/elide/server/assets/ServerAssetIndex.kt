package elide.server.assets

import io.micronaut.context.annotation.Context
import jakarta.inject.Inject

/**
 *
 */
@Context internal class ServerAssetIndex {
  @Inject internal lateinit var manifestProvider: ServerAssetManifestProvider
}
