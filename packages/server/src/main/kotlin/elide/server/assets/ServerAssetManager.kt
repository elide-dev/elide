package elide.server.assets

import com.google.common.util.concurrent.Futures
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import elide.server.cfg.AssetConfig
import elide.util.Base64
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpHeaders
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
 * @param assetConfig Server-side asset configuration.
 * @param assetIndex Active asset index for this server run.
 * @param reader Active asset reader implementation for this server run.
 */
@Suppress("UnstableApiUsage")
@Context
public class ServerAssetManager @Inject internal constructor(
  private val assetConfig: AssetConfig,
  private val assetIndex: ServerAssetIndex,
  override val reader: AssetReader,
) : AssetManager {
  public companion object {
    // Wait timeout in seconds for initialization.
    public const val waitTimeout: Long = 10L
  }

  /** @inheritDoc */
  override val logging: Logger = LoggerFactory.getLogger(AssetManager::class.java)

  // Check if a request has conditional headers.
  private fun requestIsConditional(request: HttpRequest<*>): Boolean {
    return request.headers.contains(HttpHeaders.IF_NONE_MATCH) || request.headers.contains(HttpHeaders.IF_MATCH)
  }

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
    logging.debug(
      "Serving asset with module ID '${asset.module}'"
    )

    // before serving the asset, check if the request is conditional. if it has an ETag specified that matches, or a
    // last-modified time that matches, we can skip serving it and serve a 304.
    if (assetConfig.etags && requestIsConditional(request)) {
      val etag = request.headers[HttpHeaders.IF_NONE_MATCH]
      if (etag != null && etag.isNotEmpty()) {
        if (etag.startsWith("W/")) {
          // match against the manifest timestamp. if the two match, we're able to satisfy this without sending the
          // asset, via a weak ETag.
          val generatedTime = assetIndex.getBundleTimestamp()
          val etagTime = etag.substring(2).removeSurrounding("\"")
          if (etagTime == generatedTime.toString()) {
            // we have a match against a weak ETag.
            return Futures.immediateFuture(
              HttpResponse.notModified<StreamedAsset>()
            ).asDeferred()
          }
        } else {
          require(asset.index != null && asset.index.size == 1) {
            "Asset must be inlined in asset bundle, and cannot have more than one source file specified." +
            " Please check that each of your asset bundles are specified with a maximum of 1 source file."
          }

          // it's a strong etag, so we need to compare it with the Base64-encoded asset hash.
          val content = assetIndex.readByModuleIndex(asset.index.first())
          val identityVariant = content.getVariant(0)
          val b64 = String(
            Base64.encodeWebSafe(identityVariant.getIntegrity(0).toByteArray()),
            StandardCharsets.UTF_8
          )
          if (etag == b64) {
            // we have a match against a strong ETag.
            return Futures.immediateFuture(
              HttpResponse.notModified<StreamedAsset>()
            ).asDeferred()
          }
        }
      }
    }
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
