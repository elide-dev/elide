package elide.server.assets

import com.google.common.util.concurrent.Futures
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import elide.server.cfg.ServerConfig
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.server.netty.types.files.NettyStreamedFileCustomizableResponseType
import jakarta.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.asDeferred
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

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

  /** @inheritDoc */
  override val logging: Logger = LoggerFactory.getLogger(AssetManager::class.java)

  // Build an HTTP asset response from the provided asset result.
  private fun buildAssetResponse(asset: RenderedAsset): StreamedAssetResponse {
    val responseData = NettyStreamedFileCustomizableResponseType(
      ByteArrayInputStream(asset.producer.invoke().toByteArray()),
      asset.type.mediaType
    )
    val response = HttpResponse.ok(
      responseData
    ).characterEncoding(
      StandardCharsets.UTF_8
    ).contentType(
      asset.type.mediaType
    ).contentLength(
      asset.size
    )
    asset.headers.entries.forEach {
      val (header, value) = it
      response.header(header, value)
    }
    return response
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
    logging.debug(
      "Serving asset with module ID '${asset.module}'"
    )
    return withContext(Dispatchers.IO) {
      async {
        // pass off to the reader to read the asset
        buildAssetResponse(
          reader.readAsync(
            asset,
            request = request,
          ).await()
        )
      }
    }
  }
}
