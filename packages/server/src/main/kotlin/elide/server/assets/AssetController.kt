package elide.server.assets

import elide.server.StreamedAssetResponse
import elide.server.controller.StatusEnabledController
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Options
import jakarta.inject.Inject

/**
 * Built-in controller implementation which bridges the configured asset serving prefix to the active [AssetManager]
 * layer for this server run.
 *
 * For this controller to be enabled, the configuration value `elide.assets.enabled` needs to be set to `true`. The
 * asset prefix used by this controller is governed by the configuration value `elide.assets.prefix`.
 */
@Requires(property = "elide.assets.enabled", value = "true")
@Controller("\${elide.assets.prefix}") public class AssetController : StatusEnabledController {
  @Inject internal lateinit var assetManager: AssetManager

  /**
   * TBD
   */
  @Get public suspend fun assetGet(request: HttpRequest<*>): StreamedAssetResponse {
    return assetManager.serveAsync(
      request
    ).await()
  }

  /**
   * TBD
   */
  @Options public suspend fun assetOptions(request: HttpRequest<*>): HttpResponse<*> {
    TODO("not yet implemented")
  }
}
