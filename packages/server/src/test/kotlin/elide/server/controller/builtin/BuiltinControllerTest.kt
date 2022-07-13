package elide.server.controller.builtin

import elide.server.RawResponse
import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MediaType
import io.micronaut.http.MutableHttpRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
