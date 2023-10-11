package elide.runtime.intrinsics.server.http.micronaut

import io.micronaut.context.ExecutionHandleLocator
import io.micronaut.http.HttpMethod
import io.micronaut.inject.MethodExecutionHandle
import io.micronaut.web.router.DefaultRouteBuilder
import io.micronaut.web.router.RouteBuilder.UriNamingStrategy
import elide.annotations.Singleton

/**
 * An implementation of [DefaultRouteBuilder] providing methods to build routes using
 * [lambda handles][LambdaExecutionHandle] instead of traditional class-backed execution handles.
 *
 * @see buildRoute
 */
@Singleton internal abstract class LambdaRouteBuilder(
  handleLocator: ExecutionHandleLocator,
  namingStrategy: UriNamingStrategy,
) : DefaultRouteBuilder(handleLocator, namingStrategy) {
  /** Build a route for the given [method] and [uri], using an anonymous execution [handle]. */
  @Suppress("unchecked_cast")
  protected fun <R> buildRoute(method: HttpMethod, uri: String, handle: LambdaExecutionHandle<R>) {
    super.buildRoute(method, uri, handle as MethodExecutionHandle<Any, Any>)
  }
}