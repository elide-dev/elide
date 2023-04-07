package elide.rpc.server.web

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.core.util.Toggleable
import java.time.Duration

/**
 * Describes active configuration for Elide's RPC layer as related to integration with gRPC Web.
 */
@ConfigurationProperties("elide.grpc.web")
public interface GrpcWebConfig : Toggleable {
  override fun isEnabled(): Boolean = DEFAULT_ENABLED

  /**
   * @return RPC endpoint prefix.
   */
  public val endpoint: String get() = defaultEndpoint

  /**
   * @return Timeout duration for RPC calls.
   */
  public val timeout: Duration get() = Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS)

  public companion object {
    /** Whether to enable the gRPC-Web integration by default. */
    public const val DEFAULT_ENABLED: Boolean = false

    /** Whether to enable the gRPC-Web integration by default. */
    public const val DEFAULT_TIMEOUT_SECONDS: Long = 30L

    /** Default gRPC-Web RPC traffic endpoint prefix. */
    public const val defaultEndpoint: String = "/_/rpc"

    /** Default gRPC-Web settings. */
    public val DEFAULTS: GrpcWebConfig = object : GrpcWebConfig {}
  }
}
