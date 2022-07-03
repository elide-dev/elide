package elide.server.cfg

import io.micronaut.context.annotation.ConfigurationProperties

/**
 * Configuration properties loaded at runtime through Micronaut's configuration system, which govern how Elide hosts
 * server-side code.
 */
@ConfigurationProperties("elide.server")
public data class ServerConfig(
  public var assets: AssetConfig,
)
