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

import com.google.common.annotations.VisibleForTesting
import com.google.protobuf.InvalidProtocolBufferException
import io.grpc.*
import io.micronaut.context.annotation.Requires
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import java.nio.charset.StandardCharsets
import java.security.Principal
import java.util.concurrent.CountDownLatch
import jakarta.inject.Inject
import kotlinx.coroutines.Deferred
import elide.rpc.server.RpcRuntime
import elide.rpc.server.web.GrpcWeb.Headers
import elide.rpc.server.web.GrpcWebCall.Companion.newCall
import elide.rpc.server.web.GrpcWebContentType.Companion.allValidContentTypes
import elide.runtime.Logger
import elide.runtime.Logging
import elide.server.controller.StatusEnabledController

/**
 * Entrypoint controller for gRPC Web traffic handled on behalf of the user's application by Elide's RPC framework.
 *
 * This controller is responsible for resolving the appropriate service to dispatch, enforcing expectations against the
 * incoming request, and performing serialization back and forth between the browser and backing gRPC service. Each of
 * these functions is implemented in other classes, but all are driven via this controller.
 *
 * Services are resolved via [RpcRuntime], which is notified of service registration at the time the gRPC server starts
 * up (managed by Micronaut).
 */
@Requires(property = "elide.grpc.web.isEnabled", value = "true")
@Controller("\${elide.grpc.web.endpoint:/_/rpc}") public class GrpcWebController: StatusEnabledController {
  /**
   * Describes an error encountered early during this controller's processing cycle, before the request has been
   * resolved to a service and method.
   *
   * @param status gRPC status to indicate for this error.
   * @param message Message to include with the error, if desired.
   * @param cause Cause of the error, if applicable.
   */
  private class GrpcWebControllerError (
    internal val status: Status,
    message: String?,
    cause: Throwable?,
  ): RuntimeException(
    message,
    cause,
  ) {
    // Return a set of trailers to use for this error.
    internal fun trailers(): Metadata = Metadata()
  }

  // Private logger.
  private val logging: Logger = Logging.of(GrpcWebController::class)

  /** Configuration settings interpreted from the current application. */
  @Inject internal lateinit var settings: GrpcWebConfig

  /** Access to runtime RPC service resources. */
  @Inject internal lateinit var runtime: RpcRuntime

  /** Invocation service over which we can communicate with the gRPC server. */
  @Inject internal lateinit var relayService: GrpcWebService

  // Check that the `Content-Type` on an RPC request is eligible for processing.
  @VisibleForTesting internal fun checkContentType(request: HttpRequest<RawRpcPayload>): Boolean {
    return (!request.contentType.isEmpty && allValidContentTypes.contains(request.contentType.get().toString()))
  }

  // Check that the special `grpc-web` header is present on an RPC request.
  @VisibleForTesting internal fun checkSentinelHeader(request: HttpRequest<RawRpcPayload>): Boolean {
    return (request.headers.contains(Headers.sentinel))
  }

  /**
   * Synthesize a gRPC Web-compatible error response describing the provided [status] and optionally the provided
   * [errMessage]; if no message is provided, a default message will be used.
   *
   * This method merely prepares a throwable. The caller must throw the return value to trigger the error response.
   *
   * @param status gRPC status to indicate in the synthesized response.
   * @param errMessage Error message to enclose in the synthesized response.
   * @param cause Cause of the error, if applicable.
   * @return Throwable prepared to signal the provided error info.
   */
  @Suppress("SameParameterValue")
  private fun synthesizeGrpcError(
    status: Status,
    errMessage: String?,
    cause: Throwable?
  ): GrpcWebControllerError {
    return GrpcWebControllerError(
      status,
      errMessage,
      cause,
    )
  }

  /**
   * Build a response for a gRPC Web [call] which has completed processing and now needs conversion to an HTTP response;
   * whether the call was successful or not, our job here is to translate that call into a compliant response.
   *
   * @param call gRPC Web call which has finished processing and now needs a response synthesized.
   * @return HTTP response built from the provided [call].
   */
  @VisibleForTesting
  internal fun synthesizeGrpcResponse(call: GrpcWebCall): HttpResponse<RawRpcPayload> {
    return call.httpResponse
  }

