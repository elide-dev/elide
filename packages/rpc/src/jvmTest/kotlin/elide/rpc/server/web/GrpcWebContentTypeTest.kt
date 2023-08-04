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

import io.micronaut.http.MediaType
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Tests for [GrpcWebContentType] instances. */
class GrpcWebContentTypeTest {
  @Test fun testEncodeContentTypeSymbols() {
    assertEquals(
      "application/grpc-web",
      GrpcWebContentType.BINARY.symbol,
      "raw gRPC web binary content-type symbol should be correct"
    )
    assertEquals(
      "application/grpc-web-text",
      GrpcWebContentType.TEXT.symbol,
      "raw gRPC web text content-type symbol should be correct"
    )
    assertEquals(
      "application/grpc-web",
      GrpcWebContentType.BINARY.contentType(proto = false),
      "raw gRPC web binary content-type symbol should be correct (non-proto)"
    )
    assertEquals(
      "application/grpc-web+proto",
      GrpcWebContentType.BINARY.contentType(),
      "raw gRPC web binary content-type symbol should be correct (proto)"
    )
    assertEquals(
      "application/grpc-web-text",
      GrpcWebContentType.TEXT.contentType(proto = false),
      "raw gRPC web text content-type symbol should be correct (non-proto)"
    )
    assertEquals(
      "application/grpc-web-text+proto",
      GrpcWebContentType.TEXT.contentType(),
      "raw gRPC web text content-type symbol should be correct (proto)"
    )
  }

  @Test fun testAllValidContentTypes() {
    assertTrue(
      GrpcWebContentType.allValidContentTypes.contains(
        "application/grpc-web"
      ),
      "`application/grpc-web` should be considered a valid gRPC-web content type"
    )
    assertTrue(
      GrpcWebContentType.allValidContentTypes.contains(
        "application/grpc-web-text"
      ),
      "`application/grpc-web-text` should be considered a valid gRPC-web content type"
    )
    assertTrue(
      GrpcWebContentType.allValidContentTypes.contains(
        "application/grpc-web+proto"
      ),
      "`application/grpc-web+proto` should be considered a valid gRPC-web content type"
    )
    assertTrue(
      GrpcWebContentType.allValidContentTypes.contains(
        "application/grpc-web-text+proto"
      ),
      "`application/grpc-web-text+proto` should be considered a valid gRPC-web content type"
    )
    assertFalse(
      GrpcWebContentType.allValidContentTypes.contains(
        "application/grpc"
      ),
      "`application/grpc` should NOT be considered a valid gRPC-web content type"
    )
  }

  @Test fun testValidMediaType() {
    assertEquals(
      "application/grpc-web",
      GrpcWebContentType.BINARY.mediaType(proto = false).toString(),
      "gRPC web binary media-type should be correct (non-proto)"
    )
    assertEquals(
      "application/grpc-web+proto",
      GrpcWebContentType.BINARY.mediaType().toString(),
      "gRPC web binary media-type should be correct (proto)"
    )
    assertEquals(
      "application/grpc-web-text",
      GrpcWebContentType.TEXT.mediaType(proto = false).toString(),
      "gRPC web text media-type should be correct (non-proto)"
    )
    assertEquals(
      "application/grpc-web-text+proto",
      GrpcWebContentType.TEXT.mediaType().toString(),
      "gRPC web text media-type should be correct (proto)"
    )
  }

  @Test fun testResolveFromMediaType() {
    assertEquals(
      GrpcWebContentType.BINARY,
      GrpcWebContentType.resolve(GrpcWebContentType.BINARY.mediaType(proto = false)),
      "resolving a non-proto BINARY content type should work"
    )
    assertEquals(
      GrpcWebContentType.BINARY,
      GrpcWebContentType.resolve(GrpcWebContentType.BINARY.mediaType(proto = true)),
      "resolving a proto BINARY content type should work"
    )
    assertEquals(
      GrpcWebContentType.TEXT,
      GrpcWebContentType.resolve(GrpcWebContentType.TEXT.mediaType(proto = false)),
      "resolving a non-proto TEXT content type should work"
    )
    assertEquals(
      GrpcWebContentType.TEXT,
      GrpcWebContentType.resolve(GrpcWebContentType.TEXT.mediaType(proto = true)),
      "resolving a proto TEXT content type should work"
    )
  }

  @Test fun testResolveInvalidMediaType() {
    assertThrows<IllegalArgumentException> {
      GrpcWebContentType.resolve(
        MediaType("application/grpc")
      )
    }
    assertThrows<IllegalArgumentException> {
      GrpcWebContentType.resolve(
        MediaType("text/plain")
      )
    }
    assertThrows<IllegalArgumentException> {
      GrpcWebContentType.resolve(
        MediaType("application/json")
      )
    }
  }
}
