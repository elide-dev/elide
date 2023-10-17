package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.context.ExecutionHandleLocator
import io.micronaut.context.annotation.Requires
import io.micronaut.inject.MethodExecutionHandle
import io.micronaut.web.router.DefaultRouteBuilder
import io.micronaut.web.router.RouteBuilder.UriNamingStrategy
import elide.annotations.Inject
import elide.annotations.Singleton
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpMethod
import io.micronaut.http.HttpMethod as MicronautHttpMethod

/**
 * An implementation of [DefaultRouteBuilder] providing methods to build routes using
 * [lambda handles][LambdaExecutionHandle] instead of traditional class-backed execution handles.
 *
 * @see buildRoute
 */
@Requires(property = "elide.embedded")
@Singleton @DelicateElideApi internal class GuestRouteBuilder(
  handleLocator: ExecutionHandleLocator,
  namingStrategy: UriNamingStrategy,
) : DefaultRouteBuilder(handleLocator, namingStrategy) {
  private fun HttpMethod.toMicronautMethod(): MicronautHttpMethod {
    return MicronautHttpMethod.valueOf(name)
  }

  /** Build a route for the given [method] and [uri], using an anonymous execution [handle]. */
  @Suppress("unchecked_cast")
  private fun <R> buildRoute(method: MicronautHttpMethod, uri: String, handle: LambdaExecutionHandle<R>) {
    super.buildRoute(method, uri, handle as MethodExecutionHandle<Any, Any>)
  }

  @Inject fun buildGuestRoutes(router: MicronautGuestRouter) {
    router.routes.forEach { (method, uri, handle) -> buildRoute(method.toMicronautMethod(), uri, handle) }
  }
}