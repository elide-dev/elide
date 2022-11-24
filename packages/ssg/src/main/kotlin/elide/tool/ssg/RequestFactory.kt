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
  /**
   * Create a request for the provided [page] and [controller].
   *
   * @param page Page annotation affixed to the target endpoint.
   * @param controller Controller implementation to dispatch against.
   * @return Generated request.
   */
  @Throws(SSGCompilerError::class)
  public fun create(page: Endpoint, controller: Class<*>?): HttpRequest<*>
}
