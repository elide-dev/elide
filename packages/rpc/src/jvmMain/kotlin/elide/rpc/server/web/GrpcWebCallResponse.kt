package elide.rpc.server.web

import io.grpc.Metadata
import io.grpc.Status
import io.micronaut.http.MutableHttpResponse

/**
 * Describes the possible states that a gRPC Web call outcome can take on, namely an error state and a success state
 * which provides one or more response payloads.
 *
 * This class works in concert with [GrpcWebCall] to track the status of calls to the backing gRPC server. If a
 * [GrpcWebCall] has a [GrpcWebCallResponse] object present, the call has completed processing.
 *
 * @param success Indicates whether the call was encountered an error, in which case the value will be `false`, and the
 *   implementation will be [Error], or a successful response, in which case the value will be `true` and the
 *   implementation will be an instance of [UnaryResponse].
 */
@Suppress("ArrayInDataClass")
public sealed class GrpcWebCallResponse(
  public val success: Boolean,
) {
  /**
   * Response structure which carries information for a gRPC Web call which encountered a fatal error, including the
   * ultimate [status] and any [cause] information.
   *
   * Space for [trailers] are provided, but using error trailers is completely optional.
   *
   * @param status Terminal status which should be assigned to this error state. Defaults to [Status.INTERNAL].
   * @param contentType Content type to use for this response. Defaults to [GrpcWebContentType.BINARY].
   * @param cause Cause information for this error, if known. Defaults to `null`.
   * @param headers Header metadata which was captured as part of this response cycle.
   * @param trailers Response trailers to enclose which describe this error state, if available.
   */
  public data class Error(
    val contentType: GrpcWebContentType,
    val status: Status,
    val cause: Throwable?,
    val headers: Metadata,
    val trailers: Metadata,
  ): GrpcWebCallResponse(success = false) {
    /** @inheritDoc */
    override fun fill(response: MutableHttpResponse<RawRpcPayload>) {
      ResponseUtil.writeResponse(
        contentType,
        response,
        status,
        headers,
        trailers,
        ByteArray(0),
      )
    }
  }

  /**
   * Response structure which carries information for a gRPC Web call which completed and yielded one or more response
   * [payload] structures, along with any extra [trailers] that should be sent.
   *
   * No status is accepted because it is assumed to be [Status.OK]. For all other statuses, use an [Error].
   *
   * @param payload Serialized payload data which should be enclosed in the response.
   * @param headers Header metadata which was captured as part of this response cycle.
   * @param trailers Response trailers to enclose within the response payload sent to the invoking client.
   */
  public data class UnaryResponse(
    val contentType: GrpcWebContentType,
    val payload: ByteArray,
    val headers: Metadata,
    val trailers: Metadata,
  ): GrpcWebCallResponse(success = true) {
    /** @inheritDoc */
    override fun fill(response: MutableHttpResponse<RawRpcPayload>) {
      ResponseUtil.writeResponse(
        contentType,
        response,
        Status.OK,
        headers,
        trailers,
        payload,
      )
    }
  }

  /**
   * Fill out the provided HTTP [response] with data attached to this call response state; the response is expected to
   * comply with the structure stipulated by the
   * [gRPC Web Protocol](https://github.com/grpc/grpc/blob/master/doc/PROTOCOL-WEB.md) document.
   *
   * @param response Mutable response to fill with information based on this response.
   */
  public abstract fun fill(response: MutableHttpResponse<RawRpcPayload>)
}
