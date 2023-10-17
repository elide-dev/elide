package elide.runtime.intrinsics.server.http.micronaut

import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.HttpMethod
import elide.runtime.intrinsics.server.http.HttpRouter

@DelicateElideApi internal class MicronautGuestRouter(
  private val initializeForThread: (MicronautGuestRouter) -> Unit
) : HttpRouter {
  data class MicronautGuestRoute(
    val method: HttpMethod,
    val uri: String,
    val handle: RequestExecutionHandle,
  )

  private val logging by lazy { Logging.of(MicronautGuestRouter::class) }

  /** Backing thread-local map, populated by [initializeForThread]. */
  private val localHandlerRegistry: ThreadLocal<MutableMap<String, PolyglotValue>> = ThreadLocal()

  internal val routes: MutableSet<MicronautGuestRoute> = mutableSetOf()

  init {
    // pre-initialize with an empty list for the construction thread
    // this avoids incorrect re-evaluation of the entrypoint on start
    localHandlerRegistry.set(mutableMapOf())
  }

  private val handlerRegistry: MutableMap<String, PolyglotValue>
    get() {
      // value already exists for this thread, return it
      localHandlerRegistry.get()?.let { return it }

      // prepare a new empty list
      val map: MutableMap<String, PolyglotValue> = mutableMapOf()
      localHandlerRegistry.set(map)

      // initialize for this thread (populate the list)
      initializeForThread(this)

      return map
    }

  private fun computeHandlerKey(handler: PolyglotValue): String = with(handler.sourceLocation) {
    return "${startLine}:${startColumn}"
  }

  override fun handle(method: String, path: String, handler: PolyglotValue) {
    val key = computeHandlerKey(handler)

    // create the guest execution handle
    val handle = requestExecutionHandle { incoming ->
      // wrap the request and create a response to send
      val request = MicronautHttpRequest(incoming)
      val response = MicronautHttpResponse()
      val context = HttpContext()

      // resolve the handle for the current thread and execute it
      val guestHandler = handlerRegistry[key] ?: error("Handler not found for key $key")
      guestHandler.execute(request, response, context)

      // return the original Micronaut response
      response.unwrap()
    }

    // register the route
    routes.add(MicronautGuestRoute(HttpMethod.valueOf(method), path, handle))

    // store the handler reference
    handlerRegistry[key] = handler
  }
}