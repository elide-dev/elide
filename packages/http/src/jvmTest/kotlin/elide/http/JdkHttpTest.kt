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

import org.junit.jupiter.api.assertThrows
import java.net.URI
import java.net.http.HttpHeaders
import elide.http.headers.JavaNetHttpHeaders
import elide.http.headers.JavaNetMutableHttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertIsNot
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import elide.http.body.MicronautBody
import elide.http.body.PublisherBody
import elide.http.response.JdkHttp

internal typealias JdkHttpTestBase = AbstractHttpTest<
  HttpRequest,
  HttpResponse<*>,
  JavaNetHttpHeaders,
  JavaNetMutableHttpHeaders,
>

internal class JdkHttpTest: JdkHttpTestBase() {
  private val root = URI.create("http://localhost:8080/")
  override val get: HttpRequest get() = HttpRequest.newBuilder(root)
    .GET()
    .uri(root)
    .build()

  override val post: HttpRequest get() = HttpRequest.newBuilder(root)
    .POST(HttpRequest.BodyPublishers.noBody())
    .uri(root)
    .build()

  override val postWithBody: HttpRequest get() = HttpRequest.newBuilder(root)
    .POST(HttpRequest.BodyPublishers.ofString("Hello, World!"))
    .uri(root)
    .build()

  override val ok: HttpResponse<String> get() = JdkHttp.builder()
    .build()

  override val ise: HttpResponse<String> get() = JdkHttp.builder()
    .apply {
      statusCode = 500
    }
    .build()

  override fun toReq(req: HttpRequest): Request = Http.request(req)
  override fun toResp(resp: HttpResponse<*>): Response = Http.response(resp)

  override fun sampleHeadersImmutable(): JavaNetHttpHeaders = JavaNetHttpHeaders(
    HttpRequest.newBuilder(root)
      .GET()
      .header("sample", "Hello")
      .header("multi", "One")
      .header("multi", "Two")
      .build()
      .headers()
  )

  override fun sampleHeadersMutable(): JavaNetMutableHttpHeaders = JavaNetMutableHttpHeaders(
    HttpRequest.newBuilder(root)
      .GET()
      .header("sample", "Hello")
      .header("multi", "One")
      .header("multi", "Two")
      .build()
      .headers()
  )

  override fun sampleHeadersImmutableEmpty(): JavaNetHttpHeaders = JavaNetHttpHeaders.EMPTY
  override fun sampleHeadersMutableEmpty(): JavaNetMutableHttpHeaders = JavaNetMutableHttpHeaders.empty()

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

  @Test fun testHeaderFactories() {
    assertNotNull(JavaNetHttpHeaders.EMPTY)
    assertNotNull(JavaNetMutableHttpHeaders.empty())
    assertNotNull(JavaNetHttpHeaders(get.headers()))
    assertNotNull(JavaNetMutableHttpHeaders(get.headers()))
    val headers = JavaNetHttpHeaders(get.headers())
    assertNotNull(headers.headers)
    val mut = headers.toMutable()
    assertNotNull(mut.build())
  }

  @Test override fun testMutableResponse() {
    super.testMutableResponse()
    val req = toReq(get)
    assertNotNull(req)
    val mut = req.toMutable()
    assertNotNull(mut)
    assertNotNull(mut.method)
    assertNotNull(mut.body)
    assertNotNull(mut.headers)
    assertNotNull(mut.version)
    assertNotNull(mut.url)
    assertIs<Body.Empty>(mut.body)
    assertIs<JavaNetMutableHttpHeaders>(mut.headers)
    mut.headers["foo"] = "bar"
    assertNotNull(mut.headers["foo"])
    assertTrue("foo" in mut.headers)
    mut.headers.remove("foo")
    assertTrue("foo" !in mut.headers)
    assertNull(mut.headers["foo"])
    mut.body = PublisherBody(
      HttpRequest.BodyPublishers.ofString("sample")
    )
    assertIs<PublisherBody>(mut.body)
    assertThrows<IllegalStateException> {
      mut.body = MicronautBody.string("test")
    }
    assertIsNot<Body.Empty>(mut.body)
  }

  @Test fun testJdkResponse() {
    val headerMap = mapOf(
      "sample" to listOf("Hello"),
      "multi" to listOf("One", "Two"),
    )
    val response = Response.of(
      ProtocolVersion.HTTP_1_1,
      Status.Ok,
      JavaNetHttpHeaders(HttpHeaders.of(headerMap, alwaysTruePredicate)),
      null,
      Body.Empty,
    )
    assertNotNull(response)
    val jdkResponse = JdkHttp.builder(response.status.code)
      .apply {
        headers.putAll(headerMap)
      }
      .build<Any>()
    assertNotNull(jdkResponse)
    assertIs<JdkHttp.JdkResponse<*>>(jdkResponse)
    assertNull(jdkResponse.uri())
    assertNotNull(jdkResponse.previousResponse())
    assertNotNull(jdkResponse.sslSession())
    assertFalse(jdkResponse.previousResponse().isPresent)
    assertFalse(jdkResponse.sslSession().isPresent)
  }
}
