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

import io.netty.handler.codec.http.DefaultFullHttpRequest
import io.netty.handler.codec.http.DefaultHttpRequest
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import java.nio.charset.StandardCharsets
import kotlin.test.*
import elide.http.headers.NettyHttpHeaders
import elide.http.headers.NettyMutableHttpHeaders
import elide.http.request.NettyHttpRequest
import elide.http.request.NettyMutableHttpRequest

internal typealias NettyHttpTestBase = AbstractHttpTest<
  HttpRequest,
  HttpResponse,
  NettyHttpHeaders,
  NettyMutableHttpHeaders,
>

internal class NettyHttpTest : NettyHttpTestBase() {
  override val get = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/")
  private val getHttp10 = DefaultHttpRequest(HttpVersion.HTTP_1_0, HttpMethod.GET, "http://localhost:8080/")
  override val post = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "http://localhost:8080/")
  override val postWithBody = DefaultFullHttpRequest(
    HttpVersion.HTTP_1_1,
    HttpMethod.POST,
    "http://localhost:8080/",
  ).apply {
    headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain")
    content().writeCharSequence("Hello, World!", StandardCharsets.UTF_8)
  }

  override val ok: DefaultHttpResponse = DefaultHttpResponse(
    HttpVersion.HTTP_1_1,
    HttpResponseStatus.OK,
  )

  override val ise: DefaultHttpResponse = DefaultHttpResponse(
    HttpVersion.HTTP_1_1,
    HttpResponseStatus.INTERNAL_SERVER_ERROR,
  )


  override fun toReq(req: HttpRequest): Request = Http.request(req)
  override fun toResp(resp: HttpResponse): Response = Http.response(resp)

  override fun sampleHeadersImmutable(): NettyHttpHeaders = NettyHttpHeaders(
    DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/").apply {
      headers().add("sample", "Hello")
      headers().add("multi", "One")
      headers().add("multi", "Two")
    }.headers()
  )

  override fun sampleHeadersMutable(): NettyMutableHttpHeaders = NettyMutableHttpHeaders(
    DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/").apply {
      headers().add("sample", "Hello")
      headers().add("multi", "One")
      headers().add("multi", "Two")
    }.headers()
  )

  override fun sampleHeadersImmutableEmpty(): NettyHttpHeaders = NettyHttpHeaders.EMPTY
  override fun sampleHeadersMutableEmpty(): NettyMutableHttpHeaders = NettyMutableHttpHeaders.empty()

  @Test override fun testCreateGet() {
    super.testCreateGet()
    val req = Http.request(get)
    val mut = req.toMutable()
    assertNotNull(mut)
    assertNotNull(mut.method)
    assertNotNull(mut.body)
    assertNotNull(mut.headers)
    assertNotNull(mut.version)
    assertNotNull(mut.url)
    assertIs<Body.Empty>(mut.body)
    assertEquals("GET", mut.method.symbol)
    assertIs<NettyMutableHttpRequest>(mut)
    assertNotNull(mut.request)
    assertEquals(ProtocolVersion.HTTP_1_1, mut.version)
    assertEquals(req.method, mut.method)
    assertEquals(req.url, mut.url)
    assertEquals(req.headers.size, mut.headers.size)
    assertEquals(req.version, mut.version)
    assertEquals(req.body, mut.body)
  }

  @Test fun testCreateGetHttp10() {
    val req = Http.request(getHttp10)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    assertEquals("GET", req.method.symbol)
    assertIs<NettyHttpRequest>(req)
    assertNotNull(req.request)
    assertEquals(ProtocolVersion.HTTP_1_0, req.version)
  }

  @Test override fun testCreatePost() {
    super.testCreatePost()
    val req = Http.request(post)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    assertEquals("POST", req.method.symbol)
    assertIs<NettyHttpRequest>(req)
    assertNotNull(req.request)
    assertEquals(ProtocolVersion.HTTP_1_1, req.version)
  }

  @Test override fun testCreatePostWithBody() {
    super.testCreatePostWithBody()
    val req = Http.request(postWithBody)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIsNot<Body.Empty>(req.body)
    assertEquals("POST", req.method.symbol)
    assertIs<NettyHttpRequest>(req)
    assertNotNull(req.request)
    assertEquals(ProtocolVersion.HTTP_1_1, req.version)
  }

  @Test override fun testImmutableHeaders() {
    super.testImmutableHeaders()
    val sample = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/").apply {
      headers().add("sample", "Hello")
      headers().add("multi", "One")
      headers().add("multi", "Two")
    }

    val headers = NettyHttpHeaders(sample.headers())
    assertNotEquals(0u, headers.size)
    assertNotEquals(0u, headers.sizeDistinct)
    assertEquals(3u, headers.size)
    assertEquals(2u, headers.sizeDistinct)
    assertTrue("sample" in headers)
    assertEquals("Hello", headers["sample"]?.asString())
    assertNull(headers["another"])
    assertEquals("Hello", headers["sample"]?.values?.first())
    assertNull(headers["another"]?.values?.first())

    val headerSample = HeaderName.of("sample")
    val headerMulti = HeaderName.of("multi")
    assertTrue(headerSample in headers)
    assertEquals("Hello", headers[headerSample]?.asString())
    val headerAnother = HeaderName.of("another")
    assertFalse(headerAnother in headers)
    assertNull(headers[headerAnother])
    assertNotNull(headers.headers)
    assertEquals("Hello", headers.first(headerSample))
    assertNull(headers.first(headerAnother))
    assertEquals("Hello", headers.first("sample"))
    assertNull(headers.first("another"))
    assertNotNull(headers.asOrdered())

    val multiValue = headers[headerMulti]
    assertNotNull(multiValue)
    assertIs<HeaderValue.MultiHeaderValue>(multiValue)
    assertEquals(2, multiValue.values.count())
    assertEquals("One", multiValue.values.first())
    assertEquals("Two", multiValue.values.last())

    val orderedList = headers.asOrdered().toList()
    assertEquals(2, orderedList.size)
    assertEquals("sample", orderedList[0].name.name)

    val asMap = headers.asMap()
    assertEquals(2, asMap.size)
    assertEquals("Hello", asMap[headerSample]?.asString())
    assertEquals("One", asMap[headerMulti]?.values?.first())

    val asRawMap = headers.asRawMap()
    assertEquals(2, asRawMap.size)
    assertEquals("Hello", asRawMap["sample"]?.first())
    assertEquals("One", asRawMap["multi"]?.first())
    assertEquals("Two", asRawMap["multi"]?.last())

    val mut = headers.toMutable()
    assertNotNull(mut)
    assertNotSame(headers as Headers, mut as Headers)
  }

  @Test override fun testMutableHeaders() {
    super.testMutableHeaders()

    val sample = DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "http://localhost:8080/").apply {
      headers().add("sample", "Hello")
      headers().add("multi", "One")
      headers().add("multi", "Two")
    }

    val headers = NettyMutableHttpHeaders(sample.headers())
    assertNotEquals(0u, headers.size)
    assertNotEquals(0u, headers.sizeDistinct)
    assertEquals(3u, headers.size)
    assertEquals(2u, headers.sizeDistinct)
    assertTrue("sample" in headers)
    assertEquals("Hello", headers["sample"]?.asString())
    assertNull(headers["another"])
    assertEquals("Hello", headers["sample"]?.values?.first())
    assertNull(headers["another"]?.values?.first())

    val headerSample = HeaderName.of("sample")
    val headerMulti = HeaderName.of("multi")
    assertTrue(headerSample in headers)
    assertEquals("Hello", headers[headerSample]?.asString())
    val headerAnother = HeaderName.of("another")
    assertFalse(headerAnother in headers)
    assertNull(headers[headerAnother])
    assertNotNull(headers.headers)
    assertEquals("Hello", headers.first(headerSample))
    assertNull(headers.first(headerAnother))
    assertEquals("Hello", headers.first("sample"))
    assertNull(headers.first("another"))
    assertNotNull(headers.asOrdered())

    val multiValue = headers[headerMulti]
    assertNotNull(multiValue)
    assertIs<HeaderValue.MultiHeaderValue>(multiValue)
    assertEquals(2, multiValue.values.count())
    assertEquals("One", multiValue.values.first())
    assertEquals("Two", multiValue.values.last())

    val orderedList = headers.asOrdered().toList()
    assertEquals(2, orderedList.size)
    assertEquals("sample", orderedList[0].name.name)

    val asMap = headers.asMap()
    assertEquals(2, asMap.size)
    assertEquals("Hello", asMap[headerSample]?.asString())
    assertEquals("One", asMap[headerMulti]?.values?.first())

    val asRawMap = headers.asRawMap()
    assertEquals(2, asRawMap.size)
    assertEquals("Hello", asRawMap["sample"]?.first())
    assertEquals("One", asRawMap["multi"]?.first())
    assertEquals("Two", asRawMap["multi"]?.last())

    val mut = headers.toMutable()
    assertNotNull(mut)
    assertSame(headers.headers, (mut as NettyMutableHttpHeaders).headers)

    val immutable = mut.build()
    assertNotNull(immutable)
    assertNotSame(headers as Headers, immutable)
  }

  @Test fun testRequestExtensionFns() {
    val req = toReq(get)
    assertNotNull(req)
    val other = get.toUniversalHttpRequest()
    assertNotNull(other)
    assertNotNull(other.method)
    assertNotNull(other.body)
    assertNotNull(other.headers)
    assertNotNull(other.version)
    assertNotNull(other.url)
    assertIs<Body.Empty>(other.body)
  }

  @Test fun testResponseExtensionFns() {
    val resp = toResp(ok)
    assertNotNull(resp)
    val other = ok.toUniversalHttpResponse()
    assertNotNull(other)
    assertNotNull(other.body)
    assertNotNull(other.headers)
    assertNotNull(other.version)
    assertNotNull(other.status)
    assertIs<Body.Empty>(other.body)
  }
}
