package elide.rpc.server.web

import io.grpc.Metadata
import io.micronaut.core.type.Headers
import io.micronaut.core.type.MutableHeaders
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import elide.rpc.server.web.GrpcWeb.BINARY_HEADER_SUFFIX
import elide.rpc.server.web.GrpcWeb.GRPC_HEADER_PREFIX

/** Provides metadata-related declarations and tooling for gRPC and gRPC Web. */
public object MetadataUtil {
  // Excluded headers from conversion between gRPC-Web and HTTP.
  private val EXCLUDED: SortedSet<String> = sortedSetOf(
    "content-type",
    "grpc-accept-encoding",
    "grpc-encoding",
    "x-grpc-encoding",
    "x-grpc-web",
    "x-grpc-web-internal",
  )

  // Headers explicitly allow-listed for inclusion. Overrides all other considerations.
  private val INCLUDED: SortedSet<String> = sortedSetOf(
    "authorization",
    "x-api-key",
  )

  /**
   * Given a raw [name]/[value] pair which should be used as a trailer in a gRPC Web response, pack them together in a
   * manner consistent with the gRPC Web Protocol, and add them to the provided [stream].
   *
   * This method works on raw strings, see [packTrailers] for a method which works based on a full set of [Metadata].
   *
   * @param stream Byte stream which should receive the packed result.
   * @param name Name of the trailer which we should add to the [stream].
   * @param value Value of the trailer which we should add to the [stream].
   */
  @JvmStatic public fun packTrailer(stream: ByteArrayOutputStream, name: String, value: ByteArray) {
    stream.writeBytes(
      "${name.lowercase().trim()}:".toByteArray(StandardCharsets.UTF_8)
    )
    stream.writeBytes(
      value
    )
    stream.writeBytes(
      "\r\n".toByteArray(StandardCharsets.UTF_8)
    )
  }

  /**
   * Given a set of [trailers] as gRPC [Metadata], and the provided string [stream], pack the present set of trailers
   * into the response in a manner consistent with the gRPC Web Protocol.
   *
   * Trailers are packed at the end of a given response, formatted as a set of `key:value` pairs, with each pair
   * separated by `\r\n`. A special [RpcSymbol] denotes the `TRAILER` section ([RpcSymbol.TRAILER]), and separates it
   * from the `DATA` section ([RpcSymbol.DATA]).
   *
   * @param stream Byte stream which should receive the packed trailers.
   * @param trailers Set of trailers to pack into the provided [stream].
   */
  @JvmStatic public fun packTrailers(stream: ByteArrayOutputStream, trailers: Metadata) {
    val all = trailers.keys()
    all.forEach { name ->
      // determine whether we're dealing with a binary trailer
      val (metadataKey, rawValues) = if (name.endsWith(BINARY_HEADER_SUFFIX)) {
        val k = Metadata.Key.of(name, Metadata.BINARY_BYTE_MARSHALLER)
        val values = trailers.getAll(k)

        k to (values?.toList() ?: emptyList())
      } else {
        val k = Metadata.Key.of(
          name,
          Metadata.ASCII_STRING_MARSHALLER
        )
        val values = trailers.getAll(k)
        k to (values?.map { it.toByteArray(StandardCharsets.UTF_8) } ?: emptyList())
      }

      rawValues.forEach { rawValue ->
        packTrailer(
          stream,
          metadataKey.name(),
          rawValue
        )
      }
    }
  }

  /**
   * Given a set of [headers] from a generic HTTP or gRPC request, determine a corresponding set of gRPC call
   * [io.grpc.Metadata].
   *
   * @param headers Headers to decode into metadata.
   * @return gRPC metadata decoded from the provided [headers].
   */
  @JvmStatic public fun metadataFromHeaders(headers: Headers): Metadata {
    val metadata = Metadata()
    headers.filter {
      (
        INCLUDED.contains(it.key) ||
          (
            it.key.lowercase().startsWith(GRPC_HEADER_PREFIX) &&
            !EXCLUDED.contains(it.key)
          )
      )
    }.forEach { entry ->
      val (key, values) = entry
      values.forEach { value ->
        if (key.endsWith(BINARY_HEADER_SUFFIX)) {
          metadata.put(
            Metadata.Key.of(
              key,
              Metadata.BINARY_BYTE_MARSHALLER
            ),
            value.toByteArray(StandardCharsets.UTF_8)
          )
        } else {
          metadata.put(
            Metadata.Key.of(
              key,
              Metadata.ASCII_STRING_MARSHALLER
            ),
            value
          )
        }
      }
    }
    return metadata
  }

  /**
   * Given a set of gRPC [io.grpc.Metadata], compute a corresponding set of HTTP [Headers] and return them.
   *
   * @param metadata gRPC metadata to convert into HTTP headers.
   * @param target Headers which should receive the converted results.
   * @return HTTP headers from the provided [metadata].
   */
  @JvmStatic public fun fillHeadersFromMetadata(metadata: Metadata, target: MutableHeaders) {
    metadata.keys().forEach { key ->
      if (!INCLUDED.contains(key) && EXCLUDED.contains(key)) {
        return@forEach  // skip excluded headers
      } else {
        val isBinaryHeader = key.endsWith(BINARY_HEADER_SUFFIX)
        metadata.getAll(if (isBinaryHeader) {
          Metadata.Key.of(
            key,
            Metadata.BINARY_BYTE_MARSHALLER
          )
        } else {
          Metadata.Key.of(
            key,
            Metadata.ASCII_STRING_MARSHALLER
          )
        })!!.forEach {  value ->
          if (value is String) {
            target[key] = value
          } else if (value is ByteArray) {
            target[key] = String(value)
          }
        }
      }
    }
  }
}
