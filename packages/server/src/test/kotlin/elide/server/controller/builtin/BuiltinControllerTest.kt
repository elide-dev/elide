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

package elide.server.controller.builtin

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpRequest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import elide.server.RawResponse

/** Tests for built-in controllers, which handle HTML, text, and JSON. */
abstract class BuiltinControllerTest<C : BuiltinController> {
  /**
   * Spawn a request to test this built-in controller.
   *
   * @param method HTTP method for which a request is needed.
   * @return Request template to use. It will be mutated into different tests.
   */
  abstract fun getRequestTemplate(method: HttpMethod = HttpMethod.GET): MutableHttpRequest<Any>

  /**
   * @return Controller instance to test against.
   */
  abstract fun controller(): C

  /**
   * Execute the provided [request] against the built-in controller.
   *
   * @param controller Controller instance to test against.
   * @param request Request to run against the controller.
   */
  open suspend fun executeRequest(controller: C, request: HttpRequest<Any>): RawResponse {
    return controller.handle(request)
  }

  private fun builtinResponseAssertions(response: RawResponse?, mediaType: MediaType? = null) {
    assertNotNull(response)
    if (mediaType != null) {
      assertEquals(mediaType, response.contentType.get())
    }
  }

  @CsvSource(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML, MediaType.TEXT_PLAIN)
  @ParameterizedTest fun testRespondsToContentTypes(accept: MediaType) {
    val controller = controller()
    assertNotNull(controller)
    val request = getRequestTemplate()
    val response = runBlocking {
      executeRequest(controller, request.accept(accept))
    }
    builtinResponseAssertions(
      response,
      accept
    )
  }

  @CsvSource("GET", "PUT", "POST", "DELETE", "OPTIONS")
  @ParameterizedTest fun testRespondsToMethods(method: String) {
    val methodTarget = HttpMethod.valueOf(method)
    val controller = controller()
    assertNotNull(controller)
    val request = getRequestTemplate(methodTarget)
    val response = runBlocking {
      executeRequest(controller, request)
    }
    builtinResponseAssertions(
      response,
    )
  }
}
