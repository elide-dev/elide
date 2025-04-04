/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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

package elide.http

import io.micronaut.http.HttpRequest
import io.netty.buffer.ByteBufAllocator
import org.junit.jupiter.api.assertThrows
import java.net.http.HttpRequest.BodyPublishers
import java.nio.charset.StandardCharsets
import kotlin.test.*
import elide.http.body.HttpBody
import elide.http.body.MicronautBody
import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.body.PublisherBody

class HttpBodyTest {
  fun bodyTests(body: Body?) {
    assertNotNull(body)
    assertNotNull(body.toString())
    if (body is Body.SizedBody) {
      assertNotEquals(0uL, body.contentLength)
      assertTrue(body.isPresent)
    } else if (body is Body.Empty) {
      assertFalse(body.isPresent)
    }
  }

  @Test fun testEmptyBody() {
    bodyTests(Body.Empty)
  }

  @Test fun testMicronautBody() {
    val basic = MicronautBody.string("sample")
    val other = HttpBody.micronaut<String>("sample".length.toULong(), "sample")
    assertNotNull(basic)
    assertEquals("sample", basic.unwrap())
    assertEquals("sample".length.toULong(), basic.contentLength)
    assertNotNull(other)
    bodyTests(basic)
    bodyTests(other)
    val empty = MicronautBody.of(HttpRequest.GET<String>("http://localhost:8080/"))
    assertIs<Body.Empty>(empty)
    assertFalse(empty.isPresent)
  }

  @Test fun testPrimitiveBodyString() {
    val str = PrimitiveBody.string("sample")
    val other = HttpBody.string("sample")
    val alt = PrimitiveBody.of("sample")
    assertEquals(str, other)
    assertEquals(str.unwrap(), other.unwrap())
    assertNotNull(str)
    assertNotNull(other)
    assertNotNull(str.contentLength)
    assertNotNull(str.value)
    assertTrue(str.isPresent)
    assertEquals("sample", str.unwrap())
    assertEquals("sample".length.toULong(), str.contentLength)
    bodyTests(str)
    bodyTests(other)
    bodyTests(alt)
  }

  @Test fun testPrimitiveBodyBytes() {
    val bytes = PrimitiveBody.bytes("sample".toByteArray(StandardCharsets.UTF_8))
    val other = HttpBody.bytes("sample".toByteArray(StandardCharsets.UTF_8))
    val alt = PrimitiveBody.of("sample".toByteArray(StandardCharsets.UTF_8))
    assertNotNull(bytes)
    assertNotNull(other)
    assertNotNull(bytes.contentLength)
    assertNotNull(bytes.value)
    assertTrue(bytes.isPresent)
    assertEquals("sample".length.toULong(), bytes.contentLength)
    assertContentEquals("sample".toByteArray(StandardCharsets.UTF_8), bytes.unwrap())
    assertEquals("sample".length.toULong(), bytes.contentLength)
    bodyTests(bytes)
    bodyTests(other)
    bodyTests(alt)
  }

  @Test fun testPrimitiveBodyInvalid() {
    assertThrows<IllegalStateException> { PrimitiveBody.of(5) }
  }

  @Test fun testNettyBody() {
    val bytes = "sample".toByteArray(StandardCharsets.UTF_8)
    val buf = ByteBufAllocator.DEFAULT.heapBuffer(bytes.size)
    buf.writeBytes(bytes)
    buf.capacity(buf.readableBytes())
    val netty = NettyBody(buf)
    val other = HttpBody.netty(buf)
    assertNotNull(netty)
    assertNotNull(other)
    assertContentEquals(bytes, netty.unwrap().array().sliceArray(0 until bytes.size))
    assertEquals(bytes.size.toULong(), netty.contentLength)
    bodyTests(netty)
    bodyTests(other)
  }

  @Test fun testPublisherBody() {
    val simple = PublisherBody(BodyPublishers.ofString("test"))
    assertNotNull(simple)
    bodyTests(simple)
  }
}
