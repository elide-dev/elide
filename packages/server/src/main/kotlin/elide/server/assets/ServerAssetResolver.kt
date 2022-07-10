package elide.server.assets

import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Default implementation of a server-side asset resolver, responsible for resolving an asset path, typically from an
 * HTTP request, to an asset embedded within the application.
 *
 * If the asset is found, it is typically then loaded and served by an [AssetReader] / [AssetManager], which is used in
 * concert with this object.
 */
@Context @Singleton
public class ServerAssetResolver : AssetResolver {
  @Inject internal lateinit var assetIndex: ServerAssetIndex

  /** @inheritDoc */
  override fun resolve(path: String): ServerAsset? {
    TODO("Not yet implemented")
  }
}
