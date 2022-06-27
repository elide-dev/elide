package elide.server.rpc.web

import elide.grpctest.Sample.SampleRequest
import elide.grpctest.SampleServiceGrpc
import elide.grpctest.TestSampleServiceV1
import io.grpc.Status
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Error cases from remote gRPC servers, through the gRPC-Web integration layer. */
@MicronautTest
class GrpcWebFailureTest: GrpcWebBaseTest() {
  @Inject lateinit var sampleService: TestSampleServiceV1

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testErrorRelayFromServerNotFound(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()
    val response = assertDoesNotThrow {
      // intentionally craft a message that will pass all the sniff tests on the frontend of the gRPC web relay, but
      // then fail subsequently from the backing gRPC service implementation. in this case, since we're giving it a
      // service that doesn't exist, we should ultimately get a valid response detailing a `NOT_FOUND` / 404.
      //
      // note that this state is treated differently from a 404 being returned directly from the endpoint, which is
      // appropriate for an invalid service or method path -- the server, in that case, is saying that the *resource*
      // represented by those values could not be found, rather than returning a response indicating a not-found state
      // from the service (but experiencing no resolution errors along the way).
      submitRequest(
        controller,
        HealthGrpc.getServiceDescriptor().name,
        HealthGrpc.getCheckMethod().bareMethodName ?: "Check",
        HealthCheckRequest.newBuilder()
          .setService("some.service.that.doesnt.Exist")
          .build(),
        contentType = format,
      )
    }
    validErrorResponse(
      expectedStatus = Status.NOT_FOUND,
      format,
      response,
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testErrorMethodNotFound(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        HealthGrpc.getServiceDescriptor().name,
        "UnknownMethod",
        HealthCheckRequest.newBuilder()
          .setService("some.service.that.doesnt.Exist")
          .build(),
        contentType = format,
      )
    }
    validErrorResponse(
      expectedStatus = Status.UNIMPLEMENTED,
      format,
      response,
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testErrorMalformedPayload(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        HealthGrpc.getServiceDescriptor().name,
        HealthGrpc.getCheckMethod().bareMethodName ?: "Check",
        "UHDFdiugsLIUHiyfgqyghduihvuiqhiugbfiuvhsiufhafhiaufivhdiuvhivbib".toByteArray(StandardCharsets.UTF_8),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC web controller"
    )
    assertEquals(
      400,
      response.status.code,
      "should get HTTP 400 Bad Request for malformed protocol buffer request payload"
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testErrorMalformedProto(dialect: String) {
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller()
    val badProtoData = "UHDFdiugsLIUHiyfgqyghduihvuiqhiugbfiuvhsiufhafhiaufivhdiuv".toByteArray(StandardCharsets.UTF_8)
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        HealthGrpc.getServiceDescriptor().name,
        HealthGrpc.getCheckMethod().bareMethodName ?: "Check",
        MessageFramer.getPrefix(
          badProtoData,
          RpcSymbol.DATA,
        ).plus(
          badProtoData
        ),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC web controller"
    )
    assertEquals(
      400,
      response.status.code,
      "should get HTTP 400 Bad Request for malformed protocol buffer request payload"
    )
  }

  @Test fun testAcquireErrorInjectionService() {
    assertNotNull(
      sampleService,
      "should be able to inject our sample service for testing"
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testTimeoutError(dialect: String) {
    assertNotNull(
      sampleService,
      "should be able to inject our sample service for testing"
    )
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller(settings = GrpcWebConfig(
      enabled = true,
      timeout = Duration.ofSeconds(2),
    ))
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        SampleServiceGrpc.getServiceDescriptor().name,
        SampleServiceGrpc.getMethodThatTakesTooLongMethod().fullMethodName.split("/").last(),
        SampleRequest.newBuilder().setName(
          "Elide"
        ).build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC web controller"
    )
    validErrorResponse(
      expectedStatus = Status.DEADLINE_EXCEEDED,
      format,
      response,
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testErrorWithTrailers(dialect: String) {
    assertNotNull(
      sampleService,
      "should be able to inject our sample service for testing"
    )
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller(settings = GrpcWebConfig(
      enabled = true,
      timeout = Duration.ofSeconds(2),
    ))
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        SampleServiceGrpc.getServiceDescriptor().name,
        SampleServiceGrpc.getMethodWithTrailersMethod().fullMethodName.split("/").last(),
        SampleRequest.newBuilder().setName(
          "Elide"
        ).build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC web controller"
    )
    val trailers = validErrorResponse(
      expectedStatus = Status.FAILED_PRECONDITION,
      format,
      response,
    )
    val trailerKeys = trailers.keys()
    assertTrue(
      trailerKeys.contains(GrpcWeb.Metadata.trace.name()),
      "should be able to decode arbitrary trailers from responses"
    )
    assertTrue(
      trailerKeys.contains("some-binary-header-bin"),
      "should be able to decode arbitrary binary trailers from responses"
    )
  }

  @CsvSource("BINARY", "TEXT")
  @ParameterizedTest fun testFatalInProcessError(dialect: String) {
    assertNotNull(
      sampleService,
      "should be able to inject our sample service for testing"
    )
    val format = GrpcWebContentType.valueOf(dialect)
    val controller = controller(settings = GrpcWebConfig(
      enabled = true,
      timeout = Duration.ofSeconds(2),
    ))
    val response = assertDoesNotThrow {
      submitRequest(
        controller,
        SampleServiceGrpc.getServiceDescriptor().name,
        SampleServiceGrpc.getMethodWithFatalErrorMethod().fullMethodName.split("/").last(),
        SampleRequest.newBuilder().setName(
          "Elide"
        ).build(),
        contentType = format,
      )
    }
    assertNotNull(
      response,
      "should never get `null` response from gRPC web controller"
    )
    validErrorResponse(
      expectedStatus = Status.UNKNOWN,
      format,
      response,
    )
  }
}
