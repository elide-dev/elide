package elide.rpc.server.web

import io.grpc.Status
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Basic tests for state held by [GrpcWebClientInterceptor]. */
class GrpcWebClientInterceptorTest {
  @Test fun testCreateNewInterceptor() {
    assertNotNull(
      GrpcWebClientInterceptor(CountDownLatch(0)),
      "should be able to construct a client interceptor from scratch"
    )
  }

  @Test fun testInitialInterceptorState() {
    val interceptor = GrpcWebClientInterceptor(CountDownLatch(0))
    assertEquals(
      Status.INTERNAL,
      interceptor.terminalStatus.get(),
      "default response status should be `INTERNAL`"
    )
    assertNotNull(
      interceptor.headers,
      "default set of intercepted response headers should not be `null`"
    )
    assertNotNull(
      interceptor.trailers,
      "default set of intercepted response trailers should not be `null`"
    )
  }

  @Test fun testInitialInterceptorStateMutability() {
    val interceptor = GrpcWebClientInterceptor(CountDownLatch(0))
    assertEquals(
      Status.INTERNAL,
      interceptor.terminalStatus.get(),
      "default response status should be `INTERNAL`"
    )
    assertNotNull(
      interceptor.headers,
      "default set of intercepted response headers should not be `null`"
    )
    assertNotNull(
      interceptor.trailers,
      "default set of intercepted response trailers should not be `null`"
    )
    interceptor.terminalStatus.set(Status.OK)
    assertEquals(
      Status.OK,
      interceptor.terminalStatus.get(),
      "status on interceptor should be mutable via atomic reference"
    )
  }
}
