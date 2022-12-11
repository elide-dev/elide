package elide.rpc.server

import io.grpc.ServerServiceDefinition
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Tests for the JVM-side [RpcRuntime]. */
@MicronautTest
class RpcRuntimeTest {
  @Inject internal lateinit var runtime: RpcRuntime

  @Test fun testInjectable() {
    assertNotNull(
      runtime,
      "should be able to initialize and inject RPC runtime"
    )
  }

  @Test fun testReinitializeRuntimeFail() {
    assertThrows<IllegalStateException> {
      runtime.registerServices(listOf(
        ServerServiceDefinition.builder(
          HealthGrpc.getServiceDescriptor()
        ).build()
      ))
    }
  }
}
