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
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.*

class HttpPrintableTest {
  private val get = HttpRequest.GET<String>("http://localhost:8080/")
  private val post = HttpRequest.POST<String>("http://localhost:8080/post", null)
  private val ok = HttpResponse.ok("hi")
  private val ise = HttpResponse.serverError<String>()

  private fun assertPrintedRequest(req: Request, builder: StringBuilder) {
    assertNotNull(req)
    assertNotNull(builder)
    val expectedVersion = req.version.symbol
    val expectedMethod = req.method.symbol
    val expectedUrl = req.url
    val rendered = builder.toString()
    assertNotNull(rendered)
    assertTrue(rendered.isNotEmpty())
    assertTrue(rendered.isNotBlank())
    assertTrue(rendered.startsWith(expectedVersion))
    assertTrue(expectedMethod in rendered)
    assertTrue(expectedUrl.path in rendered)
  }

  private fun assertPrintedResponse(req: Response, builder: StringBuilder) {
    assertNotNull(req)
    assertNotNull(builder)
    val rendered = builder.toString()
    assertNotNull(rendered)
    assertTrue(rendered.isNotEmpty())
    assertTrue(rendered.isNotBlank())
  }

  @Test fun testPrintGet() {
    val req = Http.request(get)
    assertNotNull(req)
    assertNotNull(req.toString())
    assertDoesNotThrow { req.renderToHttp() }
      .also { assertPrintedRequest(req, it) }
  }

  @Test fun testPrintPost() {
    val req = Http.request(post)
    assertNotNull(req)
    assertNotNull(req.toString())
    assertDoesNotThrow { req.renderToHttp() }
      .also { assertPrintedRequest(req, it) }
  }

  @Test fun testPrintOk() {
    val res = Http.response(ok)
    assertNotNull(res)
    assertNotNull(res.toString())
    assertDoesNotThrow { res.renderToHttp() }
      .also { assertPrintedResponse(res, it) }
  }

  @Test fun testPrintIse() {
    val res = Http.response(ise)
    assertNotNull(res)
    assertNotNull(res.toString())
    assertDoesNotThrow { res.renderToHttp() }
      .also { assertPrintedResponse(res, it) }
  }
}
