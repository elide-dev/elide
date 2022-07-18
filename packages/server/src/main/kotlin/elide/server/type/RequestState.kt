package elide.server.type

import io.micronaut.http.HttpRequest
import java.security.Principal

/**
 * Request state container which is passed to methods which need access to request state.
 *
 * @param request HTTP request bound to this request state.
 * @param principal Security principal detected for this request, or `null`.
 */
public data class RequestState(
  val request: HttpRequest<*>,
  val principal: Principal?,
)