  /**
   * Build a generic gRPC-Web-compliant response for the provided [status] and optional set of [headers] and [trailers];
   * the response structure returned is mutable, and any [body] provided will be encoded and attached based on the
   * provided [contentType].
   *
   * @param status gRPC status to indicate in the synthesized response.
   * @param headers gRPC metadata to include in the headers portion of the response.
   * @param trailers gRPC metadata to include in the trailers portion of the response.
   * @param contentType Content type of the response we should produce.
   * @param body Raw body data to enclose with the response, as applicable.
   * @return HTTP response built from the provided parameters.
   */
  @VisibleForTesting
  internal fun synthesizeGrpcResponse(
    status: Status,
    headers: Metadata,
    trailers: Metadata,
    contentType: GrpcWebContentType,
    body: RawRpcPayload,
  ): HttpResponse<RawRpcPayload> {
    // generate an unconditional HTTP 200
    val response = HttpResponse.ok<RawRpcPayload>()

    // write the RPC response
    ResponseUtil.writeResponse(
      contentType,
      response,
      status,
      headers,
      trailers,
      body,
    )
    return response
  }

  /**
   * Resolve the [service] and [method] described by the given parameters, via the active [RpcRuntime]; if exactly
   * matching values aren't found, throw a [StatusRuntimeException] with status [Status.NOT_FOUND].
   *
   * @throws StatusRuntimeException if the service or method cannot be located.
   * @param service Fully-qualified name of the gRPC service which we should resolve.
   * @param method Name of the gRPC method which we should resolve.
   */
  @VisibleForTesting
  internal fun resolveService(
    service: String,
    method: String
  ): Pair<ServerServiceDefinition, ServerMethodDefinition<*, *>> {
    val svc = runtime.resolveService(service)
    if (svc != null) {
      val target = svc.getMethod("$service/$method")
      if (target != null) {
        return svc to target
      }
    }
    throw synthesizeGrpcError(
      Status.UNIMPLEMENTED,
      "Service or method not found",
      cause = null,
    )
  }

  /**
   * Resolve the provided [serviceName] and [methodName] to a callable gRPC service/method pair, and then invoke the
   * method, providing the enclosed [request] and [principal]; if an error condition is encountered while decoding the
   * body from the provided [request], or while resolving the named service and method, then an exception is raised.
   *
   * If the service cannot be located, [Status.NOT_FOUND] is thrown. If the request cannot be decoded safely into a
   * valid protocol buffer message of the expected type, [Status.INVALID_ARGUMENT] is thrown. In either case, a
   * descriptive error message is enclosed in the response trailers.
   *
   * @throws StatusRuntimeException if the service or method cannot be located, or if the request cannot be decoded.
   * @param contentType gRPC Web content type of the request. The response should match.
   * @param serviceName Fully-qualified name of the gRPC service which we should resolve.
   * @param methodName Name of the gRPC method which we should resolve.
   * @param request HTTP request containing the body to decode and invoke the resolved method with.
   * @param principal Logged-in security principal, as applicable.
   * @return Deferred task which resolves to a [GrpcWebCall] describing the call result.
   */
  @VisibleForTesting
  internal suspend fun dispatchServiceAsync(
    contentType: GrpcWebContentType,
    serviceName: String,
    methodName: String,
    request: HttpRequest<RawRpcPayload>,
    principal: Principal?,
  ): Deferred<GrpcWebCall> {
    // resolve the service, method, and content type. then acquire a channel.
    val (service, method) = resolveService(serviceName, methodName)
    val channel = relayService.channel()

    // create a new gRPC call to wrap this operation
    val call = contentType.newCall(
      settings,
      service,
      method,
      channel,
      request,
      principal,
    )

    // prepare the client interceptor with this channel.
    val syncLatch = CountDownLatch(1)
    val interceptor = GrpcWebClientInterceptor(
      syncLatch,
    )

    return relayService.fulfillAsync(
      call,
      interceptor,
    )
  }

