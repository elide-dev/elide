package elide.tool.ssg

import io.micronaut.http.HttpRequest
import tools.elide.meta.Endpoint
import java.net.URL
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

  /**
   * Create a request for the provided [artifact], detected during a page load.
   *
   * @param artifact Artifact which was detected, and for which a request should be generated.
   * @param spec Static fragment specification which generated this artifact request.
   * @return Request generated to fetch the artifact.
   */
  @Throws(SSGCompilerError::class)
  public fun create(spec: StaticFragmentSpec, artifact: DetectedArtifact): HttpRequest<*>

  /**
   * Create a request for the provided [path].
   *
   * @param base Base URL for this request.
   * @param path URI the request should fetch.
   * @return Request generated to fetch the artifact.
   */
  @Throws(SSGCompilerError::class)
  public fun create(base: URL, path: String): HttpRequest<*>
}
