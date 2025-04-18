/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.intrinsics.server.http.internal

import elide.http.Request
import elide.runtime.Logging
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.js.JsProxy
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.HttpMethod
import elide.runtime.intrinsics.server.http.HttpRouter
import elide.vm.annotations.Polyglot

/**
 * The HTTP Router resolves [GuestHandler] references for an incoming [Request].
 *
 * Route handlers are registered using the [handle] method, which also writes to an internal [handlerRegistry].
 */
@DelicateElideApi internal class PipelineRouter(private val handlerRegistry: HandlerRegistry) : HttpRouter {
  /** Private logger instance. */
  private val logging by lazy { Logging.of(PipelineRouter::class) }

  /**
   * A stack of [PipelineStage] entries that forming the handler pipeline. Incoming requests are sent through each
   * stage until a match is found.
   */
  private val pipeline = mutableListOf<PipelineStage>()

  /**
   * Register a guest value from the host as an unconditional handler.
   *
   * @param handler Handler to register
   */
  internal fun handle(handler: PolyglotValue) {
    val key = handlerRegistry.register(GuestHandler.async(handler))
    pipeline.add(PipelineStage(key, unconditionalMatcher()))
  }

  @Polyglot override fun handle(method: String?, path: String?, handler: PolyglotValue) {
    // store the handler reference and get the key
    val key = handlerRegistry.register(GuestHandler.simple(handler))

    // register the handler as a stage in the pipeline
    pipeline.add(PipelineStage(key, compileMatcher(path, method?.let(HttpMethod::valueOf))))
  }

  /** Resolve a handler pipeline that iterates over every stage matching the incoming [request]. */
  internal fun pipeline(request: Request, context: HttpContext): ResolvedPipeline = sequence {
    // iterate over every handler in the pipeline
    pipeline.forEachIndexed { index, stage ->
      // test the stage against the incoming request
      logging.debug { "Handling pipeline stage: $index" }
      if (stage.matcher(request, context)) {
        // found a match, resolve the handler reference
        logging.debug { "Handler condition matches request at stage $index" }
        val handler = handlerRegistry.resolve(index) ?: error(
          "Fatal error: unable to resolve handler reference for pipeline stage $stage",
        )

        yield(handler)
      }
    }
  }

  private companion object {
    /** Name of the capturing group used to process path variable names. */
    private const val MATCHER_NAME_GROUP = "name"

    /** Regex matching path variable templates specified by a guest handler */
    private val PathVariableRegex = Regex(":(?<$MATCHER_NAME_GROUP>\\w+)")

    /**
     * Returns a matcher which always evaluates to `true`.
     */
    private fun unconditionalMatcher(): PipelineMatcher = { _, _ ->
      true
    }

    /**
     * Returns a function that tests whether an incoming [Request] should be passed to a handler, using a
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
        if (method != null && method.name != request.method.symbol) return@matcher false

        // if no matcher template is specified, accept all paths
        if (pattern == null) return@matcher true

        // otherwise return true when the pattern matches the requested path
        val urlStr = request.url.toString()
        val (matchString, params) = if (urlStr.contains("?")) {
          // strip query string from the request URI
          urlStr.substringBefore("?") to urlStr.substringAfter("?")
        } else {
          request.url.toString() to null
        }
        val jsParams = params?.split("&")?.associate {
          val (key, value) = it.split("=")
          key to value
        }
        pattern.matchEntire(matchString)?.also { match ->
          val requestParams = JsProxy.build {
            jsParams?.forEach { (key, value) ->
              put(key, value)
            }
          }

          // extract path variables and add them to the request
          for (variable in pathVariables) match.groups[variable]?.let {
            requestParams.putMember(variable, PolyglotValue.asValue(it.value))
          }

          // TODO(@darvld): use a type-safe key for this
          // store request params in the context
          context["params"] = requestParams
        } != null
      }
    }
  }
}