  /**
   * Main RPC request handler; all gRPC-Web requests are expected to operate over `POST`.
   *
   * This is the effective entrypoint for gRPC-Web traffic handled by Elide on behalf of a framework user's application.
   * Messages flow in from the JavaScript or Kotlin RPC client layer, encoded as protocol buffer messages, either in
   * binary form or wrapped in Base64.
   *
   * ### Method Resolution
   * The handler calls into [RpcRuntime] to resolve the gRPC service named at the provided [servicePath]. The method
   * described by [methodName] is then resolved from the generated gRPC service descriptor. If either of these steps
   * fail, the handler will return an [HttpResponse] describing [Status.NOT_FOUND], with an error message packed in the
   * `grpc-message` header describing what went wrong.
   *
   * ### Error handling
   * Unhandled and/or unrecognized exceptions will be translated into their gRPC equivalent, carrying a status value of
   * [Status.INTERNAL]. Any exception which is not a [StatusRuntimeException] will be considered "unrecognized."
   * Inheritors of [StatusRuntimeException] preserve their status and trailers.
   *
   * @throws StatusRuntimeException if the service or method cannot be located, or if the request cannot be decoded, or
   *   if any runtime error occurs. Any exception that *does not* inherit from this type should be considered unhandled.
   * @see RpcRuntime for service and method resolution details.
   * @param servicePath Fully-qualified path for the gRPC service under dispatch.
   * @param methodName Name of the method under dispatch.
   * @param request HTTP request which is incoming to the RPC endpoint, and which should be processed as a potential
   *   gRPC-Web invocation.
   * @param principal Logged-in security principal, if any.
   * @return HTTP response to be sent back to the invoking client.
   */
  @Post("/{servicePath}/{methodName}") public suspend fun handleRequest(
    servicePath: String,
    methodName: String,
    request: HttpRequest<RawRpcPayload>,
    principal: Principal?
  ): HttpResponse<RawRpcPayload> {
    // make sure gRPC-web integration is enabled
    return if (!settings.isEnabled) {
      return HttpResponse.notFound()
    } else try {
      // `servicePath` and `methodName` must be non-empty/non-blank
      if (servicePath.isEmpty() || servicePath.isBlank()) {
        logging.warn {
          "gRPC-web service path was blank or empty; rejecting request with `SERVICE_PATH_INVALID`."
        }
        return HttpResponse.badRequest<RawRpcPayload?>().body(
          "SERVICE_PATH_INVALID".toByteArray(StandardCharsets.UTF_8)
        )
      } else if (methodName.isEmpty() || methodName.isBlank()) {
        logging.warn {
          "gRPC-web service method was blank or empty; rejecting request with `METHOD_NAME_INVALID`."
        }
        return HttpResponse.badRequest<RawRpcPayload?>().body(
          "METHOD_NAME_INVALID".toByteArray(StandardCharsets.UTF_8)
        )
      }

      // `content-type` must be specified, and must be an acceptable value
      if (!checkContentType(request)) {
        logging.warn {
          "gRPC-web content type was not allowed or missing; rejecting request with `INVALID_CONTENT_TYPE`."
        }
        return HttpResponse.badRequest<RawRpcPayload?>().body(
          "INVALID_CONTENT_TYPE".toByteArray(StandardCharsets.UTF_8)
        )
      }

      // resolve the type so the response can be encoded appropriately
      val grpcWebContentType = GrpcWebContentType.resolve(request.contentType.get())

      // sentinel header must be present on the request
      if (!checkSentinelHeader(request)) {
        logging.warn {
          "gRPC-web request was missing sentinel header; rejecting request with `BAD_REQUEST`."
        }
        return HttpResponse.badRequest<RawRpcPayload?>().body(
          "BAD_REQUEST".toByteArray(StandardCharsets.UTF_8)
        )
      }

      logging.debug {
        "Dispatching via gRPC: '$servicePath:$methodName' (content type: '${request.contentType.get().type}')"
      }

      try {
        // dispatch the service, then serialize the response
        synthesizeGrpcResponse(
          dispatchServiceAsync(
            grpcWebContentType,
            servicePath,
            methodName,
            request,
            principal
          ).await()
        )
      } catch (sre: StatusRuntimeException) {
        logging.warn(
          "The gRPC Web request threw a gRPC-compatible exception of status '${sre.status}'"
        )
        synthesizeGrpcResponse(
          sre.status,
          Metadata(),
          sre.trailers ?: Metadata(),
          grpcWebContentType,
          ByteArray(0),
        )
      } catch (cre: GrpcWebControllerError) {
        // an error occurred before method processing began
        logging.warn(
          "Relaying pre-fulfillment gRPC-Web error: '${cre.status}'"
        )
        synthesizeGrpcResponse(
          cre.status.withCause(
            cre.cause
          ).withDescription(
            cre.message ?: ""
          ),
          Metadata(),
          cre.trailers(),
          grpcWebContentType,
          ByteArray(0),
        )
      }
    } catch (ipbe: InvalidProtocolBufferException) {
      // an internal error occurred of some other kind
      logging.warn(
        "Request was reported as invalid protocol buffer; rejecting with `MALFORMED_PAYLOAD` (Bad Request HTTP 400).",
      )
      return HttpResponse.badRequest<RawRpcPayload?>().body(
        "MALFORMED_PAYLOAD".toByteArray(StandardCharsets.UTF_8)
      )
    } catch (iae: IllegalArgumentException) {
      logging.warn(
        "The gRPC Web request was malformed; rejecting with 'MALFORMED_STREAM'."
      )
      return HttpResponse.badRequest<RawRpcPayload?>().body(
        "MALFORMED_STREAM".toByteArray(StandardCharsets.UTF_8)
      )
    }
  }
}
