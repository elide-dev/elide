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
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpRequest
import io.micronaut.http.MutableHttpResponse
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.util.Optional
import kotlin.test.*
import elide.core.api.Symbolic
import elide.http.body.MicronautBody
import elide.http.headers.MicronautHttpHeaders
import elide.http.headers.MicronautMutableHttpHeaders
import elide.http.request.JavaNetHttpUri
import elide.http.request.MicronautMutableHttpRequest
import elide.testing.annotations.Test

internal typealias MicronautHttpTestBase = AbstractHttpTest<
  HttpRequest<*>,
  HttpResponse<*>,
  MicronautHttpHeaders,
  MicronautMutableHttpHeaders,
>

internal class MicronautHttpTest: MicronautHttpTestBase() {
  override val get: MutableHttpRequest<String> =
    HttpRequest.GET<String>(URI.create("http://localhost:8080/"))
  override val post: MutableHttpRequest<String> =
    HttpRequest.POST<String>(URI.create("http://localhost:8080/"), null)
  override val postWithBody: MutableHttpRequest<String> =
    HttpRequest.POST<String>(URI.create("http://localhost:8080/"), "hi")
  private val put = HttpRequest.PUT<String>(URI.create("http://localhost:8080/"), null)
  private val putWithBody = HttpRequest.PUT<String>(URI.create("http://localhost:8080/"), "hi")
  override val ok: MutableHttpResponse<String> = HttpResponse.ok<String>()
  override val ise: MutableHttpResponse<String> = HttpResponse.serverError<String>()

  override fun toReq(req: HttpRequest<*>): Request = Http.request(req)
  override fun toResp(resp: HttpResponse<*>): Response = Http.response(resp)

  override fun sampleHeadersImmutable(): MicronautHttpHeaders = MicronautHttpHeaders(
    HttpRequest.GET<String>(URI.create("http://localhost:8080/"))
      .header("sample", "Hello")
      .header("multi", "One")
      .apply {
        headers.add("multi", "Two")
      }.headers
  )

  override fun sampleHeadersMutable(): MicronautMutableHttpHeaders = MicronautMutableHttpHeaders(
    HttpRequest.GET<String>(URI.create("http://localhost:8080/"))
      .header("sample", "Hello")
      .header("multi", "One")
      .apply {
        headers.add("multi", "Two")
      }.headers
  )

  override fun sampleHeadersImmutableEmpty(): MicronautHttpHeaders = MicronautHttpHeaders.EMPTY
  override fun sampleHeadersMutableEmpty(): MicronautMutableHttpHeaders = MicronautMutableHttpHeaders.empty()

  @Test fun testCreateGetMutable() {
    val req = Http.request(get)
    val mut1 = MicronautMutableHttpRequest(get)
    assertNotNull(mut1)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    val mut = req.toMutable() as MicronautMutableHttpRequest<*>
    assertNotNull(mut)
    assertNotNull(mut.method)
    assertNotNull(mut.body)
    assertNotNull(mut.headers)
    assertNotNull(mut.version)
    assertNotNull(mut.url)
    assertIs<Body.Empty>(mut.body)
    assertEquals("GET", mut.method.asString())
    assertNotNull(mut.request)
    assertEquals("/", mut.request.path)
    mut.url = JavaNetHttpUri(URI.create("http://localhost:8080/hello"))
    assertEquals("/hello", mut.request.path)
    val sus = object: HttpUrl.PlatformHttpUrl<Any> {
      override val host: String
        get() = TODO("Not yet implemented")
      override val params: Params
        get() = TODO("Not yet implemented")
      override val path: String
        get() = TODO("Not yet implemented")
      override val port: UShort
        get() = TODO("Not yet implemented")
      override val scheme: String
        get() = TODO("Not yet implemented")
      override val value: Any
        get() = TODO("Not yet implemented")
    }
    assertThrows<IllegalStateException> { mut.url = sus }
  }

