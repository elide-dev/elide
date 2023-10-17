package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.http.HttpAttributes
import io.micronaut.web.router.UriRouteMatch
import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpMethod
import elide.runtime.intrinsics.server.http.HttpRequest
import io.micronaut.http.HttpRequest as MicronautRequest


/** [HttpRequest] implementation wrapping a Micronaut request object. */
@DelicateElideApi internal class MicronautHttpRequest(private val request: MicronautRequest<*>) : HttpRequest {
  @get:Export override val uri: String get() = request.path
  @get:Export override val method: HttpMethod get() = HttpMethod.valueOf(request.methodName)
  @get:Export override val params: Map<String, Any> = buildMap {
    // include path variables if available
    request.getAttribute(HttpAttributes.ROUTE_MATCH, UriRouteMatch::class.java).ifPresent { match ->
      match.variableValues.forEach { (key, value) -> put(key, value) }
    }

    // include query parameters
    request.parameters.forEach { put(it.key, it.value) }
  }
}
