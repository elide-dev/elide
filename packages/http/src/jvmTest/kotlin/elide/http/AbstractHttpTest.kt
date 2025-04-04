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

import elide.http.body.NettyBody
import elide.http.body.PrimitiveBody
import elide.http.request.JavaNetHttpUri
import elide.http.response.PlatformHttpResponse
import io.netty.buffer.ByteBufAllocator
import org.junit.jupiter.api.assertDoesNotThrow
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlin.test.*

abstract class AbstractHttpTest<Req, Resp, H: Headers, HMut: MutableHeaders> {
  abstract val get: Req
  abstract val post: Req
  abstract val postWithBody: Req
  abstract val ok: Resp
  abstract val ise: Resp

  abstract fun toReq(req: Req): Request
  abstract fun toResp(resp: Resp): Response

  abstract fun sampleHeadersImmutable(): H
  abstract fun sampleHeadersMutable(): HMut
  abstract fun sampleHeadersImmutableEmpty(): H
  abstract fun sampleHeadersMutableEmpty(): HMut

  @Test open fun testCreateGet() {
    val req = toReq(get)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    assertEquals("GET", req.method.symbol)
    assertIs<Request>(req)
    assertEquals(ProtocolVersion.HTTP_1_1, req.version)
    assertEquals(Message.Type.REQUEST, req.type)
    val mut = req.toMutable()
    val mut2 = mut.toMutable()
    assertSame(mut, mut2)
    assertNotNull(mut)
    assertEquals(Message.Type.REQUEST, mut.type)
    assertNotNull(mut.method)
    assertNotNull(mut.body)
    assertNotNull(mut.headers)
    assertNotNull(mut.version)
    assertNotNull(mut.url)
    assertIs<Body.Empty>(mut.body)
    assertFalse(mut.body.isPresent)
    assertEquals("GET", mut.method.symbol)
    assertIs<MutableRequest>(mut)
    assertEquals(ProtocolVersion.HTTP_1_1, mut.version)
    assertEquals(req.method, mut.method)
    assertEquals(req.url, mut.url)
    assertEquals(req.headers.size, mut.headers.size)
    assertEquals(req.version, mut.version)
    assertEquals(req.body, mut.body)
  }

  @Test open fun testCreatePost() {
    val req = toReq(post)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertFalse(req.body.isPresent)
    assertIs<Body.Empty>(req.body)
    assertEquals("POST", req.method.symbol)
    assertIs<Request>(req)
    assertEquals(ProtocolVersion.HTTP_1_1, req.version)
  }

  @Test open fun testCreatePostWithBody() {
    val req = toReq(postWithBody)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIsNot<Body.Empty>(req.body)
    assertTrue(req.body.isPresent)
    assertEquals("POST", req.method.symbol)
    assertIs<Request>(req)
    assertEquals(ProtocolVersion.HTTP_1_1, req.version)
  }

