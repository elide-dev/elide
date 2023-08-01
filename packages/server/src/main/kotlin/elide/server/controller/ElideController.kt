package elide.server.controller

import io.micronaut.context.ApplicationContext
import elide.server.assets.AssetManager

/**
 * Describes the top-level expected interface for Elide-based controllers; any base class which inherits from this one
 * may be used as a controller, and activated/deactivated with Micronaut annotations (see: `@Controller`).
 */
public interface ElideController {
  /** @return Access to the active asset manager. */
  public fun assets(): AssetManager

  /** @return Access to the active application context. */
  public fun context(): ApplicationContext
}
