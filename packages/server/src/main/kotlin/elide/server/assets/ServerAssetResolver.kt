package elide.server.assets

import io.micronaut.context.annotation.Context
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 *
 */
@Context @Singleton
public class ServerAssetResolver : AssetResolver {
  @Inject internal lateinit var assetIndex: ServerAssetIndex

  /** @inheritDoc */
  override fun resolve(path: String): ServerAsset? {
    TODO("Not yet implemented")
  }
}
