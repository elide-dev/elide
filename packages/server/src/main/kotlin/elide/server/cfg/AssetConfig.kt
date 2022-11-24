package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Configuration loaded at runtime which governs Elide's built-in asset serving tools.
 *
 * @param enabled Whether the asset system is enabled.
 * @param prefix URI prefix where static assets are served.
 * @param etags Whether to generate, and respond to, ETag headers for assets.
 * @param preferWeakEtags Whether to prefer weak ETags. Defaults to `false`.
 * @param rewriting Whether to enable or disable rewriting (globally -- all rewriting features).
 * @param hashLinks Whether to rewrite asset links based on their content hashes.
 */
@ConfigurationProperties("elide.server.assets")
public data class AssetConfig(
  public var enabled: Boolean = true,
  public var prefix: String = "/_/assets",
  public var etags: Boolean = true,
  public var preferWeakEtags: Boolean = false,
  public var rewriting: Boolean = true,
  public var hashLinks: Boolean = true,
)
