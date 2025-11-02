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

package elide.runtime.http.server.python.wsgi

import io.netty.channel.unix.DomainSocketAddress
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyHashMap
import java.net.InetSocketAddress
import java.net.UnixDomainSocketAddress
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString
import kotlin.time.TimeSource
import elide.runtime.Logging
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.ContextLocal
import elide.runtime.exec.PinnedContext
import elide.runtime.http.server.*
import elide.runtime.http.server.netty.Http3Service
import elide.runtime.http.server.netty.HttpApplicationStack
import elide.runtime.http.server.netty.HttpCleartextService
import elide.runtime.http.server.netty.HttpsService

/**
 * An adapter used to serve WSGI guest application on Elide's server engine.
 *
 * Applications must be defined as either a WSGI callable, or, if the [entrypoint] specifies arguments, a callable
 * that returns the WSGI callable itself.
 *
 * WSGI callables must accept a map as the first argument (the [WsgiEnviron]), and an executable used to send the
 * headers, and it must return an iterable that yields buffer-like objects to be used as the response content. The
 * send callback must be invoked with an array-like in the form `(status, headers, error)`.
 */
public class WsgiServerApplication(
  private val entrypoint: WsgiEntrypoint,
  private val executor: ContextAwareExecutor,
) : HttpApplication<WsgiEnviron> {
  /** Type-safe wrapper for a WSGI guest application stack, called on each incoming request.  */
  @JvmInline private value class WsgiCallable(private val handler: Value) {
    /**
     * Call the guest WSGI application, passing a WSGI [environ] for the incoming request and a callback to send the
     * response headers.
     */
    fun call(environ: ProxyHashMap, onResponse: ProxyExecutable): Value {
      return handler.execute(environ, onResponse)
    }
  }

  /** Host information resolved once the application starts used to prepare a [WsgiEnviron]. */
  public data class HostInfo(val scheme: String, val hostname: String, val port: Int)

  @Volatile private var hostInfo: HostInfo? = null

  override fun toString(): String {
    return "WsgiServerApplication(${entrypoint.source.name})"
  }

  override fun onStart(stack: HttpApplicationStack) {
    // prefer the address to the HTTPS service, fall back to HTTP/3,
    // and finally use cleartext if nothing else is running
    val services = stack.services.associateBy { it.label }

    val binding = services[HttpsService.LABEL]?.bindResult?.getOrNull()
      ?: services[Http3Service.LABEL]?.bindResult?.getOrNull()
      ?: services[HttpCleartextService.LABEL]?.bindResult?.getOrNull()
      ?: error("Unable to resolve bound host address: no services running")

    hostInfo = when (val address = binding.address) {
      is InetSocketAddress -> HostInfo(binding.scheme, address.hostName, address.port)
      // NIO/Native domain sockets
      is UnixDomainSocketAddress -> HostInfo(binding.scheme, address.path.absolutePathString(), 0)
      is DomainSocketAddress -> HostInfo(binding.scheme, address.path(), 0)
      // fallback to cleartext. localhost and no port
      else -> HostInfo(UNKNOWN_SCHEME, UNKNOWN_HOST, UNKNOWN_PORT)
    }
  }

  override fun newContext(
    request: HttpRequest,
    response: HttpResponse,
    requestBody: ReadableContentStream,
    responseBody: WritableContentStream
  ): WsgiEnviron {
    val host = hostInfo ?: error("Host info is unresolved, cannot accept calls before binding")
    return WsgiEnviron.from(request, executor, entrypoint, host)
  }

  override fun handle(call: HttpCall<WsgiEnviron>) {
    executor.execute {
      runCatching {
        // initialize the WSGI stack if it's not yet available for this context
        val stack = LocalWsgiStack.current() ?: initializeStack().also {
          executor.setContextLocal(LocalWsgiStack, it)
        }

        callWsgiApplication(stack, call)
      }.onFailure { cause ->
        log.debug("WSGI Application call failed", cause)
        call.fail(cause)
      }
    }
  }

  private fun initializeStack(): WsgiCallable {
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
    return WsgiCallable(app)
  }

  private fun callWsgiApplication(stack: WsgiCallable, call: HttpCall<WsgiEnviron>) {
    // connect the input to the request body
    call.requestBody.consume(call.context.input)

    val responseBody = stack.call(call.context.map) { args ->
      args.getOrNull(2)?.let { log.error("Application error: $it") }
      call.applyResponse(args.getOrNull(0), args.getOrNull(1))
    }

    call.applyResponseBody(responseBody)
  }

  private fun HttpCall<WsgiEnviron>.applyResponse(status: Value?, headers: Value?) {
    val status = status?.takeIf { it.isString }?.asString()
      ?: error("Invalid status code provided: $status")

    val headers = headers?.takeIf { it.hasArrayElements() }
      ?: error("Invalid headers (not a list): $headers")

    response.status = HttpResponseStatus.parseLine(status)
    applyResponseHeaders(headers)
  }

  private fun HttpCall<WsgiEnviron>.applyResponseHeaders(headers: Value) {
    for (i in 0 until headers.arraySize) {
      val header = headers.getArrayElement(i)
      check(header.hasArrayElements() && header.arraySize == 2L) {
        "Invalid header tuple: expected (key, value), received $header"
      }

      val key = header.getArrayElement(0).takeIf { it.isString }?.asString()
        ?: error("Invalid header key, expected a string, found: ${header.getArrayElement(0)}")

      val value = header.getArrayElement(1).takeIf { it.isString }?.asString()
        ?: error("Invalid header value, expected a string, found: ${header.getArrayElement(1)}")

      response.addHeader(key, value)
    }
  }

  private fun HttpCall<WsgiEnviron>.applyResponseBody(responseBody: Value) = when {
    responseBody.hasIterator() -> sendIteratorResponse(this, responseBody.iterator)
    responseBody.isIterator -> sendIteratorResponse(this, responseBody)
    else -> error("Application returned an unsupported value: $responseBody")
  }

  private fun sendIteratorResponse(call: HttpCall<WsgiEnviron>, guestIterator: Value) {
    val firstNonEmptyChunk = AtomicReference<ByteArray?>(null)
    call.response.setHeader(HttpHeaderNames.TRANSFER_ENCODING, "chunked")

    // we need to add a source first, otherwise when we send the call it will
    // look empty, and we'll lose the content
    val contextPin = PinnedContext.current()
    call.responseBody.source { writer ->
      // consume the stored chunk first
      firstNonEmptyChunk.getAndSet(null)?.let {
        writer.write(it)
        return@source
      }

      executor.execute(contextPin) {
        // we need to make sure we're in context to use the iterator here
        if (!guestIterator.hasIteratorNextElement()) writer.end() else {
          writer.write(guestIterator.iteratorNextElement.readToByteArray())
          if (!guestIterator.hasIteratorNextElement()) writer.end() // end early
        }
      }
    }

    // we can't avoid blocking here until we get a non-empty chunk; after
    // that we can start sending and the rest of the iterator will be async
    while (true) {
      if (!guestIterator.hasIteratorNextElement()) {
        // if we reach the end, send an empty response
        call.responseBody.close()
        call.send()
        return
      }

      val element = guestIterator.iteratorNextElement
      if (element.bufferSize == 0L) continue

      // we need to store the first non-empty chunk here since we already
      // polled it, it will be pulled later
      firstNonEmptyChunk.set(element.readToByteArray())
      call.send()
      break
    }
  }

  private fun Value.readToByteArray(): ByteArray {
    val bytes = ByteArray(bufferSize.toInt())
    readBuffer(0L, bytes, 0, bytes.size)
    return bytes
  }

  public companion object {
    private const val UNKNOWN_SCHEME: String = "http"
    private const val UNKNOWN_HOST: String = "localhost"
    private const val UNKNOWN_PORT: Int = 0

    private val log = Logging.of(WsgiServerApplication::class.java)
    private val LocalWsgiStack = ContextLocal<WsgiCallable>()
  }
}
