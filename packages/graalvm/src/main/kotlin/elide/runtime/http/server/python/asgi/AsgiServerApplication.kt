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
package elide.runtime.http.server.python.asgi

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import kotlin.time.TimeSource
import elide.runtime.Logging
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.http.server.*
import elide.runtime.http.server.netty.Http3Service
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.HttpApplicationStack.ServiceBinding
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.HttpsService

/**
 * An adapter used to serve ASGI guest applications on Elide's server engine.
 *
 * ASGI (Asynchronous Server Gateway Interface) is the async successor to WSGI, used by modern Python frameworks
 * such as FastAPI, Starlette, and async Django. This adapter bridges Netty's event-driven HTTP pipeline to the
 * Python ASGI protocol.
 *
 * ## ASGI Protocol
 *
 * An ASGI application is an async callable with the signature:
 *
 * ```python
 * async def app(scope: dict, receive: Callable, send: Callable) -> None
 * ```
 *
 * Where:
 * - `scope` describes the connection (type, path, headers, etc.)
 * - `receive` is an awaitable that returns incoming message dicts (request body chunks)
 * - `send` is an awaitable that accepts outgoing message dicts (response start, body chunks)
 *
 * ## Integration with Netty
 *
 * Each incoming HTTP request is translated to an ASGI HTTP connection scope and dispatched to the Python
 * application via the [ContextAwareExecutor]. The response flows back through the [AsgiSend] callable
 * which writes headers and body data to the Netty response pipeline.
 *
 * ## asyncio Event Loop
 *
 * The ASGI application runs on GraalPy's asyncio event loop within the context-aware executor thread.
 * The bridge uses `asyncio.get_event_loop().run_until_complete()` to drive the coroutine, with results
 * bridged back to Netty via the call lifecycle.
 *
 * @see AsgiScope
 * @see AsgiReceive
 * @see AsgiSend
 * @see AsgiEntrypoint
 */
