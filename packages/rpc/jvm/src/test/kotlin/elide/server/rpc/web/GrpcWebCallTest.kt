package elide.server.rpc.web

import elide.server.rpc.web.GrpcWebCall.Companion.newCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthGrpc
import io.grpc.inprocess.InProcessChannelBuilder
import io.micronaut.http.HttpRequest
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.nio.charset.StandardCharsets
import java.security.Principal
import kotlin.test.*

/** Tests for the [GrpcWebCall] container. */
class GrpcWebCallTest {
  @Test fun testCreateWebCallText() {
    val settings = GrpcWebConfig.DEFAULTS
    val serviceBuilder = ServerServiceDefinition.builder(
      HealthGrpc.getServiceDescriptor()
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getCheckMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getWatchMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    val svc = serviceBuilder.build()
    val method = svc.methods.first()
    val channel = InProcessChannelBuilder.forName("example").build()
    val buf = ByteArray(0)
    val req = HttpRequest.POST("/_/rpc", buf)
    val principal = Principal { "sample" }
    val call = GrpcWebContentType.TEXT.newCall(
      settings,
      svc,
      method,
      channel,
      req,
      principal,
    )
    assertNotNull(
      call,
      "should not get `null` from call factory based on content type"
    )
    assertEquals(
      GrpcWebContentType.TEXT,
      call.contentType,
      "text type for call should be preserved"
    )
    assertNotNull(call.config)
    assertNotNull(call.channel)
    assertNotNull(call.contentType)
    assertNotNull(call.httpRequest)
    assertNotNull(call.method)
    assertNotNull(call.service)
    assertNotNull(call.httpResponse)
    assertNotNull(call.principal)
  }

  @Test fun testCreateWebCallBinary() {
    val settings = GrpcWebConfig.DEFAULTS
    val serviceBuilder = ServerServiceDefinition.builder(
      HealthGrpc.getServiceDescriptor()
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getCheckMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getWatchMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    val svc = serviceBuilder.build()

    val method = svc.methods.first()
    val channel = InProcessChannelBuilder.forName("example").build()
    val buf = ByteArray(0)
    val req = HttpRequest.POST("/_/rpc", buf)
    val principal = Principal { "sample" }
    val call = GrpcWebContentType.BINARY.newCall(
      settings,
      svc,
      method,
      channel,
      req,
      principal,
    )
    assertNotNull(
      call,
      "should not get `null` from call factory based on content type"
    )
    assertEquals(
      GrpcWebContentType.BINARY,
      call.contentType,
      "text type for call should be preserved"
    )
    assertNotNull(call.config)
    assertNotNull(call.channel)
    assertNotNull(call.contentType)
    assertNotNull(call.httpRequest)
    assertNotNull(call.method)
    assertNotNull(call.service)
    assertNotNull(call.httpResponse)
    assertNotNull(call.principal)
  }

  @Test fun testFinalizedGrpcWebCall() {
    val settings = GrpcWebConfig.DEFAULTS
    val serviceBuilder = ServerServiceDefinition.builder(
      HealthGrpc.getServiceDescriptor()
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getCheckMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    serviceBuilder.addMethod(
      ServerMethodDefinition.create(HealthGrpc.getWatchMethod()) { _, _ ->
        object : ServerCall.Listener<HealthCheckRequest>() {
          // nothing
        }
      }
    )
    val svc = serviceBuilder.build()

    val method = svc.methods.first()
    val channel = InProcessChannelBuilder.forName("example").build()
    val buf = ByteArray(0)
    val req = HttpRequest.POST("/_/rpc", buf)
    val principal = Principal { "sample" }
    val call = GrpcWebContentType.BINARY.newCall(
      settings,
      svc,
      method,
      channel,
      req,
      principal,
    )

    // finalize the response
    assertDoesNotThrow {
      call.notifyResponse(
        GrpcWebCallResponse.Error(
          contentType = GrpcWebContentType.BINARY,
          status = Status.INTERNAL,
          cause = null,
          headers = Metadata(),
          trailers = Metadata(),
        )
      )
    }

    // finalized should now report as `true`
    assertTrue(
      call.finished(),
      "call should report `finished()` as `true` when finished"
    )

    // finalizing again should throw
    assertThrows<IllegalStateException> {
      call.notifyResponse(
        GrpcWebCallResponse.Error(
          contentType = GrpcWebContentType.BINARY,
          status = Status.INTERNAL,
          cause = null,
          headers = Metadata(),
          trailers = Metadata(),
        )
      )
    }
  }

  @Test fun testErrorResponseType() {
    val errorResponse = GrpcWebCallResponse.Error(
      contentType = GrpcWebContentType.BINARY,
      status = Status.INTERNAL,
      cause = null,
      headers = Metadata(),
      trailers = Metadata(),
    )
    assertEquals(
      GrpcWebContentType.BINARY,
      errorResponse.contentType,
      "error response should preserve gRPC content type"
    )
    assertEquals(
      Status.INTERNAL,
      errorResponse.status,
      "error response should preserve gRPC status"
    )
    assertNotNull(
      errorResponse.headers,
      "error response should preserve gRPC headers"
    )
    assertNotNull(
      errorResponse.trailers,
      "error response should preserve gRPC trailers"
    )
    assertNull(
      errorResponse.cause,
      "error response should preserve `null` cause"
    )
    assertFalse(
      errorResponse.success,
      "success should report as `false` for error response"
    )
    val errorResponse2 = GrpcWebCallResponse.Error(
      contentType = GrpcWebContentType.BINARY,
      status = Status.INTERNAL,
      cause = IllegalStateException("sample"),
      headers = Metadata(),
      trailers = Metadata(),
    )
    assertNotNull(
      errorResponse2.cause,
      "error response should preserve cause if provided"
    )
    assertFalse(
      errorResponse2.success,
      "success should report as `false` for error response"
    )
  }

  @Test fun testUnaryResponseType() {
    val unaryResponse = GrpcWebCallResponse.UnaryResponse(
      contentType = GrpcWebContentType.BINARY,
      headers = Metadata(),
      trailers = Metadata(),
      payload = ByteArray(0),
    )
    assertEquals(
      GrpcWebContentType.BINARY,
      unaryResponse.contentType,
      "unary response should preserve gRPC content type"
    )
    assertNotNull(
      unaryResponse.headers,
      "unary response should preserve gRPC headers"
    )
    assertNotNull(
      unaryResponse.trailers,
      "unary response should preserve gRPC trailers"
    )
    assertTrue(
      unaryResponse.success,
      "success should report as `true` for unary response"
    )
    assertEquals(
      0,
      unaryResponse.payload.size,
      "response payload on unary response structure should not mutate or error when given empty data"
    )
    val unaryResponse2 = GrpcWebCallResponse.UnaryResponse(
      contentType = GrpcWebContentType.BINARY,
      headers = Metadata(),
      trailers = Metadata(),
      payload = "hello".toByteArray(StandardCharsets.UTF_8),
    )
    assertEquals(
      GrpcWebContentType.BINARY,
      unaryResponse2.contentType,
      "unary response should preserve gRPC content type"
    )
    assertNotNull(
      unaryResponse2.headers,
      "unary response should preserve gRPC headers"
    )
    assertNotNull(
      unaryResponse2.trailers,
      "unary response should preserve gRPC trailers"
    )
    assertTrue(
      unaryResponse2.success,
      "success should report as `true` for unary response"
    )
    assertEquals(
      "hello".toByteArray(StandardCharsets.UTF_8).size,
      unaryResponse2.payload.size,
      "response payload size should correspond to given data"
    )
  }
}