  @Test open fun testImmutableHeaders() {
    val headers = sampleHeadersImmutable()
    assertNotNull(headers)
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

    val asSequence = headers.asSequence().toList()
    assertEquals(2, asSequence.count())
    assertTrue("sample" in asSequence.map { it.name.name })
    assertTrue("sample" in asSequence.map { it.name.nameNormalized })

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

  @Test open fun testMutableHeaders() {
    val headers = sampleHeadersMutable()
    assertNotNull(headers)
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

    val asSequence = headers.asSequence()
    assertEquals(2, asSequence.count())
    assertTrue("sample" in asSequence.map { it.name.nameNormalized }.toList())

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

    // remove a single header by name
    val sampleHeader = HeaderName.of("sample")
    mut.remove("sample")
    assertFalse("sample" in mut)
    assertFalse(sampleHeader in mut)
    assertEquals(2u, mut.size)
    assertEquals(1u, mut.sizeDistinct)

    // now put it back
    mut["sample"] = "Hello"
    assertTrue("sample" in mut)
    assertTrue(sampleHeader in mut)
    assertEquals(3u, mut.size)
    assertEquals(2u, mut.sizeDistinct)

    // remove a header by type
    mut.remove(sampleHeader)
    assertFalse("sample" in mut)
    assertFalse(sampleHeader in mut)
    assertEquals(2u, mut.size)
    assertEquals(1u, mut.sizeDistinct)
    assertNull(mut[sampleHeader])

    // put it back
    mut[sampleHeader] = HeaderValue.single("Hello")
    assertTrue("sample" in mut)
    assertTrue(sampleHeader in mut)
    assertEquals(3u, mut.size)
    assertEquals(2u, mut.sizeDistinct)

    // remove by value
    mut.remove("sample", "Hello")
    assertFalse("sample" in mut)
    assertFalse(sampleHeader in mut)
    assertEquals(2u, mut.size)
    assertEquals(1u, mut.sizeDistinct)

    // put it back
    mut[sampleHeader] = HeaderValue.single("Hello")
    assertTrue("sample" in mut)
    assertTrue(sampleHeader in mut)
    assertEquals(3u, mut.size)
    assertEquals(2u, mut.sizeDistinct)

    // remove by value typed
    mut.remove(Header.of(sampleHeader, HeaderValue.single("Hello")))
    assertFalse("sample" in mut)
    assertFalse(sampleHeader in mut)
    assertEquals(2u, mut.size)
    assertEquals(1u, mut.sizeDistinct)

    // put it back
    mut[sampleHeader] = HeaderValue.single("Hello")
    assertTrue("sample" in mut)
    assertTrue(sampleHeader in mut)
    assertEquals(3u, mut.size)
    assertEquals(2u, mut.sizeDistinct)

    // remove a multi-header by name
    val multiHeader = HeaderName.of("multi")
    mut.remove("multi")
    assertFalse("multi" in mut)
    assertFalse(multiHeader in mut)
    assertEquals(1u, mut.size)
    assertEquals(1u, mut.sizeDistinct)
    assertNull(mut[multiHeader])
    assertNull(mut["multi"])

    // put it back
    mut[multiHeader] = HeaderValue.multi("One", "Two")
    assertTrue("multi" in mut)
    assertTrue(multiHeader in mut)
    assertEquals(3u, mut.size)
    assertEquals(2u, mut.sizeDistinct)

    val immutable = mut.build()
    assertNotNull(immutable)
    assertNotSame(headers as Headers, immutable)
  }

  @Test open fun testMutableHeadersOverwrite() {
    val headers1 = sampleHeadersMutable()
    val headers2 = sampleHeadersMutable()
    assertTrue("sample" in headers1)
    assertFalse("Not-Found" in headers1)
    assertEquals("Hello", headers1["sample"]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1["sample"] = "World"
    assertEquals("World", headers1["sample"]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertTrue("sample" in headers2)
    assertTrue("multi" in headers2)
    assertFalse("Not-Found" in headers2)
    assertEquals(3u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("Hello", headers2["sample"]?.asString())
    assertEquals("One", headers2["multi"]?.values?.first())
    assertEquals("Two", headers2["multi"]?.values?.last())
    headers2["multi"] = "Three"
    assertEquals(2u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("Three", headers2["multi"]?.asString())
    assertEquals("Three", headers2["multi"]?.values?.first())
    assertEquals("Three", headers2["multi"]?.values?.last())
  }

  @Test open fun testMutableHeadersOverwriteTyped() {
    val headers1 = sampleHeadersMutable()
    val sample = HeaderName.of("sample")
    val multi = HeaderName.of("multi")
    val notFound = HeaderName.of("not-found")
    assertTrue(sample in headers1)
    assertFalse(notFound in headers1)
    assertTrue(multi in headers1)
    assertEquals("Hello", headers1[sample]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1.set(Header.of(sample, HeaderValue.single("World")))
    assertEquals("World", headers1["sample"]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertEquals("World", headers1["sample"]?.values?.first())
    assertEquals("World", headers1["sample"]?.values?.last())
    assertTrue(multi in headers1)
    headers1.set(Header.of(multi, HeaderValue.single("Three")))
    assertEquals(2u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertEquals("Three", headers1["multi"]?.asString())
    assertEquals("Three", headers1["multi"]?.values?.first())
    assertEquals("Three", headers1["multi"]?.values?.last())
    headers1.set(Header.of(multi, HeaderValue.multi("Four", "Five")))
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertEquals("Four, Five", headers1["multi"]?.asString())
    assertEquals("Four", headers1["multi"]?.values?.first())
    assertEquals("Five", headers1["multi"]?.values?.last())
  }

  @Test fun testMutableHeadersAppendFromZero() {
    val headers1 = sampleHeadersMutable()
    val headers2 = sampleHeadersMutable()
    assertTrue("sample" in headers1)
    assertFalse("another" in headers1)
    assertEquals("Hello", headers1["sample"]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1.append("another", "one")
    assertEquals("one", headers1["another"]?.asString())
    assertEquals(4u, headers1.size)
    assertEquals(3u, headers1.sizeDistinct)
    assertEquals(3u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
  }

  @Test fun testMutableHeadersAppendFromZeroTyped() {
    val headers1 = sampleHeadersMutable()
    val headers2 = sampleHeadersMutable()
    val sample = HeaderName.of("sample")
    val another = HeaderName.of("another")
    assertTrue(sample in headers1)
    assertFalse(another in headers1)
    assertEquals("Hello", headers1[sample]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1.append(Header.of(another, HeaderValue.single("one")))
    assertEquals("one", headers1["another"]?.asString())
    assertEquals(4u, headers1.size)
    assertEquals(3u, headers1.sizeDistinct)
    assertEquals(3u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
  }

  @Test open fun testMutableHeadersAppend() {
    val headers1 = sampleHeadersMutable()
    val headers2 = sampleHeadersMutable()
    assertTrue("sample" in headers1)
    assertFalse("Not-Found" in headers1)
    assertEquals("Hello", headers1["sample"]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1.append("sample", "World")
    assertEquals("Hello, World", headers1["sample"]?.asString())
    assertEquals(4u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertEquals("Hello", headers1["sample"]?.values?.first())
    assertEquals("World", headers1["sample"]?.values?.last())
    assertTrue("sample" in headers2)
    assertTrue("multi" in headers2)
    assertFalse("Not-Found" in headers2)
    assertEquals(3u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("Hello", headers2["sample"]?.asString())
    assertEquals("One", headers2["multi"]?.values?.first())
    assertEquals("Two", headers2["multi"]?.values?.last())
    headers2.append("multi", "Three")
    assertEquals(4u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("One, Two, Three", headers2["multi"]?.asString())
    assertEquals("One", headers2["multi"]?.values?.first())
    assertEquals("Three", headers2["multi"]?.values?.last())
  }

  @Test open fun testMutableHeadersAppendTyped() {
    val headers1 = sampleHeadersMutable()
    val headers2 = sampleHeadersMutable()
    val sample = HeaderName.of("sample")
    val multi = HeaderName.of("multi")
    val notFound = HeaderName.of("not-found")
    assertTrue(sample in headers1)
    assertFalse(notFound in headers1)
    assertTrue(multi in headers1)
    assertFalse(notFound in headers2)
    assertEquals("Hello", headers1[sample]?.asString())
    assertEquals(3u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    headers1.append(Header.of(sample, HeaderValue.single("World")))
    assertEquals("Hello, World", headers1["sample"]?.asString())
    assertEquals(4u, headers1.size)
    assertEquals(2u, headers1.sizeDistinct)
    assertEquals("Hello", headers1["sample"]?.values?.first())
    assertEquals("World", headers1["sample"]?.values?.last())
    assertTrue("sample" in headers2)
    assertTrue("multi" in headers2)
    assertFalse("not-found" in headers2)
    assertEquals(3u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("Hello", headers2["sample"]?.asString())
    assertEquals("One", headers2["multi"]?.values?.first())
    assertEquals("Two", headers2["multi"]?.values?.last())
    headers2.append("multi", "Three")
    assertEquals(4u, headers2.size)
    assertEquals(2u, headers2.sizeDistinct)
    assertEquals("One, Two, Three", headers2["multi"]?.asString())
    assertEquals("One", headers2["multi"]?.values?.first())
    assertEquals("Three", headers2["multi"]?.values?.last())
  }

  @Test fun testCreateOk() {
    val res = toResp(ok)
    assertNotNull(res)
    assertNotNull(res.body)
    assertNotNull(res.headers)
    assertNotNull(res.version)
    assertNotNull(res.status)
    assertEquals(Message.Type.RESPONSE, res.type)
    assertIs<Body.Empty>(res.body)
    assertEquals(200u, res.status.code.symbol)
    if (res is PlatformHttpResponse<*>) {
      assertNotNull(res.response)
      assertDoesNotThrow { res.trailers }
    }
    val mut = res.toMutable()
    val mut2 = mut.toMutable()
    assertSame(mut, mut2)
    assertNotNull(mut)
    assertEquals(Message.Type.RESPONSE, mut.type)
    assertNotNull(mut.body)
    assertNotNull(mut.headers)
    assertNotNull(mut.version)
    assertNotNull(mut.status)
    assertIs<Body.Empty>(mut.body)
    assertEquals(200u, mut.status.code.symbol)
  }

  @Test fun testMutableRequest() {
    val req = toReq(get)
    val mut = req.toMutable()
    val mut2 = mut.toMutable()
    assertSame(mut, mut2)
    mut.headers["another"] = "one"
    assertTrue("another" in mut.headers)
    assertEquals("one", mut.headers["another"]?.asString())
    mut.headers.remove("another")
    assertFalse("another" in mut.headers)
    val req2 = mut.build()
    assertNotNull(req2)
    assertNotNull(req2.body)
    assertNotNull(req2.headers)
    assertNotNull(req2.version)
    assertNotNull(req2.url)
    assertNotNull(req2.method)
  }

  @Test open fun testMutableResponse() {
    val res = toResp(ok)
    val mut = res.toMutable()
    val mut2 = mut.toMutable()
    assertSame(mut, mut2)
    mut.headers["another"] = "one"
    assertTrue("another" in mut.headers)
    assertEquals("one", mut.headers["another"]?.asString())
    mut.headers.remove("another")
    assertFalse("another" in mut.headers)
    mut.status = Status.of(StatusCode.resolve(404u))
    assertEquals(404u, mut.status.code.symbol)
    mut.body = Body.Empty
    val bytebuf = ByteBufAllocator.DEFAULT.buffer()
    bytebuf.writeBytes("sample".toByteArray(StandardCharsets.UTF_8))
    mut.body = NettyBody(bytebuf)
    mut.body = PrimitiveBody.bytes("sample".toByteArray(StandardCharsets.UTF_8))
    mut.body = PrimitiveBody.of("sample")
    val res2 = mut.build()
    assertNotNull(res2)
    assertNotNull(res2.body)
    assertNotNull(res2.headers)
    assertNotNull(res2.version)
    assertNotNull(res2.status)
    assertIsNot<Body.Empty>(res2.body)
    assertEquals(404u, res2.status.code.symbol)
    assertTrue(res2.body.isPresent)
  }

  @Test fun testCreateServerError() {
    val res = toResp(ise)
    assertNotNull(res)
    assertNotNull(res.body)
    assertNotNull(res.headers)
    assertNotNull(res.version)
    assertNotNull(res.status)
    assertIs<Body.Empty>(res.body)
    assertEquals(500u, res.status.code.symbol)
    val mut = res.toMutable()
    val mut2 = mut.toMutable()
    assertNotNull(mut)
    assertSame(mut, mut2)
  }


  @Test fun testHttpUrl() {
    val req = toReq(get)
    assertNotNull(req)
    assertNotNull(req.url)
    val url = req.url
    assertNotNull(url.scheme)
    assertNotNull(url.host)
    assertNotNull(url.port)
    assertNotNull(url.path)
    assertNotNull(url.params)
    assertNotNull(JavaNetHttpUri(URI.create("http://localhost:8080/")))
  }

  @Test fun testHttpUrlParams() {
    val urlWithParams = URI.create("http://localhost:8080/?a=1&b=2&c=3&d=4&d=5")
    val req = toReq(get)
    assertNotNull(req)
    assertNotNull(req.url)
    val url = req.url
    assertNotNull(url.scheme)
    assertNotNull(url.host)
    assertNotNull(url.port)
    assertNotNull(url.path)
    assertNotNull(url.params)
    assertIs<Params.Empty>(url.params)
    val emptyParams = url.params
    assertNotNull(emptyParams)
    assertIs<Params.Empty>(emptyParams)
    assertEquals(0u, emptyParams.size)
    assertEquals(0u, emptyParams.sizeDistinct)
    assertFalse("a" in emptyParams)
    assertFalse("b" in emptyParams)
    assertFalse("c" in emptyParams)
    assertFalse("d" in emptyParams)
    assertFalse(ParamName.of("a") in emptyParams)
    assertNotNull(Params.Empty.toString())
    assertNotNull(Params.Empty)
    assertNull(emptyParams["a"])
    assertNull(emptyParams[ParamName.of("b")])
    assertNotNull(JavaNetHttpUri(urlWithParams))
    val wrapped = JavaNetHttpUri(urlWithParams)
    assertNotNull(wrapped)
    assertEquals("http", wrapped.scheme)
    assertEquals("localhost", wrapped.host)
    assertEquals(8080u, wrapped.port)
    assertEquals("/", wrapped.path)
    val params = wrapped.params
    assertNotNull(params)
    assertIsNot<Params.Empty>(params)
    assertEquals(5u, params.size)
    assertEquals(4u, params.sizeDistinct)
    assertTrue("a" in params)
    assertTrue("b" in params)
    assertTrue("c" in params)
    assertTrue("d" in params)
    assertFalse("e" in params)
    val paramA = ParamName.of("a")
    val paramB = ParamName.of("b")
    val paramE = ParamName.of("e")
    assertTrue(paramA in params)
    assertTrue(paramB in params)
    assertFalse(paramE in params)
    assertEquals("1", params[paramA]?.asString())
    assertEquals("2", params[paramB]?.asString())
    assertEquals("1", params["a"]?.asString())
    assertEquals("2", params["b"]?.asString())
  }

  @Test fun testParamName() {
    val name = ParamName.of("a")
    assertNotNull(name)
    assertNotNull(name.toString())
    assertNotNull(name.asString())
    assertEquals("a", name.name)
    assertEquals(name, ParamName.of("a"))
  }
}
