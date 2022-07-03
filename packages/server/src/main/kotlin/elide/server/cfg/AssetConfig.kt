package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Configuration loaded at runtime which governs Elide's built-in asset serving tools.
 */
@ConfigurationProperties("elide.server.assets")
public data class AssetConfig(
  public var enabled: Boolean = true,
  public var prefix: String = "/_/assets",
)