public class AsgiServerApplication(
  private val entrypoint: AsgiEntrypoint,
  private val executor: ContextAwareExecutor,
) : HttpApplication<AsgiCallContext> {

  /** Wrapper for a resolved ASGI application callable, asyncio runner, and async bridge factory. */
  private class AsgiStack(
    val app: Value,
    val asyncioRunUntilComplete: Value,
    val asyncWrapperFactory: Value,
  )

  /** Host information resolved once the application starts, used to build ASGI scopes. */
  @Volatile private var hostInfo: ServiceBinding? = null

  override fun toString(): String = "AsgiServerApplication(${entrypoint.source.name})"

  override fun onStart(stack: HttpApplicationStack) {
    val services = stack.services.associateBy { it.label }

    val binding = services[HttpsService.LABEL]?.bindResult?.getOrNull()
      ?: services[Http3Service.LABEL]?.bindResult?.getOrNull()
      ?: services[HttpCleartextService.LABEL]?.bindResult?.getOrNull()
      ?: error("Unable to resolve bound host address: no services running")

    hostInfo = binding
    log.info("ASGI application started: {}", entrypoint.source.name)
  }

  override fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: HttpRequestBody,
    responseBody: HttpResponseBody,
  ): AsgiCallContext {
    val host = hostInfo ?: error("Host info is unresolved, cannot accept calls before binding")
    val scope = AsgiScope.httpScope(request, host, entrypoint)
    val receive = AsgiReceive()

    return AsgiCallContext(scope, receive, response, responseBody)
  }

  override fun handle(call: HttpCall<AsgiCallContext>) {
    val ctx = call.context

    val send = AsgiSend(call.response, call.responseBody) {
      call.send()
    }

    call.requestBody.consume(ctx.receive)

    executor.execute {
      runCatching {
        val stack = LocalAsgiStack.current() ?: initializeStack().also {
          executor.setContextLocal(LocalAsgiStack, it)
        }

        callAsgiApplication(stack, ctx, send)

        if (!send.isComplete) {
          call.fail(IllegalStateException("ASGI application returned without sending a complete response"))
        }
      }.onFailure { cause ->
        log.debug("ASGI application call failed", cause)
        call.fail(cause)
      }
    }
  }

  /**
   * Initialize the ASGI stack for the current GraalPy context.
   *
   * This evaluates the entrypoint source, resolves the ASGI app callable, and prepares the asyncio runner
   * that will be used to drive ASGI coroutines.
   */
  private fun initializeStack(): AsgiStack {
    val context = Context.getCurrent()
    log.trace("Initializing ASGI stack for context {}", context)

    val start = TimeSource.Monotonic.markNow()

    // Evaluate the entrypoint module
    val module = context.eval(entrypoint.source)
    check(module.hasMember(entrypoint.bindingName)) {
      "Module does not have a member named '${entrypoint.bindingName}'"
    }

    val appOrFactory = module.getMember(entrypoint.bindingName)

    val app = if (entrypoint.bindingArgs == null) appOrFactory else {
      check(appOrFactory.canExecute()) { "Application factory is not callable" }
      appOrFactory.execute(*(entrypoint.bindingArgs.toTypedArray()))
    }

    check(app.canExecute()) { "ASGI application is not callable" }

    // Prepare the asyncio runner: we use `asyncio.get_event_loop().run_until_complete` to drive coroutines.
    // This approach works because GraalPy's asyncio event loop is backed by java.nio selectors,
    // meaning it integrates naturally with the JVM threading model.
    val asyncio = context.eval("python", "import asyncio; asyncio")
    val loop = asyncio.invokeMember("get_event_loop")
    val runUntilComplete = loop.getMember("run_until_complete")

    check(runUntilComplete.canExecute()) {
      "asyncio event loop does not have a callable run_until_complete"
    }

    // Create a Python factory that wraps Java callables (ProxyExecutable) into proper Python
    // async functions. ASGI requires receive/send to be async callables — calling them must return
    // an awaitable. Our Java-side ProxyExecutable.execute() returns plain values, so `await receive()`
    // would raise TypeError without this bridge. The wrapper creates thin async def functions that
    // call through to the Java side synchronously (acceptable for single-request-per-coroutine flow).
    @Suppress("MaxLineLength")
    val asyncWrapperFactory = context.eval("python", ASYNC_WRAPPER_SOURCE)

    check(asyncWrapperFactory.canExecute()) {
      "Failed to create async wrapper factory for ASGI bridge"
    }

    log.trace("Initialized ASGI stack for context {} in {}", context, start.elapsedNow())
    return AsgiStack(app, runUntilComplete, asyncWrapperFactory)
  }

  /**
   * Call the ASGI application with the given scope, receive, and send callables.
   *
   * The application is invoked as a coroutine: `app(scope, receive, send)` returns a coroutine object,
   * which we then drive to completion using `asyncio.get_event_loop().run_until_complete(coro)`.
   */
  private fun callAsgiApplication(stack: AsgiStack, ctx: AsgiCallContext, send: AsgiSend) {
    // Wrap Java ProxyExecutable receive/send into Python async functions so that
    // `await receive()` and `await send(msg)` work correctly in the ASGI app.
    val wrappers = stack.asyncWrapperFactory.execute(ctx.receive, send)
    val asyncReceive = wrappers.getArrayElement(0)
    val asyncSend = wrappers.getArrayElement(1)

    // Call the ASGI application — this returns a coroutine object
    val coroutine = stack.app.execute(ctx.scope, asyncReceive, asyncSend)

    // Drive the coroutine to completion using asyncio's event loop
    stack.asyncioRunUntilComplete.execute(coroutine)
  }

  public companion object {
    private val log = Logging.of(AsgiServerApplication::class.java)
    private val LocalAsgiStack = ContextLocal<AsgiStack>()

    // language=python
    private const val ASYNC_WRAPPER_SOURCE = """
def _make_async_wrappers(java_receive, java_send):
    async def receive():
        return java_receive()
    async def send(message):
        java_send(message)
    return (receive, send)
_make_async_wrappers
"""
  }
}
