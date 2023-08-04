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

import io.grpc.ManagedChannel
import io.grpc.ServerMethodDefinition
import io.grpc.ServerServiceDefinition
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import java.security.Principal
import elide.rpc.server.web.GrpcWebCall.Binary
import elide.rpc.server.web.GrpcWebCall.Text

/**
 * Sealed class which represents all format cases for a gRPC Web call, namely [Text] and [Binary] cases; objects are
 * created from each child depending on the request format, to keep track of that state for the response.
 *
 * @param contentType gRPC Web Protocol content type which is in use for this call.
 * @param config gRPC Web configuration which is active for the lifecycle of this call.
 * @param service Resolved gRPC service binding which is under invocation in this call.
 * @param method Resolved gRPC service method binding which is under invocation in this call.
 * @param channel gRPC channel over which the call will be invoked, or was invoked.
 * @param principal Logged-in principal reported by Micronaut, as applicable.
 * @param httpRequest Original HTTP request which produced this gRPC Web call.
 * @param httpResponse HTTP response that will ultimately be filled and returned if the request succeeds.
 */
internal sealed class GrpcWebCall private constructor (
  val contentType: GrpcWebContentType,
  val config: GrpcWebConfig,
  val service: ServerServiceDefinition,
  val method: ServerMethodDefinition<*, *>,
  val channel: ManagedChannel,
  val principal: Principal?,
  val httpRequest: HttpRequest<RawRpcPayload>,
  val httpResponse: MutableHttpResponse<RawRpcPayload> = HttpResponse.ok(),
) {
  // Response which will be filled in once the call has completed.
  private var response: GrpcWebCallResponse? = null

  /**
   * Implementation of a [GrpcWebCall] which is based on the gRPC Web Protocol's base64-encoded text format.
   *
   * @param config gRPC Web configuration which is active for the lifecycle of this call.
   * @param httpRequest Original HTTP request which produced this gRPC Web call.
   */
  internal class Text(
    config: GrpcWebConfig,
    service: ServerServiceDefinition,
    method: ServerMethodDefinition<*, *>,
    channel: ManagedChannel,
    httpRequest: HttpRequest<RawRpcPayload>,
    principal: Principal?,
  ): GrpcWebCall(
    contentType = GrpcWebContentType.TEXT,
    config = config,
    service = service,
    method = method,
    channel = channel,
    httpRequest = httpRequest,
    principal = principal,
  )

  /**
   * Implementation of a [GrpcWebCall] which is based on the gRPC Web Protocol's raw binary format.
   *
   * @param config gRPC Web configuration which is active for the lifecycle of this call.
   * @param httpRequest Original HTTP request which produced this gRPC Web call.
   */
  internal class Binary(
    config: GrpcWebConfig,
    service: ServerServiceDefinition,
    method: ServerMethodDefinition<*, *>,
    channel: ManagedChannel,
    httpRequest: HttpRequest<RawRpcPayload>,
    principal: Principal?,
  ): GrpcWebCall(
    contentType = GrpcWebContentType.BINARY,
    config = config,
    service = service,
    method = method,
    channel = channel,
    httpRequest = httpRequest,
    principal = principal,
  )

  companion object {
    /**
     * Create a new [GrpcWebCall] typed to the current [GrpcWebContentType]; enclose all relevant information for the
     * call to be fulfilled.
     *
     * @param config Active gRPC Web integration settings.
     * @param service Resolved gRPC service definition.
     * @param method Resolved gRPC method definition.
     * @param channel Channel over which we should communicate with the gRPC server.
     * @param request HTTP request carrying the payload bytes.
     * @param principal Logged-in request principal.
     * @return gRPC call object.
     */
    @JvmStatic fun GrpcWebContentType.newCall(
      config: GrpcWebConfig,
      service: ServerServiceDefinition,
      method: ServerMethodDefinition<*, *>,
      channel: ManagedChannel,
      request: HttpRequest<RawRpcPayload>,
      principal: Principal?,
    ): GrpcWebCall = when (this) {
      // return a binary call
      GrpcWebContentType.BINARY -> Binary(
        config,
        service,
        method,
        channel,
        request,
        principal,
      )

      // return a text call
      GrpcWebContentType.TEXT -> Text(
        config,
        service,
        method,
        channel,
        request,
        principal,
      )
    }
  }

  // Internal mutable function to notify the call that a response has been received.
  @Synchronized internal fun notifyResponse(response: GrpcWebCallResponse): GrpcWebCall {
    if (finished()) {
      throw IllegalStateException(
        "Cannot provide more than one response to a given `GrpcWebCall` structure"
      )
    }
    this.response = response
    response.fill(httpResponse)
    return this
  }

  /** @return Whether this call has finished processing yet. */
  fun finished(): Boolean = response != null
}
