/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.server.assets

import com.google.common.util.concurrent.Futures
import io.micronaut.context.annotation.Context
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.server.netty.types.files.NettyStreamedFileCustomizableResponseType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import jakarta.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.withContext
import elide.server.AssetModuleId
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import elide.server.cfg.AssetConfig

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

  override fun linkForAsset(module: AssetModuleId, overrideType: AssetType?): String {
    val pointer = reader.pointerTo(module)
    require(pointer != null) {
      "Failed to generate link for asset: asset module '$module' not found"
    }
    val prefix = (assetConfig.prefix ?: AssetConfig.DEFAULT_ASSET_PREFIX).removeSuffix("/")
    val tag = pointer.tag
    val extension = when (overrideType ?: pointer.type) {
      AssetType.STYLESHEET -> ".css"
      AssetType.SCRIPT -> ".js"
      AssetType.TEXT -> ".txt"
      AssetType.GENERIC -> ""
    }
    return "$prefix/$tag$extension"
  }

  @Suppress("NestedBlockDepth", "ReturnCount")
  override suspend fun renderAssetAsync(request: HttpRequest<*>, asset: ServerAsset): Deferred<StreamedAssetResponse> {
    logging.debug(
      "Serving asset with module ID '${asset.module}'"
    )

    // before serving the asset, check if the request is conditional. if it has an ETag specified that matches, or a
    // last-modified time that matches, we can skip serving it and serve a 304.
    if ((assetConfig.etags ?: AssetConfig.DEFAULT_ENABLE_ETAGS) && requestIsConditional(request)) {
      val etag = request.headers[HttpHeaders.IF_NONE_MATCH]
      if (etag != null && etag.isNotEmpty()) {
        // fast path: the current server assigned the ETag.
        val module = assetIndex.activeManifest().moduleIndex[asset.module]
        if (module != null) {
          val reference = module.etag
          if (etag == reference) {
            // we have a match against a strong ETag.
            return Futures.immediateFuture(
              HttpResponse.notModified<StreamedAsset>()
            ).asDeferred()
          }
        }
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
        }
        // if we arrive here, the ETag either didn't match, or was not present in a substantive way. either way we need
        // to fall back to regular serving (below).
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
