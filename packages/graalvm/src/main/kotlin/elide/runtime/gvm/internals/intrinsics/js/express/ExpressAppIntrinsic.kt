package elide.runtime.gvm.internals.intrinsics.js.express

import elide.vm.annotations.Polyglot
import elide.runtime.intrinsics.js.express.ExpressApp
import io.netty.handler.codec.http.HttpMethod
import org.graalvm.polyglot.Value
import org.reactivestreams.Publisher
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.http.server.HttpServerResponse
import reactor.netty.http.server.HttpServerRoutes

/**
 * An [ExpressApp] implemented as a wrapper around a Reactor Netty server.
 *
 * A server builder is used internally and can be configured by methods like [get]. Calling [listen] will cause the
 * server to be built and bound to the specified port.
 */
internal class ExpressAppIntrinsic(private val context: ExpressContext) : ExpressApp {
  /** Represents a route handler registered by a guest script. */
  private data class RouteHandler(
    val method: HttpMethod,
    val path: String,
    val handler: Value,
  )

  /** Internal collection of route handlers requested by guest scripts. */
  private val routeRegistry = mutableSetOf<RouteHandler>()

  @Polyglot override fun get(path: String, handler: Value) = registerRouteHandler(HttpMethod.GET, path, handler)
  @Polyglot override fun post(path: String, handler: Value) = registerRouteHandler(HttpMethod.POST, path, handler)
  @Polyglot override fun put(path: String, handler: Value) = registerRouteHandler(HttpMethod.PUT, path, handler)
  @Polyglot override fun delete(path: String, handler: Value) = registerRouteHandler(HttpMethod.DELETE, path, handler)
  @Polyglot override fun head(path: String, handler: Value) = registerRouteHandler(HttpMethod.HEAD, path, handler)
  @Polyglot override fun options(path: String, handler: Value) = registerRouteHandler(HttpMethod.OPTIONS, path, handler)

  @Polyglot override fun listen(port: Int, callback: Value?) {
    // configure all the route handlers, set the port and bind the socket
    HttpServer.create().route { routes ->
      for(route in routeRegistry) route.install(routes) { handler, req, res ->
        // construct the wrappers
        val request = ExpressRequestIntrinsic(req)
        val response = ExpressResponseIntrinsic(res)

        // invoke the JS handler
        useCallback(handler) {
          executeVoid(request, response)
        }

        // return the internal publisher handled by the wrapper
        response.end()
      }
    }.port(port).bindNow()

    // prevent the JVM from exiting while the server is running
    context.pin()

    // notify listeners
    useCallback(callback) { executeVoid() }
  }

  private fun registerRouteHandler( method: HttpMethod, path: String, handler: Value) {
    routeRegistry.add(RouteHandler(method, mapExpressToReactorRoute(path), handler))
  }

  private inline fun RouteHandler.install(
    routes: HttpServerRoutes,
    crossinline wrapper: (Value, HttpServerRequest, HttpServerResponse) -> Publisher<Void>
  ) {
    // Reactor Netty does not have a generic route registration method, so we need to select the function manually
    when(method) {
      HttpMethod.GET -> routes.get(path) { req, res -> wrapper(handler, req, res) }
      HttpMethod.POST -> routes.post(path) { req, res -> wrapper(handler, req, res) }
      HttpMethod.PUT -> routes.put(path) { req, res -> wrapper(handler, req, res) }
      HttpMethod.DELETE -> routes.delete(path) { req, res -> wrapper(handler, req, res) }
      HttpMethod.HEAD -> routes.head(path) { req, res -> wrapper(handler, req, res) }
      HttpMethod.OPTIONS -> routes.options(path) { req, res -> wrapper(handler, req, res) }
      else -> error("Unsupported method $method")
    }
  }

  /** Safely enter the execution context and invoke the [block] on the [callback]. */
  private inline fun useCallback(callback: Value?, crossinline block: Value.() -> Unit) {
    // nothing to do if no valid callback is passed
    if (callback == null) return
    
    // enter the context and then invoke the block
    context.useGuest { callback.block() }
  }

  companion object {
    private val ExpressRouteParamRegex = Regex(":(?<param>\\w+)")

    /** Map an Express route path specified to the format used by Reactor Netty. */
    fun mapExpressToReactorRoute(expressRoute: String): String = expressRoute.replace(ExpressRouteParamRegex) {
      "{${it.groups["param"]!!.value}}"
    }
  }
}
