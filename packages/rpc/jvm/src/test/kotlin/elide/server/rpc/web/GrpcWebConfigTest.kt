package elide.server.rpc.web

import java.time.Duration
import kotlin.test.*

/** Tests for server-side config via [GrpcWebConfig]. */
class GrpcWebConfigTest {
  @Test fun testGrpcWebConfigDefaults() {
    val default = GrpcWebConfig.DEFAULTS
    assertFalse(
      default.enabled,
      "gRPC-web integration should not be enabled unless explicitly enabled"
    )
    assertEquals(
      default.endpoint,
      "/_/rpc",
      "default endpoint should be expected value"
    )
    assertNotNull(
      default.timeout,
      "default timeout value should be non-null"
    )
    assertTrue(
      default.timeout.seconds > 0,
      "default timeout seconds should be a non-negative non-zero number"
    )
    assertTrue(
      default.timeout.seconds < 3600,
      "default timeout seconds should be reasonable value (less than an hour)"
    )
  }

  @Test fun testGrpcWebConfigMutability() {
    val default = GrpcWebConfig.DEFAULTS
    assertFalse(
      default.enabled,
      "gRPC-web integration should not be enabled unless explicitly enabled"
    )
    assertEquals(
      default.endpoint,
      "/_/rpc",
      "default endpoint should be expected value"
    )
    default.enabled = true
    default.endpoint = "/_/some-other-endpoint"
    default.timeout = Duration.ofSeconds(1)
    @Suppress("KotlinConstantConditions")
    assertTrue(
      default.enabled,
      "gRPC-web integration enablement should be mutable"
    )
    assertEquals(
      default.endpoint,
      "/_/some-other-endpoint",
      "default RPC endpoint should be mutable"
    )
    assertEquals(
      1,
      default.timeout.seconds,
      "should be able to override default request timeout"
    )
  }
}
