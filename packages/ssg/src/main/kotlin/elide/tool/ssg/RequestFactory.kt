package elide.tool.ssg

import elide.server.annotations.Page
import elide.server.controller.PageController
import elide.server.controller.PageWithProps
import io.micronaut.http.HttpRequest


/**
 *
 */
@Suppress("UNUSED_PARAMETER", "unused")
class RequestFactory {
  /**
   *
   */
  suspend fun create(page: Page, controller: PageController): HttpRequest<*> {
    TODO("not yet implemented")
  }

  /**
   *
   */
  suspend fun <S> create(page: Page, controller: PageWithProps<S>, state: S): HttpRequest<*> {
    TODO("not yet implemented")
  }
}
