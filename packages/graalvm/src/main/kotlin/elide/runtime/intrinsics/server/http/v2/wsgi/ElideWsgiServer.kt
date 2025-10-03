/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2.wsgi

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelPromise
import io.netty.handler.codec.http.*
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import java.util.concurrent.Executors
import jakarta.inject.Provider
import jakarta.inject.Singleton
import elide.runtime.Logging
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeLatch
import elide.runtime.core.SharedContextFactory
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.intrinsics.server.http.v2.*
import elide.runtime.intrinsics.server.http.v2.flask.FlaskHttpIntrinsic

@Singleton public class ElideWsgiServerBuilder(
  private val runtimeLatchProvider: Provider<RuntimeLatch>,
  private val entrypointProvider: Provider<EntrypointRegistry>,
  private val contextProvider: Provider<SharedContextFactory>,
) {
  public fun bind(
    bindingName: String,
    bindingArgs: List<String>? = null,
    contextPoolSize: Int = defaultContextPoolSize(),
    port: Int = DEFAULT_PORT
  ) {
    logging.debug { "Starting Elide WSGI server on port $port" }

    val handlerExecutor = ContextAwareExecutor(
      maxContextPoolSize = contextPoolSize,
      baseExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
      contextFactory = { contextProvider.get().acquire() ?: error("No context provider available") },
    )

    val stackManager = WsgiStackManager(
      executor = handlerExecutor,
      bindingName = bindingName,
      bindingArgs = bindingArgs,
      entrypointProvider = entrypointProvider.get(),
    )

    val server = ElideWsgiServer(
      handlerExecutor = handlerExecutor,
      runtimeLatch = runtimeLatchProvider.get(),
      stackManager = stackManager,
    )

    server.bind(port)
  }

  public companion object {
    public const val DEFAULT_PORT: Int = 8080

    private val logging by lazy { Logging.of(ElideWsgiServerBuilder::class) }

    public fun defaultContextPoolSize(): Int = Runtime.getRuntime().availableProcessors()
  }
}

internal class WsgiApplicationStack {
  private var handler: Value? = null

  fun call(environ: ProxyHashMap, onResponse: ProxyExecutable): Value {
    return handler?.execute(environ, onResponse) ?: error("Application is not registered")
  }

  fun register(application: Value) {
    if (handler != null) error("Application is already registered")
    handler = application
  }
}

internal class WsgiStackManager(
  executor: ContextAwareExecutor,
  override val entrypointProvider: EntrypointRegistry,
  private val bindingName: String,
  private val bindingArgs: List<String>? = null,
) : GuestHandlerStackManager<WsgiApplicationStack>(executor) {
  override fun newStack(): WsgiApplicationStack = WsgiApplicationStack()

  override fun initializeStack(stack: WsgiApplicationStack, entrypoint: Source) {
    val module = Context.getCurrent().eval(entrypoint)

    check(module.hasMember(bindingName)) { "Module does not have a member named '$bindingName'" }
    val appOrFactory = module.getMember(bindingName)

    val app = if (bindingArgs == null) appOrFactory else {
      check(appOrFactory.canExecute()) { "Application factory is not callable" }
      appOrFactory.execute(*bindingArgs.toTypedArray())
    }

    check(appOrFactory.canExecute()) { "Application is not callable" }
    stack.register(app)
  }
}

public class ElideWsgiServer internal constructor(
  private val handlerExecutor: ContextAwareExecutor,
  override val runtimeLatch: RuntimeLatch,
  private val stackManager: WsgiStackManager,
) : AbstractHttpIntrinsic() {
  override fun acquireHandler(): HttpContextHandler = HttpContextHandler { context, scope ->
    val promise = scope.newPromise()

    handlerExecutor.execute {
      runCatching {
        stackManager.withStack { stack ->
          callWsgiApplication(
            stack = stack,
            startResponse = promise,
            context = context as WsgiHttpContext,
          )
        }
      }.onFailure { cause ->
        logging.error("WSGI Application call failed", cause)
        applyServerErrorResponse(context)
        promise.setFailure(cause)
      }
    }

    promise
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
        logging.error { "Application error: $it" }
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

      // write callback
//      ProxyExecutable { writeArgs ->
//        logging.debug { "Writing response body chunk" }
//        val chunk = writeArgs.firstOrNull() ?: error("Expected a string or buffer to write")
//
//        if (chunk.hasBufferElements() && chunk.bufferSize > 0) {
//          applyResponseBody(chunk, context)
//          startResponse.setSuccess()
//        }
//      }
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
      logging.debug { "Application returned an unsupported value: $returnValue" }
//      logging.error { "Application returned an invalid response: $returnValue" }
//      applyServerErrorResponse(httpContext)
    }
  }

  private fun sendIteratorResponse(sink: HttpContentSink, guestIterator: Value) {
    val pinnedContext = handlerExecutor.pinContext()

    sink.source {
      handlerExecutor.execute(pinnedContext) {
        if (!guestIterator.hasIteratorNextElement()) return@execute

        val chunk = mapContentChunk(
          value = guestIterator.iteratorNextElement,
          last = !guestIterator.hasIteratorNextElement(),
        )

        it.push(chunk)
      }
    }
  }

  private fun mapContentChunk(value: Value, last: Boolean = false): HttpContent {
    if (!value.hasBufferElements()) error("Value type is not supported as response body: $value")

    val bytes = ByteArray(value.bufferSize.toInt())
    value.readBuffer(0L, bytes, 0, bytes.size)

    val buf = Unpooled.wrappedBuffer(bytes)

    return if (last) DefaultLastHttpContent(buf)
    else DefaultHttpContent(buf)
  }

  private companion object {
    private val logging by lazy { Logging.of(FlaskHttpIntrinsic::class) }
  }
}
