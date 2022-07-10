package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.common.util.concurrent.Futures
import elide.server.cfg.AssetConfig
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpRequest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.guava.asDeferred
import tools.elide.assets.AssetBundle.AssetContent
import tools.elide.data.CompressedData
import tools.elide.data.CompressionMode

/**
 * Default implementation of an [AssetReader]; used in concert with the default [AssetManager] to fulfill HTTP requests
 * for static assets embedded within the application.
 */
@Context @Singleton
public class ServerAssetReader : AssetReader {
  // Server-side asset configuration.
  @Inject internal lateinit var assetConfig: AssetConfig

  // Live index of asset data.
  @Inject internal lateinit var assetIndex: ServerAssetIndex

  @VisibleForTesting
  internal fun baselineHeaders(asset: ServerAsset, variant: CompressedData): Map<String, String> {
    return emptyMap()
  }

  @VisibleForTesting
  internal fun selectBestVariant(
    asset: ServerAsset,
    content: AssetContent,
    request: HttpRequest<*>
  ): Pair<Map<String, String>, CompressedData> {
    // @TODO(sgammon) select variant based on request
    val variant = content.getVariant(0)
    return baselineHeaders(asset, variant) to variant
  }

  /** @inheritDoc */
  override suspend fun readAsync(descriptor: ServerAsset, request: HttpRequest<*>?): Deferred<RenderedAsset> {
    val module = descriptor.module
    require(descriptor.index != null) {
      "Asset index required to serve local asset"
    }
    val content = assetIndex.readByModuleIndex(
      descriptor.index
    )
    val (headers, selectedVariant) = if (request != null) {
      // select the best content variant to use based on the input request, which may specify supported compression
      // schemes, or may be expressing if-not-modified or if-modified-since conditions.
      selectBestVariant(
        descriptor,
        content,
        request,
      )
    } else {
      // because we don't have a request, we should always serve the `IDENTITY` variant, which doesn't implement any
      // compression at all.
      val identityData = content.getVariant(0)
      require(identityData.compression == CompressionMode.IDENTITY) {
        "First variant stanza for an asset should always be `IDENTITY` (module: '$module')"
      }
      baselineHeaders(descriptor, identityData) to identityData
    }
    return Futures.immediateFuture(
      RenderedAsset(
        module = module,
        type = descriptor.assetType,
        variant = selectedVariant.compression,
        headers = headers,
        size = selectedVariant.size,
        lastModified = assetIndex.getBundleTimestamp(),
        digest = if (selectedVariant.integrityCount > 0) {
          val subj = selectedVariant.getIntegrity(0)
          subj.hash to subj.fingerprint
        } else {
          null
        }
      ) { selectedVariant.data.raw }
    ).asDeferred()
  }

  /** @inheritDoc */
  override fun resolve(path: String): ServerAsset? {
    val unprefixed = if (path.startsWith(assetConfig.prefix)) {
      // if the asset is prefixed, trim it first
      path.substring(assetConfig.prefix.length + 1)
    } else {
      path
    }
    val unextensioned = if (unprefixed.contains(".")) {
      unprefixed.dropLast(unprefixed.length - unprefixed.lastIndexOf("."))
    } else {
      unprefixed
    }
    return assetIndex.resolveByTag(
      unextensioned
    )
  }
}
