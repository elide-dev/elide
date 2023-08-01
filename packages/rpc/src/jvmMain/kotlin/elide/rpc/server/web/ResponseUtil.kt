package elide.rpc.server.web

import io.grpc.Metadata
import io.grpc.Status
import io.micronaut.http.MutableHttpResponse
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*

/** Provides static utilities used internally to prepare compliant gRPC-Web responses. */
internal object ResponseUtil {
  // Trailers which are included with every response, and indicated in the `Trailer` header.
  private val baseTrailers = sortedSetOf(
    "grpc-status",
    "grpc-message",
  )

  // Headers included on every response.
  private val baseHeaders = sortedMapOf(
    "trailer" to baseTrailers.joinToString(","),
    "transfer-encoding" to "chunked",
  )

  /**
   * Given a [stream] of pending output data which will be converted to gRPC Web format, write the provided [data] as a
   * chunk of the provided [type].
   *
   * The data is written using [MessageFramer] to enclose the expected prefix symbol.
   *
   * @param stream Data stream which we should write the payload to.
   * @param data Payload data which we should write to the stream, prefixed via [MessageFramer].
   * @param type Type of data being written to the stream.
   */
  @JvmStatic private fun writeRawData(
    stream: ByteArrayOutputStream,
    data: ByteArray,
    type: RpcSymbol,
  ) {
    // generate prefixed stanza
    val prefix = MessageFramer.getPrefix(
      data,
      type,
    )

    // if instructed, base64-wrap the payload
    stream.writeBytes(prefix.plus(data))
  }

  /**
   * Given a [stream] of pending output data which will be converted to gRPC Web format, write the provided [status] and
   * set of [trailers] as a [RpcSymbol.TRAILER] chunk.
   *
   * The trailers, including the terminal [status], are written via [MetadataUtil.packTrailer].
   *
   * @param stream Data stream which we should write the computed set of trailers to.
   * @param status Terminal status for the call, which we should write as a [GrpcWeb.Headers.status] trailer.
   * @param trailers Set of extra trailers to write with the status trailer.
   */
  @JvmStatic private fun writeTrailer(stream: ByteArrayOutputStream, status: Status, trailers: Metadata) {
    val trailerStream = ByteArrayOutputStream()
    trailerStream.use { target ->
      // pack any extra trailers first
      MetadataUtil.packTrailers(
        target,
        trailers,
      )

      // finally, pack the status trailer
      MetadataUtil.packTrailer(
        target,
        GrpcWeb.Headers.status,
        status.code.value().toString().toByteArray(StandardCharsets.UTF_8),
      )
    }

    writeRawData(
      stream,
      trailerStream.toByteArray(),
      RpcSymbol.TRAILER,
    )
  }

  /**
   * Given a [response] which we are preparing in gRPC Web format, write any baseline headers included on all responses,
   * plus any [headers] provided as extra metadata, plus any content-type headers for the provided [contentType].
   *
   * @param response HTTP response which we are preparing in gRPC Web format.
   * @param contentType gRPC Web content type which applies to this response.
   * @param headers Headers which should be written in the response.
   */
  @JvmStatic fun writeHeaders(
    response: MutableHttpResponse<RawRpcPayload>,
    contentType: GrpcWebContentType,
    headers: Metadata,
  ) {
    baseHeaders.forEach {
      response.headers.set(it.key, it.value)
    }
    MetadataUtil.fillHeadersFromMetadata(
      headers,
      response.headers,
    )
    response.contentType(
      contentType.mediaType(),
    )
  }

  /**
   * Prepare the provided [response] as a protocol-compliant gRP Web response with the provided [contentType].
   *
   * Headers provided via [headers] are provided as regular HTTP headers. The provided terminal [status] and set of
   * extra [trailers] are then written in encoded form as part of the response body, and wrapped with the provided
   * [payload], if any.
   *
   * If no [payload] is provided, an empty byte array is sent, effectively creating a trailers-only response which is
   * allowed by spec.
   *
   * @param contentType gRPC Web content type which applies to this response.
   * @param response HTTP response which we are preparing.
   * @param status Terminal status for the call, which is either [Status.OK] or an error.
   * @param headers Set of headers (gRPC [Metadata]) to provide as HTTP response headers.
   * @param trailers Set of values (gRPC [Metadata]) to provide as arbitrary extra response trailers.
   * @param payload Response payload data to enclose, as applicable. If no payload is provided, an empty byte array is
   *   sent, effectively creating a trailers-only response.
   */
  @JvmStatic fun writeResponse(
    contentType: GrpcWebContentType,
    response: MutableHttpResponse<RawRpcPayload>,
    status: Status,
    headers: Metadata,
    trailers: Metadata,
    payload: ByteArray,
  ) {
    // add headers
    writeHeaders(response, contentType, headers)

    // begin a response stream
    val responseStream = ByteArrayOutputStream()
    responseStream.use { stream ->
      // write payload body first, headers are already in. if there is no payload, add an empty byte array, which is
      // allowed by spec and signifies a trailers-only response.
      writeRawData(
        stream,
        payload,
        RpcSymbol.DATA,
      )

      // finally, write any trailer portion of the body.
      writeTrailer(
        stream,
        status,
        trailers,
      )
    }

    // collect the stream in the final response, wrapping the entire thing in Base64 if we're operating in `TEXT` mode.
    response.body(
      if (contentType == GrpcWebContentType.TEXT) {
      Base64.getEncoder().encode(
        responseStream.toByteArray(),
      )
    } else {
      responseStream.toByteArray()
    },
    )
  }
}
