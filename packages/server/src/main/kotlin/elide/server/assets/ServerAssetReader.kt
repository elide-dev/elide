package elide.server.assets

import kotlinx.coroutines.Deferred

/**
 *
 */
public class ServerAssetReader: AssetReader {
  /** @inheritDoc */
  override suspend fun readAsync(descriptor: ServerAsset): Deferred<RenderedAsset> {
    TODO("Not yet implemented")
  }
}
