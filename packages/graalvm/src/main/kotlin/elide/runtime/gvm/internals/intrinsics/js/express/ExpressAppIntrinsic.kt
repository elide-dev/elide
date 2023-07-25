package elide.runtime.gvm.internals.intrinsics.js.express

import elide.runtime.Logging
import elide.runtime.intrinsics.js.express.ExpressApp
import org.graalvm.polyglot.Value
import reactor.netty.http.server.HttpServer

/**
 * An [ExpressApp] implemented as a wrapper around a Reactor Netty server.
 *
 * A server builder is used internally and can be configured by methods like [get]. Calling [listen] will cause the
 * server to be built and bound to the specified port.
 */
internal class ExpressAppIntrinsic(private val context: ExpressContext) : ExpressApp {
  /** Represents a route handler registered by a guest script. */
  private data class RouteHandler(
    val path: String,
    val handler: Value,
  )

  private val logging by lazy { Logging.of(ExpressAppIntrinsic::class) }

  /** Internal collection of route handlers requested by guest scripts. */
  private val routeRegistry = mutableSetOf<RouteHandler>()

  override fun get(path: String, handler: Value) {
    // register the route for later use
    routeRegistry.add(RouteHandler(path, handler))
  }

  override fun listen(port: Int, callback: Value?) {
    // configure all the route handlers, set the port and bind the socket
    HttpServer.create().route { routes ->
      for(route in routeRegistry) routes.get(mapExpressToReactorRoute(route.path)) { req, res ->
        // construct the wrappers
        val request = ExpressRequestIntrinsic(req)
        val response = ExpressResponseIntrinsic(res)

        // invoke the JS handler
        logging.info("Calling guest handler")
        useCallback(route.handler) { executeVoid(request, response) }
        logging.info("Exited guest handler")

        // return the internal publisher handled by the wrapper
        response.end()
      }
    }.port(port).bindNow()

    // prevent the JVM from exiting while the server is running
    context.pin()

    // notify listeners
    useCallback(callback) { executeVoid() }
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