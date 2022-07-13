package elide.server.controller.builtin

import io.micronaut.http.HttpMethod
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpRequest
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import kotlin.test.Test
import kotlin.test.assertNotNull

/** Contract tests for [ServerErrorController]. */
@MicronautTest class ServerErrorControllerTest : BuiltinControllerTest<ServerErrorController>() {
  @Inject lateinit var controller: ServerErrorController

  @Test fun testInjectable() {
    assertNotNull(controller)
  }

  override fun controller(): ServerErrorController {
    return controller
  }

  override fun getRequestTemplate(method: HttpMethod): MutableHttpRequest<Any> {
    return HttpRequest.create(method, "/error/internal")
  }
}
