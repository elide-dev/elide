package elide.rpc.server.web

import com.google.protobuf.Message
import io.grpc.Metadata
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckResponse
import io.grpc.health.v1.HealthGrpc
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.assertThrows
import jakarta.inject.Inject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Tests for the [GrpcWebServiceRelay] internals. */
@MicronautTest
class GrpcWebServiceRelayTest {
  @Inject internal lateinit var serviceRelay: GrpcWebServiceRelay

  @Test fun testInjectable() {
    assertNotNull(
      serviceRelay,
      "should be able to inject a service relay instance"
    )
  }

  @Test fun testLoadUnrecognizedClass() {
    assertThrows<StatusRuntimeException> {
      serviceRelay.reflectivelyLoadGrpcClass(
        "class.that.does.not.Exist"
      )
    }
  }

  @Test fun testLoadRestrictedClass() {
    assertThrows<StatusRuntimeException> {
      serviceRelay.reflectivelyLoadGrpcClass(
        "java.lang.String"
      )
    }
  }

  @Test fun testLoadUnrecognizedMethod() {
    assertThrows<StatusRuntimeException> {
      serviceRelay.reflectivelyLoadGrpcMethod(
        GrpcWebServiceRelay::class.java,
        "unknownMethod"
      )
    }
  }

  @Test fun testResolveJavaPackagePath() {
    val (path, name) = serviceRelay.resolveServiceJavaPackageAndName(
      ServerServiceDefinition.builder(HealthGrpc.getServiceDescriptor())
        .addMethod(
          HealthGrpc.getCheckMethod()
        ) { _, _ ->
          throw IllegalStateException("not dispatchable")
        }
        .addMethod(
          HealthGrpc.getWatchMethod()
        ) { _, _ ->
          throw IllegalStateException("not dispatchable")
        }
        .build()
    )
    assertEquals(
      "io.grpc.health.v1",
      path,
      "path for health service should be expected value"
    )
    assertEquals(
      "Health",
      name,
      "name for health service should be expected value"
    )
  }

  @Test fun testTrailersFromThrowable() {
    val sampleTrailers = Metadata()
    sampleTrailers.put(
      GrpcWeb.Metadata.apiKey,
      "here is an epi key"
    )

    val throwable = Status.INTERNAL.withDescription(
      "Error description"
    ).asRuntimeException(
      sampleTrailers
    )
    val trailers = serviceRelay.trailersFromThrowable(throwable)
    assertNotNull(
      trailers,
      "should resolve non-null trailers from `StatusRuntimeException`"
    )

    val throwable2 = Status.INTERNAL.withDescription(
      "Error description"
    ).asException(
      sampleTrailers
    )
    val trailers2 = serviceRelay.trailersFromThrowable(throwable2)
    assertNotNull(
      trailers2,
      "should resolve non-null trailers from `StatusException`"
    )

    val throwable3 = IllegalStateException(
      "not a gRPC throwable"
    )
    val trailers3 = serviceRelay.trailersFromThrowable(throwable3)
    assertNull(
      trailers3,
      "should resolve null trailers from non-gRPC exception"
    )
  }

  @Test fun testSerializeSingleResponse() {
    val responses = listOf(
      HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.SERVING).build(),
    )
    val serialized = serviceRelay.serializeResponses(
      responses
    )
    val unary = HealthCheckResponse.parseFrom(serialized)
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      unary.status,
      "should properly serialize response"
    )
  }

  @Test fun testSerializeMultipleResponsesPickFirst() {
    val responses = listOf(
      HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.SERVING).build(),
      HealthCheckResponse.newBuilder().setStatus(HealthCheckResponse.ServingStatus.NOT_SERVING).build(),
    )
    val serialized = serviceRelay.serializeResponses(
      responses
    )
    val unary = HealthCheckResponse.parseFrom(serialized)
    assertEquals(
      HealthCheckResponse.ServingStatus.SERVING,
      unary.status,
      "should pick first response"
    )
  }

  @Test fun testSerializeEmptyResponse() {
    val responses = listOf<Message>()
    val serialized = serviceRelay.serializeResponses(responses)
    assertEquals(
      0,
      serialized.size,
      "should get back empty byte array for empty list of responses"
    )
  }

  @Test fun testSerializeNonMessages() {
    assertThrows<IllegalArgumentException> {
      val responses = listOf(
        "sample bad type for response"
      )
      serviceRelay.serializeResponses(responses)
    }
  }
}
