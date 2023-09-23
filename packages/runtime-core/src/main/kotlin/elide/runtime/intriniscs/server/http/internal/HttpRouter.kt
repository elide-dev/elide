package elide.runtime.intriniscs.server.http.internal

import io.netty.handler.codec.http.HttpMethod
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsProxy

@DelicateElideApi internal class HttpRouter(handlerRegistry: HandlerRegistry) : RoutingRegistry(handlerRegistry) {
  /** Private logger instance. */
  private val logging by lazy { Logging.of(HttpRouter::class) }

  /**
   * A stack of [PipelineStage] entries that forming the handler pipeline. Incoming requests are sent through each
   * stage until a match is found.
   */
  private val pipeline = mutableListOf<PipelineStage>()

  override fun handle(method: HttpMethod?, path: String?, handler: PolyglotValue) {
    val key = compileRouteKey(path, method)

    handlerRegistry.register(key, GuestHandler.of(handler))
    pipeline.add(PipelineStage(key, compileMatcher(path, method)))
  }

  fun route(request: HttpRequest, context: HttpRequestContext): GuestHandlerFunction? {
    return checkPipelineStage(request, context)
  }

  /**
   * Iterate over the pipeline until a matching stage is found, returning a [GuestHandler] reference associated with
   * it, or `null` if no matching stage is found.
   *
   * @param request The incoming [HttpRequest], currently being routed.
   * @param context The context for the incoming request, used to store information such as path variable values.
   * @return a [GuestHandler] for the incoming [request], or null if no registered handler is found matching the URI.
   */
  private tailrec fun checkPipelineStage(
    request: HttpRequest,
    context: HttpRequestContext,
    index: Int = 0,
  ): GuestHandler? {
    // get the next handler in the pipeline (or end if no more handlers remaining)
    logging.debug { "Handling pipeline stage: $index" }
    val stage = pipeline.getOrNull(index) ?: return null

    // test the stage against the incoming request
    if (stage.matcher(request, context)) {
      // found a match, resolve the handler reference
      logging.debug { "Handler condition matches request at stage $index" }
      return handlerRegistry.resolve(stage.key)
    } else {
      // skip this stage
      logging.debug { "Handler condition does not match request at stage $index" }
      return checkPipelineStage(request, context, index + 1)
    }
  }

  private companion object {
    /** Name of the capturing group used to process path variable names. */
    private const val MATCHER_NAME_GROUP = "name"

    /** Regex matching path variable templates specified by a guest handler */
    private val PathVariableRegex = Regex(":(?<$MATCHER_NAME_GROUP>\\w+)")

    /**
     * Returns a function that tests whether an incoming [HttpRequest] should be passed to a handler, using a
     * [template] string and optionally filtering by HTTP [method]. Path variables included in the [template] will be
     * captured by the matcher and added to the request proxy.
     *
     * @param template An optional template string used to match incoming request paths, can contain variable matchers
     * in the format specified by the Express.js documentation. If not provided, all requests are matched regardless of
     * the path, unless the [method] option is set.
     * @param method An optional HTTP method filter. If not specified, requests are only filtered by path, as specified
     * by the [template].
     */
    private fun compileMatcher(
      template: String? = null,
      method: HttpMethod? = null
    ): PipelineMatcher {
      // keep a record of all path variables in the template
      val pathVariables = mutableListOf<String>()

      // create a matching pattern using the provided path template
      val pattern = template?.replace(PathVariableRegex) { result ->
        // replace express path variable matchers with named capture groups
        val paramName = result.groups[MATCHER_NAME_GROUP]?.value ?: error("Invalid path matcher")
        pathVariables.add(paramName)

        "(?<$paramName>[^\\/]+)"
      }?.let(::Regex)

      return matcher@{ request, context ->
        // Filter by HTTP method
        if (method != null && method != request.method) return@matcher false

        // if no matcher template is specified, accept all paths
        if (pattern == null) return@matcher true

        // otherwise return true when the pattern matches the requested path
        pattern.matchEntire(request.uri)?.also { match ->
          // TODO(@darvld): maybe we don't need this if the instrinsics handle the unwrapping logic
          val requestParams = JsProxy.build { /* empty */ }

          // extract path variables and add them to the request
          for (variable in pathVariables) match.groups[variable]?.let {
            requestParams.putMember(variable, PolyglotValue.asValue(it.value))
          }

          // TODO(@darvld): use a type-safe key for this
          // store request params in the context
          context["params"] = requestParams
        } != null
        return@matcher true
      }
    }
  }
}