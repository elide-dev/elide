package elide.server.rpc.web

import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.Metadata.Key

/** Provides constant values which are used internally by Elide's gRPC integration layer. */
@Suppress("unused") object GrpcWeb {
  /** Special header names. */
  object Headers {
    /** gRPC status header, used on a gRPC-Web response. */
    const val status = "grpc-status"

    /** Error message header, used on an exceptional gRPC-Web response. */
    const val errorMessage = "grpc-error"

    /** Indicates whether a given HTTP request is encoded for use with gRPC-web. */
    const val sentinel = "grpc-web"
  }

  /** Special metadata keys. */
  object Metadata {
    /** Key which is used to signify an internal gRPC Web call to the backing server. */
    val internalCall: Key<String> = Key.of(
      "x-grpc-web-internal",
      ASCII_STRING_MARSHALLER,
    )

    /** User-provided `authorization` header. */
    val authorization: Key<String> = Key.of(
      "authorization",
      ASCII_STRING_MARSHALLER,
    )

    /** User-provided `x-api-key` header. */
    val apiKey: Key<String> = Key.of(
      "x-api-key",
      ASCII_STRING_MARSHALLER,
    )

    /** Trace header. */
    val trace: Key<String> = Key.of(
      "x-trace",
      ASCII_STRING_MARSHALLER,
    )
  }

  /** Suffix applied to headers which should be considered with a binary value. */
  const val BINARY_HEADER_SUFFIX = "-bin"

  /** Prefix for gRPC-standard headers. */
  const val GRPC_HEADER_PREFIX = "x-grpc-"
}
