package elide.runtime.gvm.internals.intrinsics.js.express

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
  /** Internal builder used to configure the server before binding. */
  private val serverBuilder = HttpServer.create()

  override fun get(path: String, handler: Value) {
    // register the route with the builder
    serverBuilder.route { routes ->
      routes.get(mapExpressToReactorRoute(path)) { req, res ->
        // construct the wrappers
        val request = ExpressRequestIntrinsic(req)
        val response = ExpressResponseIntrinsic(res)

        // invoke the JS handler
        useCallback(handler) { executeVoid(request, response) }

        // return a publisher waiting for the request to be sent
        res.then()
      }
    }
  }

  override fun listen(port: Int, callback: Value?) {
    // set the port and bind the socket
    serverBuilder.port(port).bindNow()
    
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

  private companion object {
    private val ExpressRouteParamRegex = Regex(":(<param>\\w)")

    /** Map an Express route path specified to the format used by Reactor Netty. */
    private fun mapExpressToReactorRoute(expressRoute: String): String = expressRoute.replace(ExpressRouteParamRegex) {
      "{${it.groups["param"]}}"
    }
  }
}