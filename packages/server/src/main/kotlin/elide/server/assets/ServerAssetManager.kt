package elide.server.assets

import com.google.common.util.concurrent.Futures
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import elide.server.cfg.ServerConfig
import io.micronaut.caffeine.cache.Cache
import io.micronaut.caffeine.cache.Caffeine
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.asDeferred
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Built-in asset manager implementation for use with Elide applications.
 *
 * Resolves and loads assets embedded in the application at build-time, based on binary-encoded protocol buffer messages
 * which define the dependency structure and metadata of each embedded asset.
 *
 * @param reader Active asset reader implementation for this server run.
 */
@Suppress("UnstableApiUsage")
@Context
public class ServerAssetManager @Inject constructor(
  override val reader: AssetReader,
  private val config: ServerConfig,
) : AssetManager {
  public companion object {
    // Wait timeout in seconds for initialization.
    public const val waitTimeout: Long = 10L
  }

  // Cache for rendered asset results.
  private val assetCache: Cache<ServerAsset, RenderedAsset> = Caffeine.newBuilder()
    .build()

  /** @inheritDoc */
  override val logging: Logger = LoggerFactory.getLogger(AssetManager::class.java)

  // Build an HTTP asset response from the provided asset result.
  private fun buildAssetResponse(asset: RenderedAsset): StreamedAssetResponse {
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun renderAssetAsync(request: HttpRequest<*>, asset: ServerAsset): Deferred<StreamedAssetResponse> {
    // if asset serving is disabled, return a 404 for all asset calls.
    if (!config.assets.enabled) {
      return Futures.immediateFuture(
        HttpResponse.notFound<StreamedAsset>()
      ).asDeferred()
    }

    return withContext(Dispatchers.IO) {
      async {
        // pass off to the reader to read the asset
        val assetContent = reader.readAsync(asset)
        assetContent.invokeOnCompletion {
          if (it != null) {
            logging.error("Error reading asset: $asset", it)
          } else {
            assetCache.put(asset, assetContent.getCompleted())
          }
        }
        buildAssetResponse(
          assetContent.await()
        )
      }
    }
  }
}
