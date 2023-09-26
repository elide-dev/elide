package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse

/**
 * Represents the signature of a method used as request handler. The [GuestHandler] wrapper implements this signature
 * by executing a guest value.
 */
@DelicateElideApi internal fun interface GuestHandlerFunction {
  /**
   * Handle an incoming HTTP [request] by accessing the outgoing [response] and a [context] shared between all the
   * handlers in the pipeline. The return value indicates whether the next handler will be invoked.
   *
   * @param request The incoming [HttpRequest] being handled.
   * @param response The outgoing [HttpResponse] to be sent back to the client.
   * @param context A generic container used to pass data between handlers in the pipeline.
   * @return Whether the next handler in the pipeline (if any) will be invoked after this one.
   */
  operator fun invoke(request: HttpRequest, response: HttpResponse, context: HttpContext): Boolean
}

/** Internal alias used for a list of handler references. */
@DelicateElideApi internal typealias HandlerStack = MutableList<GuestHandler>