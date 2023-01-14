package elide.rpc.server.web

import elide.grpctest.HelloServiceGrpc
import elide.grpctest.Nopackage.HelloRequest
import elide.grpctest.Nopackage.HelloResponse
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Tests for well-formed requests to the [HealthGrpc] service via the gRPC-Web gateway layer. */
@MicronautTest
class GrpcWebUnaryTest: GrpcWebBaseTest() {
  @Test fun testAcquireController() {
    assertNotNull(
      controller(),
      "should be able to acquire a test gRPC Web controller"
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testBasicHealthMethod(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()

    // submit the request, which should not throw
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        HealthGrpc.getServiceDescriptor().name,
        HealthGrpc.getCheckMethod().bareMethodName ?: "Check",
        HealthCheckRequest.newBuilder()
          .setService("grpc.health.v1.Health")
          .build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC Web Controller"
    )
    validSuccessResponse(
      format,
      response,
    )

    // decode gRPC Web response here
    val decoded = decodeResponse(
      response,
      format,
      HealthCheckResponse.getDefaultInstance(),
    )
    assertNotNull(
      decoded,
      "should be able to decode valid protocol buffer response from gRPC Web health check"
    )
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      decoded.status,
      "known-good service should report as `SERVING`"
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testDispatchNoExplicitJavaPackage(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()

    // submit the request, which should not throw
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        HelloServiceGrpc.getServiceDescriptor().name,
        HelloServiceGrpc.getRenderMessageMethod().fullMethodName.split("/").last(),
        HelloRequest.newBuilder()
          .setName("Sam")
          .build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC Web Controller"
    )
    validSuccessResponse(
      format,
      response,
    )

    // decode gRPC Web response here
    val decoded = decodeResponse(
      response,
      format,
      HelloResponse.getDefaultInstance(),
    )
    assertNotNull(
      decoded,
      "should be able to decode valid protocol buffer response from gRPC Web health check"
    )
    assertEquals(
      "Hello, Sam!",
      decoded.message,
      "known-good service should report as `SERVING`"
    )
  }
}
