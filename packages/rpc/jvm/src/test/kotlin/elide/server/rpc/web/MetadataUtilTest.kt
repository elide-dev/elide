package elide.server.rpc.web

import io.grpc.Metadata
import io.micronaut.http.HttpResponse
import java.nio.charset.StandardCharsets
import kotlin.test.*

/** Tests for [MetadataUtil]. */
class MetadataUtilTest {
  @Test fun testConvertHeadersToMetadata() {
    val headers = HttpResponse.ok<String>().headers
    headers.add("content-type", "something/blahblah")
    headers.add("grpc-encoding", "some-encoding")
    headers.add("x-grpc-encoding", "some-encoding")
    headers.add("x-grpc-something-bin", "something-binary")
    headers.add("some-header", "123")
    headers.add("x-grpc-some-header", "123")
    val metadata = MetadataUtil.metadataFromHeaders(headers)
    assertNotNull(
      metadata,
      "converted metadata from headers should not be `null`"
    )
    val found = metadata.get(
      Metadata.Key.of(
        "some-header",
        Metadata.ASCII_STRING_MARSHALLER
      )
    )
    assertNull(
      found,
      "should not find non-grpc header in converted headers from metadata"
    )
    val found2 = metadata.get(
      Metadata.Key.of(
        "x-grpc-some-header",
        Metadata.ASCII_STRING_MARSHALLER
      )
    )
    assertNotNull(
      found2,
      "grpc-related header should be copied into grpc metadata"
    )
    assertEquals(
      "123",
      found2,
      "grpc header values should be preserved"
    )
    assertNull(
      metadata.get(Metadata.Key.of(
        "content-type",
        Metadata.ASCII_STRING_MARSHALLER
      )),
      "excluded headers should not get copied into metadata"
    )
    assertNull(
      metadata.get(Metadata.Key.of(
        "grpc-encoding",
        Metadata.ASCII_STRING_MARSHALLER
      )),
      "excluded headers should not get copied into metadata, even if prefixed"
    )
    assertNull(
      metadata.get(Metadata.Key.of(
        "x-grpc-encoding",
        Metadata.ASCII_STRING_MARSHALLER
      )),
      "excluded headers should not get copied into metadata, even if prefixed"
    )
    assertNotNull(
      metadata.get(
        Metadata.Key.of(
          "x-grpc-something-bin",
          Metadata.BINARY_BYTE_MARSHALLER
        )
      ),
      "grpc binary header should be handled correctly by metadata copy"
    )
  }

  @Test fun testConvertMetadataToHeaders() {
    val metadata = Metadata()
    metadata.put(
      Metadata.Key.of(
        "some-header",
        Metadata.ASCII_STRING_MARSHALLER
      ),
      "123"
    )
    metadata.put(
      Metadata.Key.of(
        "some-binary-header${Metadata.BINARY_HEADER_SUFFIX}",
        Metadata.BINARY_BYTE_MARSHALLER
      ),
      "123".toByteArray(StandardCharsets.UTF_8)
    )
    val excludedHeader = "content-type"
    metadata.put(
      Metadata.Key.of(
        excludedHeader,
        Metadata.ASCII_STRING_MARSHALLER
      ),
      "application/grpc"
    )
    val headers = HttpResponse.ok<String>().headers
    MetadataUtil.fillHeadersFromMetadata(
      metadata,
      headers
    )
    assertEquals(
      "123",
      headers["some-header"],
      "all headers should be preserved for gRPC response (unless excluded)"
    )
    assertEquals(
      "123",
      headers["some-binary-header${Metadata.BINARY_HEADER_SUFFIX}"],
      "binary headers should be properly suffixed"
    )
    assertFalse(
      headers.contains(excludedHeader),
      "excluded headers should not be present in converted HTTP headers"
    )
  }
}
