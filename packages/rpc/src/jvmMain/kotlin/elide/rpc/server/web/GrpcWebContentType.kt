package elide.rpc.server.web

import io.micronaut.http.MediaType

/**
 * Describes the content types available for use with gRPC Web, including their corresponding HTTP symbols.
 *
 * @param symbol HTTP `Content-Type` value corresponding to this format.
 */
public enum class GrpcWebContentType constructor (internal val symbol: String) {
  /**
   * Binary dispatch for gRPC-Web, potentially with Protocol Buffers.
   *
   * @see contentType For calculation of a full `Content-Type` header.
   */
  BINARY("application/grpc-web"),

  /**
   * Text (base64-encoded) dispatch for gRPC-Web, potentially with Protocol Buffers.
   *
   * @see contentType For calculation of a full `Content-Type` header.
   */
  TEXT("application/grpc-web-text");

  public companion object {
    // All valid `Content-Type` values for gRPC-Web.
    @JvmStatic internal val allValidContentTypes: Array<String> = listOf(
      BINARY.contentType(false),
      BINARY.contentType(true),
      TEXT.contentType(false),
      TEXT.contentType(true),
    ).toTypedArray()

    // Resolve a `GrpcWebContentType` from the provided HTTP `MediaType`, or throw.
    @JvmStatic internal fun resolve(contentType: MediaType): GrpcWebContentType = when (
      contentType.toString().trim().lowercase()
    ) {
      "application/grpc-web+proto",
      "application/grpc-web" -> BINARY
      "application/grpc-web-text+proto",
      "application/grpc-web-text" -> TEXT
      else -> throw IllegalArgumentException("Cannot resolve invalid `Content-Type` for gRPC-Web: '$contentType'")
    }
  }

  /**
   * Render an HTTP `Content-Type` string for the selected format with consideration made for use of [proto]col buffers.
   *
   * @param proto Whether protocol buffers are in use.
   * @return `Content-Type` string to use.
   */
  public fun contentType(proto: Boolean = true): String {
    return if (proto) {
      "$symbol+proto"
    } else {
      this.symbol
    }
  }

  /**
   * Render a Micronaut [MediaType] for the selected format with consideration made for the use of [proto]col buffers.
   *
   * @param proto Whether protocol buffers are in use.
   * @return Micronaut [MediaType] to use.
   */
  public fun mediaType(proto: Boolean = true): MediaType {
    return MediaType(
      contentType(proto)
    )
  }
}
