/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.rpc.server.web

import com.google.protobuf.Message
import io.grpc.Metadata
import io.grpc.Status
import io.micronaut.context.BeanContext
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.Principal
import java.util.*
import jakarta.inject.Inject
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import elide.rpc.server.RpcRuntime
import elide.runtime.Logger
import elide.runtime.Logging

/** Provides shared testing logic to tests in the `elide.rpc.web` package. */
@Suppress("unused", "SameParameterValue", "MemberVisibilityCanBePrivate") abstract class GrpcWebBaseTest {
  /** Access to runtime RPC service resources. */
  @Inject internal lateinit var runtime: RpcRuntime

  /** Invocation service over which we can communicate with the gRPC server. */
  @Inject internal lateinit var relayService: GrpcWebService

  /** Bean context for testing DI conditions. */
  @Inject internal lateinit var beanContext: BeanContext

  companion object {
    // Test logger.
    protected val logging: Logger = Logging.root()
  }

  protected fun controller(settings: GrpcWebConfig = object : GrpcWebConfig {}): GrpcWebController {
    val ctr = GrpcWebController()
    ctr.runtime = runtime
    ctr.relayService = relayService
    ctr.settings = settings
    return ctr
  }

  // Build a valid gRPC Web request for the provided `service`, `method`, and message `payload`.
  protected fun buildRequest(
    service: String,
    method: String,
    payload: Message?,
    overridePayload: ByteArray?,
    contentType: GrpcWebContentType = GrpcWebContentType.BINARY,
  ): HttpRequest<RawRpcPayload> {
    // serialize the payload as a gRPC Web request
    val bytestream = ByteArrayOutputStream()
    val serialized = overridePayload ?: if (payload != null) {
      val rawMessageBytes = payload.toByteArray()
      val prefix = MessageFramer.getPrefix(rawMessageBytes, RpcSymbol.DATA)
      bytestream.use { stream ->
        if (contentType == GrpcWebContentType.TEXT) {
          stream.writeBytes(
            Base64.getEncoder().encode(
              prefix.plus(rawMessageBytes)
            )
          )
        } else {
          stream.writeBytes(prefix)
          stream.writeBytes(rawMessageBytes)
        }
      }
      bytestream.toByteArray()
    } else {
      ByteArray(0)
    }

    // build our initial request
    return HttpRequest.POST(
      "/_/rpc/$service/$method",
      serialized
    ).header(
      GrpcWeb.Headers.sentinel,
      "1"
    ).header(
      HttpHeaders.ACCEPT,
      contentType.toString(),
    ).contentType(
      contentType.mediaType(),
    )
  }

  // Submit the provided `request` to the provided `controller`, using existing values to make sure things align.
  protected fun submitRequest(
    controller: GrpcWebController,
    service: String,
    method: String,
    payload: ByteArray,
    principal: Principal? = null,
    contentType: GrpcWebContentType = GrpcWebContentType.BINARY,
  ): HttpResponse<RawRpcPayload> {
    return runBlocking {
      controller.handleRequest(
        service,
        method,
        buildRequest(
          service,
          method,
          null,
          payload,
          contentType,
        ),
        principal,
      )
    }
  }

  // Submit the provided `request` to the provided `controller`, using existing values to make sure things align.
  protected fun submitRequest(
    controller: GrpcWebController,
    service: String,
    method: String,
    payload: Message,
    principal: Principal? = null,
    contentType: GrpcWebContentType = GrpcWebContentType.BINARY,
  ): HttpResponse<RawRpcPayload> {
    return runBlocking {
      controller.handleRequest(
        service,
        method,
        buildRequest(
          service,
          method,
          payload,
          null,
          contentType,
        ),
        principal,
      )
    }
  }

  // Decode the protobuf described by `defaultInstance` from the provided `response`, using `format`.
  protected fun <T: Message> decodeResponse(
    response: HttpResponse<RawRpcPayload>,
    format: GrpcWebContentType,
    defaultInstance: T
  ): T {
    // check basic valid response state
    assertNotNull(
      response,
      "should never get `null` response from gRPC Web Controller"
    )
    validGrpcWebResponse(
      format,
      response,
    )

    // grab body, de-frame/de-serialize
    val bodyData = response.body.orElseThrow()
    val stream = ByteArrayInputStream(bodyData)
    val deframer = ResponseDeframer()
    val decodedMessage: Message = if (deframer.processInput(stream, format)) {
      defaultInstance.parserForType.parseFrom(
        deframer.toByteArray()
      )
    } else {
      throw IllegalArgumentException(
        "Data stream for gRPC Web dispatch was malformed"
      )
    }
    @Suppress("UNCHECKED_CAST")
    return decodedMessage as T
  }

  protected fun validGrpcWebResponse(
    format: GrpcWebContentType,
    response: HttpResponse<RawRpcPayload>
  ): Pair<Status, Metadata> {
    assertEquals(
      200,
      response.status.code,
      "response code from valid response should be HTTP 200, even for relayed error"
    )
    assertTrue(
      response.body.isPresent,
      "response body from valid response should not be empty, even for relayed error"
    )
    assertTrue(
      response.contentType.isPresent,
      "response should have a content type set, even for relayed error"
    )
    assertEquals(
      format.contentType(),
      response.contentType.get().toString(),
      "response should have a content-type of expected value matching request"
    )

    // should be able to decode data frames from the message, or, at least an empty one
    val bodyData = response.body.orElseThrow()
    assertNotNull(
      bodyData,
      "body response data should not be `null`"
    )
    assertTrue(
      bodyData.size >= 5,
      "expected at least 5 bytes for gRPC Web prefix value, even with empty data frame"
    )
    val deframer = ResponseDeframer()
    assertTrue(
      deframer.processInput(
        ByteArrayInputStream(bodyData),
        format,
      ),
      "processing response data should indicate a well-formed stream for a valid gRPC Web response"
    )
    assertNotNull(
      deframer.status,
      "should have decoded gRPC status from gRPC web response trailer (status trailer is required)"
    )
    assertTrue(
      deframer.trailers.keys().isNotEmpty(),
      "trailers should be decoded properly from gRPC web response"
    )
    return deframer.status!! to deframer.trailers
  }

  protected fun validSuccessResponse(format: GrpcWebContentType, response: HttpResponse<RawRpcPayload>): Metadata {
    val (status, trailers) = validGrpcWebResponse(
      format,
      response,
    )
    assertNotNull(
      status,
      "should not get `null` for decoded status"
    )
    assertNotNull(
      trailers,
      "should not get `null` for decoded trailers"
    )
    assertEquals(
      Status.Code.OK,
      status.code,
      "well-formed request which is expected to succeed should get OK status"
    )
    return trailers
  }

  protected fun validErrorResponse(
    expectedStatus: Status,
    format: GrpcWebContentType,
    response: HttpResponse<RawRpcPayload>,
  ): Metadata {
    val (status, trailers) = validGrpcWebResponse(
      format,
      response,
    )
    assertNotNull(
      status,
      "should not get `null` for decoded status"
    )
    assertNotNull(
      trailers,
      "should not get `null` for decoded trailers"
    )
    assertEquals(
      expectedStatus.code,
      status.code,
      "expected error status of provided type, but got different status"
    )
    return trailers
  }
}
