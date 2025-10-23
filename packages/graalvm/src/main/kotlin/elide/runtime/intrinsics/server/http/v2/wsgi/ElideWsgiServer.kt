/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2.wsgi

import elide.runtime.intrinsics.server.http.v2.*
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import kotlin.time.TimeSource
import elide.runtime.Logging
import elide.runtime.core.RuntimeLatch
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.intrinsics.server.http.v2.guest.sourceIterator

/**
 * Describes a WSGI application entrypoint called for every incoming request received by the [ElideWsgiServer].
 *
 * For every pooled context, the [source] is evaluated once, and an app binding is resolved from it using the specified
 * [bindingName]. If the [bindingArgs] are specified, the binding is treated as a factory and called once to get the
 * actual application stack to be called.
 */
public data class WsgiEntrypoint(
  val source: Source,
  val bindingName: String,
  val bindingArgs: List<String>? = null,
)

/**
 * A WSGI server implementation that calls a guest application for every incoming request.
 *
 * Each context in the [executor]'s pool has an associated application stack created lazily using the [entrypoint]
 * configuration.
 *
 * @see WsgiEntrypoint
 */
public class ElideWsgiServer(
  private val entrypoint: WsgiEntrypoint,
  private val executor: ContextAwareExecutor,
  override val runtimeLatch: RuntimeLatch,
) : HttpServer() {
  /** Type-safe wrapper for a WSGI guest application stack, called on each incoming request.  */
  @JvmInline private value class WsgiApplicationStack(private val handler: Value) {
    /**
     * Call the guest WSGI application, passing a WSGI [environ] for the incoming request and a callback to send the
     * response headers.
     */
    fun call(environ: ProxyHashMap, onResponse: ProxyExecutable): Value {
      return handler.execute(environ, onResponse)
    }
  }

  private fun initializeStack(): WsgiApplicationStack {
    val context = Context.getCurrent()
    log.trace("Initializing WSGI stack for context {}", context)

    val start = TimeSource.Monotonic.markNow()
    val module = context.eval(entrypoint.source)
    check(module.hasMember(entrypoint.bindingName)) {
      "Module does not have a member named '${entrypoint.bindingName}'"
    }

    val appOrFactory = module.getMember(entrypoint.bindingName)

    val app = if (entrypoint.bindingArgs == null) appOrFactory else {
      check(appOrFactory.canExecute()) { "Application factory is not callable" }
      appOrFactory.execute(*(entrypoint.bindingArgs.toTypedArray()))
    }

    check(app.canExecute()) { "Application is not callable" }

    log.trace("Initialized WSGI stack for context {} in {}", context, start.elapsedNow())
    return WsgiApplicationStack(app)
  }

  override fun acquireHandler(): HttpContextHandler = HttpContextHandler { httpContext, channel ->
    val completion = channel.newPromise()

    executor.execute {
      runCatching {
        // initialize the WSGI stack if it's not yet available for this context
        val stack = LocalWsgiStack.current() ?: initializeStack().also {
          executor.setContextLocal(LocalWsgiStack, it)
        }

        callWsgiApplication(
          stack = stack,
          startResponse = completion,
          context = httpContext as WsgiHttpContext,
        )
      }.onFailure { cause ->
        log.error("WSGI Application call failed", cause)
        applyServerErrorResponse(httpContext)
        completion.setFailure(cause)
      }
    }

    completion
  }

  override fun acquireFactory(): HttpContextFactory<*> =
    HttpContextFactory<HttpContext> { incomingRequest, _, requestSource, responseSink ->
        WsgiHttpContext(
            request = incomingRequest,
            requestBody = requestSource,
            response = DefaultHttpResponse(incomingRequest.protocolVersion(), HttpResponseStatus.NOT_FOUND),
            responseBody = responseSink,
            session = HttpSession(),
        )
    }

  private fun callWsgiApplication(
      stack: WsgiApplicationStack,
      context: WsgiHttpContext,
      startResponse: ChannelPromise,
  ) {
    val environ = context.toWsgiEnviron(
      input = System.`in`,
      errors = System.err,
      version = 1 to 0,
      multithread = true,
      multiprocess = false,
      runOnce = false,
      urlScheme = "http",
      defaultHost = "localhost",
      defaultPort = 3000,
    )

    @Suppress("UNCHECKED_CAST")
    val environMap = ProxyHashMap.from(environ as Map<in Any, Any?>?)

    val responseBody = stack.call(environMap) { args ->
      args.getOrNull(2)?.let {
        log.error("Application error: $it")
      }

      val status = args.getOrNull(0)?.takeIf { it.isString }?.asString()
        ?: error("Invalid status code provided: ${args.getOrNull(0)}")

      val headers = args.getOrNull(1)?.takeIf { it.hasArrayElements() }
        ?: error("Invalid headers (not a list): ${args.getOrNull(1)}")

      // apply status
      context.response.status = HttpResponseStatus.parseLine(status)

      // apply headers
      context.response.headers().apply {
        for (i in 0 until headers.arraySize) {
          val header = headers.getArrayElement(i)
          check(header.hasArrayElements() && header.arraySize == 2L) { "Invalid header tuple: expected (key, value)" }

          val key = header.getArrayElement(0).takeIf { it.isString }?.asString()
            ?: error("Invalid header key, expected a string, found: ${header.getArrayElement(0)}")

          val value = header.getArrayElement(1).takeIf { it.isString }?.asString()
            ?: error("Invalid header value, expected a string, found: ${header.getArrayElement(1)}")

          add(key, value)
        }
      }
    }

    applyResponseBody(responseBody, context)
    startResponse.setSuccess()
  }

  private fun applyServerErrorResponse(httpContext: HttpContext) {
    httpContext.response.status = HttpResponseStatus.INTERNAL_SERVER_ERROR
    httpContext.response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0)
    httpContext.responseBody.close()
  }

  private fun applyResponseBody(
    returnValue: Value,
    httpContext: HttpContext,
  ) = when {
    returnValue.hasIterator() -> sendIteratorResponse(httpContext.responseBody, returnValue.iterator)
    returnValue.isIterator -> sendIteratorResponse(httpContext.responseBody, returnValue)

    else -> {
      log.debug("Application returned an unsupported value: {}", returnValue)
      applyServerErrorResponse(httpContext)
    }
  }

  private fun sendIteratorResponse(sink: HttpContentSink, guestIterator: Value) {
    sink.sourceIterator(guestIterator, executor, ::mapContentChunk)
  }

  private fun mapContentChunk(value: Value, last: Boolean = false): HttpContent {
    if (!value.hasBufferElements()) error("Value type is not supported as response body: $value")

    val bytes = ByteArray(value.bufferSize.toInt())
    value.readBuffer(0L, bytes, 0, bytes.size)

    val buf = Unpooled.wrappedBuffer(bytes)

    return if (last) DefaultLastHttpContent(buf)
    else DefaultHttpContent(buf)
  }

  public companion object {
    public const val DEFAULT_PORT: Int = 8080

    private val log = Logging.of(ElideWsgiServer::class.java)
    private val LocalWsgiStack = ContextLocal<WsgiApplicationStack>()
  }
}
