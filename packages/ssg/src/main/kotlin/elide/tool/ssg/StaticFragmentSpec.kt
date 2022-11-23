package elide.tool.ssg

import io.micronaut.http.HttpRequest
import tools.elide.meta.Endpoint

/**
 * Payload class for an expected site fragment.
 *
 * @param request Request generated for this endpoint.
 * @param endpoint Endpoint specification that generated this request.
 */
public data class StaticFragmentSpec(
  val request: HttpRequest<*>,
  val endpoint: Endpoint,
)
