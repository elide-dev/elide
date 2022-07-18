package elide.server.controller.builtin

import elide.server.RawResponse
import elide.server.controller.BaseController
import elide.server.controller.PageController
import elide.server.controller.StatusEnabledController
import elide.server.runtime.jvm.UncaughtExceptionHandler
import io.micronaut.http.HttpRequest

/**
 * Base class for built-in controllers provided by Elide.
 *
 * "Built-in" controllers are mounted within the application context by default, and handle events like global
 * `404 Not Found` and upstream call failures.
 *
 * ### Built-in controllers
 *
 * Each built-in controller operates at the default `@Singleton` scope, and complies with [StatusEnabledController]. As
 * such, state tied to individual requests is not allowed on built-in controllers unless proper synchronization is used.
 *
 * Users can replace built-in controllers via Micronaut annotations. See below for more.
 *
 * ### Overriding built-in controllers
 *
 * To override a built-in controller, implement [BaseController] and annotate your class as follows:
 * ```kotlin
 * @Controller
 * @Replaces(SomeBuiltinController::class)
 * class MyController {
 *   // ...
 * }
 * ```
 *
 * ### Default built-in controllers
 *
 * The following built-in controllers are provided by the framework by default:
 * - [NotFoundController]: handles `404 Not Found` events by rendering a designated HTML template.
 * - [ServerErrorController]: handles generic `500 Internal Server Error` events via a designated HTML template.
 *
 * ### Low-level error handler
 *
 * General/low-level error handling is provided at the executor level by [UncaughtExceptionHandler], which can also be
 * customized / replaced via the same mechanism shown above. See docs on that class for more info.
 *
 * @see NotFoundController for the built-in controller which handles `404 Not Found` events.
 * @see ServerErrorController for the built-in controller which handles generic internal error events.
 * @see UncaughtExceptionHandler for customizable background error handling logic.
 */
public abstract class BuiltinController : StatusEnabledController, PageController() {
  /**
   * Handles a request to this built-in controller.
   *
   * To perform I/O or any other blocking task, this method should suspend against the appropriate dispatcher.
   *
   * @param request Subject request to handle.
   * @return HTTP response which should be returned in response to the provided [request].
   */
  public abstract suspend fun handle(request: HttpRequest<out Any>): RawResponse
}
