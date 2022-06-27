package elide.server.rpc.web

import io.micronaut.context.annotation.ConfigurationProperties
import java.time.Duration

/**
 * Describes active configuration for Elide's RPC layer as related to integration with gRPC Web.
 *
 * @param enabled Whether gRPC Web support is enabled.
 * @param endpoint Base URI where RPC requests should be handled by the built-in controller.
 */
@ConfigurationProperties("elide.grpc.web")
data class GrpcWebConfig(
  var enabled: Boolean = false,
  var endpoint: String = defaultEndpoint,
  var timeout: Duration = Duration.ofSeconds(30),
) {
  companion object {
    const val defaultEndpoint: String = "/_/rpc"
    val DEFAULTS: GrpcWebConfig = GrpcWebConfig()
  }
}
