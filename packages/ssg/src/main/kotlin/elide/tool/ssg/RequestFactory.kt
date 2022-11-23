package elide.tool.ssg

import io.micronaut.http.HttpRequest
import tools.elide.meta.Endpoint
import kotlin.jvm.Throws

/**
 * # SSG: Request Factory
 *
 * This factory is responsible for synthesizing requests to an Elide app at build time, for the purpose of generating
 * static site packages.
 */
public interface RequestFactory {
  /** Specialized exception which is thrown if a request cannot be generated. */
  public class RequestGenerationFailed(public val endpoint: Endpoint, throwable: Throwable?): RuntimeException(
    "Failed to generate request for endpoint",
    throwable,
  )

  /**
   * Create a request for the provided [page] and [controller].
   *
   * @param page Page annotation affixed to the target endpoint.
   * @param controller Controller implementation to dispatch against.
   * @return Generated request.
   */
  @Throws(RequestGenerationFailed::class)
  public fun create(page: Endpoint, controller: Class<*>?): HttpRequest<*>

  /**
   * Create a request for the provided stateful [page] and [controller] with the provided [state].
   *
   * @param page Page annotation affixed to the target endpoint.
   * @param controller Controller implementation to dispatch against.
   * @param state Page state to enclose.
   * @return Generated request.
   */
  @Throws(RequestGenerationFailed::class)
  public fun <S> create(page: Endpoint, controller: Class<*>?, state: S?): HttpRequest<*> {
    TODO("Stateful SSG requests are not implemented yet.")
  }
}
