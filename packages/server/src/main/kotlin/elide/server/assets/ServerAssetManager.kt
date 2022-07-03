package elide.server.assets

import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.runtime.AppExecutor
import io.micronaut.caffeine.cache.Cache
import io.micronaut.caffeine.cache.Caffeine
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpResponse
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Built-in asset manager implementation for use with Elide applications.
 *
 * Resolves and loads assets embedded in the application at build-time, based on binary-encoded protocol buffer messages
 * which define the dependency structure and metadata of each embedded asset.
 *
 * @param exec Application executor that should be used for asset-related background work.
 * @param reader Active asset reader implementation for this server run.
 * @param resolver Active asset resolver implementation for this server run.
 */
@Context @Singleton public class ServerAssetManager @Inject constructor (
  private val exec: AppExecutor,
  override val reader: AssetReader,
  override val resolver: AssetResolver,
): AssetManager {
  public companion object {
    // Wait timeout in seconds for initialization.
    public const val waitTimeout: Long = 10L
  }

  // Whether the manager has started initializing yet.
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Whether the manager has finished initializing.
  private val ready: AtomicBoolean = AtomicBoolean(false)

  // Wait latch for asset consumers.
  private val latch: CountDownLatch = CountDownLatch(1)

  // Cache for rendered asset results.
  private val assetCache: Cache<ServerAsset, RenderedAsset> = Caffeine.newBuilder()
    .build()

  /** @inheritDoc */
  override val logging: Logger = Logging.of(AssetManager::class)

  @Synchronized private fun initialize() {
    if (initialized.compareAndSet(false, true)) {
      exec.service().run {
        // read the embedded asset bundle

        // build index of assets to module paths

        // build index based on dependency topology

        // allow asset serving now
        latch.countDown()
      }
    }
  }

  // Build an HTTP asset response from the provided asset result.
  private fun buildAssetResponse(asset: RenderedAsset): HttpResponse<ByteArray> {
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun renderAssetAsync(asset: ServerAsset): Deferred<HttpResponse<ByteArray>> {
    return withContext(Dispatchers.IO) {
      // if the asset system is still initializing, wait for it to complete
      if (!ready.get()) {
        initialize()
        latch.await(
          waitTimeout,
          java.util.concurrent.TimeUnit.SECONDS
        )
      }

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
