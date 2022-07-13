package elide.server.http

import elide.server.assets.AssetManager
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Inject
import org.reactivestreams.Publisher

/**
 * Provides an [HttpServerFilter] which affixes context values at known keys in [HttpRequest]s processed by Elide apps;
 * known keys are defined via [RequestContext.Key].
 *
 * @see RequestContext.Key for an exhaustive review of available request context.
 */
@Filter("/**") public class RequestContextFilter : HttpServerFilter {
  @Inject internal lateinit var assetManager: AssetManager

  /** @inheritDoc */
  override fun doFilter(request: HttpRequest<*>, chain: ServerFilterChain): Publisher<MutableHttpResponse<*>> {
    request.setAttribute(
      RequestContext.Key.ASSET_MANAGER.name,
      assetManager
    )
    return chain.proceed(request)
  }
}
