package elide.tool.ssg

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import tools.elide.meta.Endpoint
import java.nio.ByteBuffer

/**
 * Internal payload for a single static site fragment, associated with the HTTP request that produced it.
 *
 * @param request HTTP request which produced this site fragment.
 * @param endpoint HTTP endpoint specification for this site fragment.
 * @param response HTTP response produced for this fragment.
 * @param content Parsed data which should be saved as file content.
 * @param discovered Set of additionally-discovered transitive static fragment specs to resolve.
 */
public data class StaticFragment(
  val request: HttpRequest<*>,
  val endpoint: Endpoint,
  val response: HttpResponse<*>,
  val content: ByteBuffer,
  val discovered: List<StaticFragmentSpec>,
)
