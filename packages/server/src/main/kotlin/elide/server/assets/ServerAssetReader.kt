package elide.server.assets

import io.micronaut.context.annotation.Context
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred

/**
 *
 */
@Context @Singleton public class ServerAssetReader: AssetReader {
  /** @inheritDoc */
  override suspend fun readAsync(descriptor: ServerAsset): Deferred<RenderedAsset> {
    TODO("Not yet implemented")
  }
}