  @Test fun testMutableRequestBody() {
    val req = Http.request(get)
    assertNotNull(req)
    val mut = req.toMutable() as MicronautMutableHttpRequest<*>
    assertFalse(mut.body.isPresent)
    assertIs<Body.Empty>(mut.body)
    mut.body = MicronautBody(2uL, Optional.of("hi"))
    assertTrue(mut.body.isPresent)
    assertIs<MicronautBody<String>>(mut.body)
    mut.body = Body.Empty
    assertFalse(mut.body.isPresent)
    assertIs<Body.Empty>(mut.body)
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
    assertFalse(req.body.isPresent)
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
    assertIs<MicronautBody<String>>(req.body)
    val body = req.body
    assertIs<MicronautBody<String>>(body)
    assertTrue(body.isPresent)
  }

  @Test fun testCreatePut() {
    val req = Http.request(put)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    assertFalse(req.body.isPresent)
  }

  @Test fun testCreatePutWithBody() {
    val req = Http.request(putWithBody)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIsNot<Body.Empty>(req.body)
    assertIs<MicronautBody<String>>(req.body)
    val body = req.body
    assertIs<MicronautBody<String>>(body)
    assertTrue(body.isPresent)
  }

  @Test fun testCreateGetToMutable() {
    val req = Http.request(get)
    assertNotNull(req)
    assertNotNull(req.method)
    assertNotNull(req.body)
    assertNotNull(req.headers)
    assertNotNull(req.version)
    assertNotNull(req.url)
    assertIs<Body.Empty>(req.body)
    assertFalse(req.body.isPresent)
    assertEquals("GET", req.method.asString())
    assertFalse("Hello" in req.headers)
    val mut = req.toMutable()
    assertNotNull(mut)
    mut.headers["Hello"] = "World"
    assertEquals("World", mut.headers["Hello"]?.asString())
    assertTrue("Hello" in mut.headers)
    val built = mut.build()
    assertNotNull(built)
    assertNotNull(built.method)
    assertNotNull(built.body)
    assertNotNull(built.headers)
    assertNotNull(built.version)
    assertNotNull(built.url)
    assertIs<Body.Empty>(built.body)
    assertFalse(built.body.isPresent)
    assertEquals("World", built.headers["Hello"]?.asString())
  }

  @Test override fun testImmutableHeaders() {
    super.testImmutableHeaders()

    val sample =
      HttpRequest.GET<String>(URI.create("http://localhost:8080/"))
        .header("sample", "Hello")
        .header("multi", "One")
        .apply {
          headers.add("multi", "Two")
        }

    val headers = MicronautHttpHeaders(sample.headers)
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

    val sample =
      HttpRequest.GET<String>(URI.create("http://localhost:8080/"))
        .header("sample", "Hello")
        .header("multi", "One")
        .apply {
          headers.add("multi", "Two")
        }

    val headers = MicronautMutableHttpHeaders(sample.headers)
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
    assertSame(headers.headers, (mut as MicronautMutableHttpHeaders).headers)

    val immutable = mut.build()
    assertNotNull(immutable)
    assertNotSame(headers as Headers, immutable)

    val empty = MicronautMutableHttpHeaders.empty()
    assertNotNull(empty)
    assertEquals(0u, empty.size)
    assertEquals(0u, empty.sizeDistinct)

    val created = MicronautMutableHttpHeaders(get.headers)
    assertNotNull(created)
  }

  @Test fun testResolveNonStandardHeader() {
    assertDoesNotThrow { StandardHeader.resolve("accept") }
    assertThrows<Symbolic.Unresolved> { StandardHeader.resolve("non-standard") }
  }

  @Test fun testSingleHeaderValue() {
    val single = HeaderValue.single("sample")
    assertNotNull(single)
    assertEquals(1u, single.count)
    assertEquals("sample", single.values.first())
    assertEquals("sample", single.asString())
    assertEquals("sample", single.toString())
  }

  @Test fun testMultiHeaderValue() {
    val multi = HeaderValue.multi(listOf("One", "Two"))
    assertNotNull(multi)
    assertEquals(2u, multi.count)
    assertEquals("One", multi.values.first())
    assertEquals("Two", multi.values.last())
    assertEquals("One, Two", multi.toString())
    assertEquals("One, Two", multi.asString())
  }

  @Test fun testMultiSequenceHeaderValue() {
    val multi = HeaderValue.multi(sequenceOf("One", "Two"))
    assertNotNull(multi)
    assertEquals(2u, multi.count)
    assertEquals("One", multi.values.first())
    assertEquals("Two", multi.values.last())
    assertEquals("One, Two", multi.toString())
    assertEquals("One, Two", multi.asString())
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
