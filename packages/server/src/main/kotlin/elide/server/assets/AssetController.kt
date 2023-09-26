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

import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import java.nio.charset.StandardCharsets
import jakarta.inject.Inject
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.FinalizedAssetResponse
import elide.server.controller.StatusEnabledController

/**
 * Built-in controller implementation which bridges the configured asset serving prefix to the active [AssetManager]
 * layer for this server run.
 *
 * For this controller to be enabled, the configuration value `elide.assets.enabled` needs to be set to `true`. The
 * asset prefix used by this controller is governed by the configuration value `elide.assets.prefix`.
 *
 * @param assetManager Main asset manager which should be used to resolve and serve assets.
 */
@Requires(property = "elide.assets.isEnabled", notEquals = "false")
@Controller("\${elide.assets.prefix:/_/assets}")
public class AssetController @Inject constructor(private val assetManager: AssetManager) : StatusEnabledController {
  // Logger pipe.
  private val logging: Logger = Logging.of(AssetController::class)

  /**
   * Handles HTTP `GET` calls to asset endpoints based on "asset tag" values, which are generated at build time, and are
   * typically composed of  8-16 characters from the tail end of the content hash for the asset.
   *
   * @param request HTTP request incoming to this endpoint.
   * @param tag Decoded tag value from the URL.
   * @param ext Extension value from the URL.
   */
  @Get("/{tag}.{ext}")
  public suspend fun assetGet(request: HttpRequest<*>, tag: String, ext: String): FinalizedAssetResponse {
    logging.debug(
      "Loading asset with tag '$tag' (extension: '$ext')"
    )
    return assetManager.serveAsync(
      request
    ).await().let { response ->
      val body = response.body()
      if (body != null) {
        val (type, stream) = response.body()
        response.contentType(type).body(stream)
      } else {
        response.body("Not found".toByteArray(StandardCharsets.UTF_8))
      }
    }
  }
}
