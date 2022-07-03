package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Configuration loaded at runtime which governs Elide's built-in asset serving tools.
 *
 * @param enabled Whether the asset system is enabled.
 * @param prefix URI prefix where static assets are served.
 */
@ConfigurationProperties("elide.server.assets")
public data class AssetConfig(
  public var enabled: Boolean = true,
  public var prefix: String = "/_/assets",
)
