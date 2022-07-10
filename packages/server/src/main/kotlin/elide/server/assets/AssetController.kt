package elide.server.assets

import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.StreamedAssetResponse
import elide.server.controller.StatusEnabledController
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject

/**
 * Built-in controller implementation which bridges the configured asset serving prefix to the active [AssetManager]
 * layer for this server run.
 *
 * For this controller to be enabled, the configuration value `elide.assets.enabled` needs to be set to `true`. The
 * asset prefix used by this controller is governed by the configuration value `elide.assets.prefix`.
 *
 * @param assetManager Main asset manager which should be used to resolve and serve assets.
 */
@Requires(property = "elide.assets.enabled", notEquals = "false")
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
  public suspend fun assetGet(request: HttpRequest<*>, tag: String, ext: String): StreamedAssetResponse {
    logging.debug(
      "Loading asset with tag '$tag' (extension: '$ext')"
    )
    return assetManager.serveAsync(
      request
    ).await()
  }
}
