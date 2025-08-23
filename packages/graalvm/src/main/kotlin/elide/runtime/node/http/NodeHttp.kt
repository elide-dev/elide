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

import elide.annotations.Inject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTPAPI
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.js.JsError
import org.graalvm.polyglot.Value
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import elide.runtime.intrinsics.server.http.internal.ThreadLocalHandlerRegistry
import elide.runtime.intrinsics.server.http.netty.NettyServerConfig
import elide.runtime.intrinsics.server.http.netty.NettyServerEngine
import elide.runtime.intrinsics.js.JsPromise.Companion.promise

// Installs the Node `http` module into the intrinsic bindings.
@Intrinsic internal class NodeHttpModule @Inject constructor(
  private val exec: GuestExecutorProvider,
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp.create(exec) }
  internal fun provide(): HTTPAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP)) { provide() }
  }
}

/**
 * # Node API: `http`
 */
internal class NodeHttp private constructor (
  private val exec: GuestExecutorProvider,
) : ReadOnlyProxyObject, HTTPAPI {
  // Minimal built-ins
  private val methods: Array<String> = arrayOf(
    "ACL", "BIND", "CHECKOUT", "CONNECT", "COPY", "DELETE", "GET", "HEAD", "LINK", "LOCK",
    "M-SEARCH", "MERGE", "MKACTIVITY", "MKCALENDAR", "MKCOL", "MOVE", "NOTIFY", "OPTIONS",
    "PATCH", "POST", "PROPFIND", "PROPPATCH", "PURGE", "PUT", "REBIND", "REPORT", "SEARCH",
    "SOURCE", "SUBSCRIBE", "TRACE", "UNLINK", "UNLOCK", "UNSUBSCRIBE"
  )

  private val statusCodes: Map<Int, String> = mapOf(
    100 to "Continue",
    101 to "Switching Protocols",
    200 to "OK",
    201 to "Created",
    202 to "Accepted",
    204 to "No Content",
    301 to "Moved Permanently",
    302 to "Found",
    304 to "Not Modified",
    307 to "Temporary Redirect",
    308 to "Permanent Redirect",
    400 to "Bad Request",
    401 to "Unauthorized",
    403 to "Forbidden",
    404 to "Not Found",
    405 to "Method Not Allowed",
    413 to "Payload Too Large",
    414 to "URI Too Long",
    415 to "Unsupported Media Type",
    418 to "I'm a Teapot",
    425 to "Too Early",
    426 to "Upgrade Required",
    429 to "Too Many Requests",
    500 to "Internal Server Error",
    501 to "Not Implemented",
    502 to "Bad Gateway",
    503 to "Service Unavailable",
    504 to "Gateway Timeout",
  )

  internal companion object {
    @JvmStatic fun create(exec: GuestExecutorProvider): NodeHttp = NodeHttp(exec)
  }

  // @TODO not yet implemented
  override fun getMemberKeys(): Array<String> = arrayOf(
    "Agent",
    "ClientRequest",
    "Server",
    "ServerResponse",
    "IncomingMessage",
    "OutgoingMessage",
    "METHODS",
    "STATUS_CODES",
    "createServer",
    "get",
    "globalAgent",
    "maxHeaderSize",
    "request",
    "validateHeaderName",
    "validateHeaderValue",
    "setMaxIdleHTTPParsers",
  )
  override fun getMember(key: String?): Any? = when (key) {
    // class/object stubs
    "Agent", "ClientRequest", "Server", "ServerResponse", "IncomingMessage", "OutgoingMessage" ->
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = emptyArray()
        override fun getMember(key: String?): Any? = null
      }

    // constants
    "METHODS" -> methods
    "STATUS_CODES" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = statusCodes.keys.map { it.toString() }.toTypedArray()
      override fun getMember(key: String?): Any? = key?.toIntOrNull()?.let { statusCodes[it] }
    }

    // global agent stub and config
    "globalAgent" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = emptyArray()
      override fun getMember(key: String?): Any? = null
    }
    "maxHeaderSize" -> 16384
    "setMaxIdleHTTPParsers" -> ProxyExecutable { _ -> null }

    // validators
    "validateHeaderName" -> ProxyExecutable { args ->
      val name = args.getOrNull(0)?.asString() ?: throw JsError.typeError("Header name must be a string")
      val token = Regex("^[!#$%&'*+\\-.^_`|~0-9A-Za-z]+$")
      if (!token.matches(name)) throw JsError.typeError("Invalid HTTP header name: $name")
      null
    }
    "validateHeaderValue" -> ProxyExecutable { args ->
      val name = args.getOrNull(0)?.asString() ?: throw JsError.typeError("Header name must be a string")
      val value = args.getOrNull(1)?.asString() ?: throw JsError.typeError("Header value must be a string")
      if (value.any { ch -> ch == '\r' || ch == '\n' || ch.code < 0x20 && ch != '\t' || ch.code == 0x7F }) {
        throw JsError.typeError("Invalid character in header \"$name\"")
      }
      null
    }

    // server/request shims
    "createServer" -> ProxyExecutable { args ->
      // optional request handler function
      val handler = args.firstOrNull()?.takeIf { it != null && it.canExecute() }

      // build server components
      val config = NettyServerConfig()
      val handlerRegistry = ThreadLocalHandlerRegistry { _ ->
        // no pre-initialized handlers; stages are registered on the router
      }
      val router = PipelineRouter(handlerRegistry)
      // register unconditional simple handler; allows (req, res) signature and prevents 404 fallthrough
      handler?.let { router.handle(null, null, it) }
      val engine = NettyServerEngine(config, router, exec)

      // server object surfaced to JS
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("listen", "close")
        override fun getMember(key: String?): Any? = when (key) {
          "listen" -> ProxyExecutable { largs ->
            // parse (port?, host?, callback?)
            val portArg = largs.getOrNull(0)?.takeIf { it != null && it.isNumber }?.asInt()
            val hostArg = largs.getOrNull(1)?.takeIf { it != null && it.isString }?.asString()
            val cb = largs.lastOrNull()?.takeIf { it != null && it.canExecute() }

            // apply config
            portArg?.let { config.port = it }
            hostArg?.let { config.host = it }
            cb?.let { config.onBind(it) }

            // if callback provided, use callback semantics and return server immediately
            if (cb != null) {
              engine.start()
              this
            } else {
              // otherwise return a Promise that resolves with the server after bind
              val self = this
              exec.executor().promise<Value> {
                runCatching {
                  engine.start()
                }.onSuccess {
                  resolve(Value.asValue(self))
                }.onFailure { err ->
                  reject(Value.asValue(err.message ?: "listen failed"))
                }
              }
            }
          }
          "close" -> ProxyExecutable { largs ->
            val cb = largs.lastOrNull()?.takeIf { it != null && it.canExecute() }

            // downcast to NettyServerEngine to access stop()
            val netty = engine as NettyServerEngine

            if (cb != null) {
              runCatching { netty.stop() }
                .onFailure { /* surface error by throwing */ throw it }
              cb.execute()
              null
            } else {
              exec.executor().promise<Value> {
                runCatching { netty.stop() }
                  .onSuccess { resolve(Value.asValue(null)) }
                  .onFailure { err -> reject(Value.asValue(err.message ?: "close failed")) }
              }
            }
          }
          else -> null
        }
      }
    }
    "get" -> ProxyExecutable { _ -> null }
    "request" -> ProxyExecutable { _ -> null }

    else -> null
  }
}
