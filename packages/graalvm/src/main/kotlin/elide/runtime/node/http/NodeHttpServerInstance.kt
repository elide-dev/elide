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
package elide.runtime.node.http

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.exec.ContextAware
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.js.node.NodeHttpServerRequest
import elide.runtime.http.server.js.node.NodeHttpServerResponse
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.http.HttpServerAPI
import elide.runtime.node.events.EventAware

public class NodeHttpServerInstance internal constructor(
  private val application: NodeHttpServerHolder,
  private val options: HttpApplicationOptions,
  private val events: EventAware = EventAware.create(),
) : HttpServerAPI, ProxyObject, EventAware by events {
  private val state = AtomicInteger()
  override val listening: Boolean get() = state.get() == STATE_STARTED

  override var headersTimeout: Long = 60_000
  override var requestTimeout: Long = 300_000
  override var maxHeadersCount: Long = 2_000
  override var maxRequestsPerSocket: Long = 0
  override var timeout: Long = 0
  override var keepAliveTimeout: Long = 5_000
  override var keepAliveTimeoutBuffer: Long = 1_000

  @ContextAware internal fun onCallReceived(request: NodeHttpServerRequest, response: NodeHttpServerResponse) {
    // always called from a context thread
    emit(HttpServerAPI.EVENT_REQUEST, request, response)
  }

  @ContextAware internal fun onStarted() {
    emit(HttpServerAPI.EVENT_LISTENING)
  }

  @ContextAware internal fun onStopped() {
    // always set the state to closed, but only trigger the event if it was updated
    if (state.getAndSet(STATE_STOPPED) != STATE_STARTED) return
    emit(HttpServerAPI.EVENT_CLOSE)
  }

  private fun doListen(port: Int, host: String?, callback: Value?) {
    if (!state.compareAndSet(STATE_IDLE, STATE_STARTED)) return
    // note this is a noop if an instance from another context already bound the server;
    // this is intentional, to allow re-evaluating the endpoint without side effects
    application.bind(prepareOptions(overrideHost = host, overridePort = port))

    callback?.takeIf { it.canExecute() }?.execute()
  }

  private fun prepareOptions(overrideHost: String?, overridePort: Int): HttpApplicationOptions {
    val overrideAddress = InetSocketAddress(overrideHost ?: DEFAULT_HOST, overridePort)
    return HttpApplicationOptions(http = options.http?.copy(address = overrideAddress))
  }

  override fun listen(port: Int, callback: Value?) {
    doListen(port, host = null, callback)
  }

  override fun listen(port: Int, host: String, callback: Value?) {
    doListen(port, host, callback)
  }

  override fun close(callback: Value?) {
    // when we succeed in scheduling a shutdown, we need to defer the event;
    // if it was already stopped, use a CAS to avoid duplicate events
    if (application.stop() || !state.compareAndSet(STATE_STARTED, STATE_STOPPED)) return
    emit(HttpServerAPI.EVENT_CLOSE)
  }

  override fun closeAllConnections(): Unit = close()
  override fun closeIdleConnections(): Unit = close()
  override fun setTimeout(millis: Long?, callback: Value?): Unit = Unit // not yet supported

  override fun getMember(key: String): Any? = when (key) {
    MEMBER_LISTEN -> ProxyExecutable { args ->
      when (args.size) {
        2 -> {
          val port = args.getOrNull(0)?.takeIf { it.isNumber && it.fitsInInt() }
            ?: throw TypeError.create("Port value must be an integer")
          val callback = args.getOrNull(1)?.takeIf { it.canExecute() }
          listen(port.asInt(), callback)
        }

        3 -> {
          val port = args.getOrNull(0)?.takeIf { it.isNumber && it.fitsInInt() }
            ?: throw TypeError.create("Port value must be an integer")
          val host = args.getOrNull(1)?.takeIf { it.isString }
            ?: throw TypeError.create("Host name must be an string")
          val callback = args.getOrNull(2)?.takeIf { it.canExecute() }
          listen(port.asInt(), host.asString(), callback)
        }

        else -> error("Invalid arguments for 'listen', expected [port],[callback] or [port,[host,[callback]]]: $args")
      }
    }

    MEMBER_CLOSE -> ProxyExecutable { close(it.getOrNull(0)) }
    MEMBER_CLOSE_ALL_CONNECTIONS -> ProxyExecutable { close() }
    MEMBER_CLOSE_IDLE_CONNECTIONS -> ProxyExecutable { close() }
    MEMBER_SET_TIMEOUT -> ProxyExecutable { /* not yet supported */ }

    MEMBER_LISTENING -> listening
    MEMBER_HEADERS_TIMEOUT -> headersTimeout
    MEMBER_REQUEST_TIMEOUT -> requestTimeout
    MEMBER_MAX_HEADERS_COUNT -> maxHeadersCount
    MEMBER_MAX_REQUESTS_PER_SOCKET -> maxRequestsPerSocket
    MEMBER_TIMEOUT -> timeout
    MEMBER_KEEP_ALIVE_TIMEOUT -> keepAliveTimeout
    MEMBER_KEEP_ALIVE_TIMEOUT_BUFFER -> keepAliveTimeoutBuffer
    else -> null
  }

  override fun putMember(key: String, value: Value?): Unit = when (key) {
    MEMBER_HEADERS_TIMEOUT -> headersTimeout = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Headers timeout must be a Long value: $value")

    MEMBER_REQUEST_TIMEOUT -> requestTimeout = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Request timeout must be a Long value: $value")

    MEMBER_MAX_HEADERS_COUNT -> maxHeadersCount = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Max headers count must be a Long value: $value")

    MEMBER_MAX_REQUESTS_PER_SOCKET -> maxRequestsPerSocket = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Max requests per socket must be a Long value: $value")

    MEMBER_TIMEOUT -> timeout = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Timeout must be a Long value: $value")

    MEMBER_KEEP_ALIVE_TIMEOUT -> keepAliveTimeout = value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
      ?: throw TypeError("Keepalive timeout must be a Long value: $value")

    MEMBER_KEEP_ALIVE_TIMEOUT_BUFFER -> keepAliveTimeoutBuffer =
      value?.takeIf { it.isNumber && it.fitsInLong() }?.asLong()
        ?: throw TypeError("Keepalive timeout buffer must be a Long value: $value")

    else -> error("Cannot set the value of property $key for server object")
  }

  override fun getMemberKeys(): Array<String> = MemberKeys
  override fun hasMember(key: String): Boolean = MemberKeys.binarySearch(key) >= 0

  private companion object {
    private const val STATE_IDLE = 0
    private const val STATE_STARTED = 1
    private const val STATE_STOPPED = 2

    private const val DEFAULT_HOST = "localhost"

    private const val MEMBER_LISTEN = "listen"
    private const val MEMBER_CLOSE = "close"
    private const val MEMBER_CLOSE_ALL_CONNECTIONS = "closeAllConnections"
    private const val MEMBER_CLOSE_IDLE_CONNECTIONS = "closeIdleConnections"
    private const val MEMBER_SET_TIMEOUT = "setTimeout"

    private const val MEMBER_LISTENING = "listening"
    private const val MEMBER_HEADERS_TIMEOUT = "headersTimeout"
    private const val MEMBER_REQUEST_TIMEOUT = "requestTimeout"
    private const val MEMBER_MAX_HEADERS_COUNT = "maxHeadersCount"
    private const val MEMBER_MAX_REQUESTS_PER_SOCKET = "maxRequestsPerSocket"
    private const val MEMBER_TIMEOUT = "timeout"
    private const val MEMBER_KEEP_ALIVE_TIMEOUT = "keepAliveTimeout"
    private const val MEMBER_KEEP_ALIVE_TIMEOUT_BUFFER = "keepAliveTimeoutBuffer"

    private val MemberKeys = arrayOf(
      MEMBER_LISTEN,
      MEMBER_CLOSE,
      MEMBER_CLOSE_ALL_CONNECTIONS,
      MEMBER_CLOSE_IDLE_CONNECTIONS,
      MEMBER_SET_TIMEOUT,
      MEMBER_LISTENING,
      MEMBER_HEADERS_TIMEOUT,
      MEMBER_REQUEST_TIMEOUT,
      MEMBER_MAX_HEADERS_COUNT,
      MEMBER_MAX_REQUESTS_PER_SOCKET,
      MEMBER_TIMEOUT,
      MEMBER_KEEP_ALIVE_TIMEOUT,
      MEMBER_KEEP_ALIVE_TIMEOUT_BUFFER,
    ).sortedArray()
  }
}
