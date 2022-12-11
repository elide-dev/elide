package elide.rpc.server.web

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

/**
 * Describes active configuration for Elide's RPC layer as related to integration with gRPC Web.
 *
 * @param enabled Whether gRPC Web support is enabled.
 * @param endpoint Base URI where RPC requests should be handled by the built-in controller.
 */
@ConfigurationProperties("elide.grpc.web")
public data class GrpcWebConfig(
  public var enabled: Boolean = false,
  public var endpoint: String = defaultEndpoint,
  public var timeout: Duration = Duration.ofSeconds(30),
) {
  public companion object {
    public const val defaultEndpoint: String = "/_/rpc"
    public val DEFAULTS: GrpcWebConfig = GrpcWebConfig()
  }
}
