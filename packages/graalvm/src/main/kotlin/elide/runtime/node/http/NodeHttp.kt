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
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTPAPI
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import elide.runtime.intrinsics.server.http.internal.ThreadLocalHandlerRegistry
import elide.runtime.intrinsics.server.http.netty.NettyServerConfig
import elide.runtime.intrinsics.server.http.netty.NettyServerEngine
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node `http` module into the intrinsic bindings.
@Intrinsic internal class NodeHttpModule(
  private val exec: GuestExecutorProvider = GuestExecutorProvider { GuestExecution.direct() }
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp.create(exec) }
  internal fun provide(): HTTPAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP)) { provide() }
  }
}

/** Minimal placeholder object type */
private class ReadOnlyTypeObject(private val name: String) : ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
  override fun toString(): String = "[object $name]"
}

/**
 * # Node API: `http`
 * Minimal behavior for createServer and basic lifecycle; other members are stubs.
 */
internal class NodeHttp private constructor (
  private val exec: GuestExecutorProvider?,
) : ReadOnlyProxyObject, HTTPAPI {
  private val ALL_MEMBERS = arrayOf(
    "Agent","ClientRequest","Server","ServerResponse","IncomingMessage","OutgoingMessage",
    "METHODS","STATUS_CODES","createServer","get","globalAgent","maxHeaderSize","request",
    "validateHeaderName","validateHeaderValue","setMaxIdleHTTPParsers"
  )

  internal companion object {
    @JvmStatic fun create(exec: GuestExecutorProvider? = null): NodeHttp = NodeHttp(exec)
  }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS
  override fun getMember(key: String?): Any? = when (key) {
    // Type-like placeholders
    "Agent","ClientRequest","Server","ServerResponse","IncomingMessage","OutgoingMessage" -> ReadOnlyTypeObject(key!!)

    // Constants (lightweight placeholders)
    "METHODS" -> ProxyArray.fromArray(*arrayOf("GET","POST","PUT","DELETE","HEAD","OPTIONS","PATCH"))
    "STATUS_CODES" -> ProxyObject.fromMap(mapOf<String, Any>(
      "200" to "OK", "404" to "Not Found", "500" to "Internal Server Error"
    ))
    "globalAgent" -> ReadOnlyTypeObject("Agent")
    "maxHeaderSize" -> 16384

    // API functions
    "validateHeaderName","validateHeaderValue","setMaxIdleHTTPParsers","get","request" -> ProxyExecutable { _ ->
      // not yet implemented; return minimal no-op or throw later
      null
    }

    "createServer" -> ProxyExecutable { argv: Array<Value> ->
      val maybeHandler = argv.getOrNull(0)
      val registry = ThreadLocalHandlerRegistry { reg ->
        // register the same handler order per-thread
        maybeHandler?.takeIf { it.canExecute() }?.let {
          reg.register(elide.runtime.intrinsics.server.http.internal.GuestHandler.simple(it))
        }
      }
      val router = PipelineRouter(registry)
      // also register on construction thread to align keys
      maybeHandler?.takeIf { it.canExecute() }?.let { router.handle(null, null, it) }

      val config = NettyServerConfig()
      val engine = NettyServerEngine(config, router, exec ?: GuestExecutorProvider { GuestExecution.direct() })

      object : ReadOnlyProxyObject {
        private var started = false

        override fun getMemberKeys(): Array<String> = arrayOf("listen","close","address","on")
        override fun getMember(k: String?): Any? = when (k) {
          "listen" -> ProxyExecutable { args: Array<Value> ->
            // parse (port?, host?, cb?)
            val port = args.getOrNull(0)?.takeIf { it.fitsInInt() }?.asInt()
            val host = args.getOrNull(1)?.takeIf { it.isString }?.asString()
            val cb = args.lastOrNull()?.takeIf { it.canExecute() }
            port?.let { config.port = it }
            host?.let { config.host = it }
            if (!started) {
              started = true
              // set up callback to execute after server is bound
              val callbackValue = ProxyExecutable { _: Array<Value> ->
                // emit 'listening' if an on('listening') was set via `on`
                _onListening?.let { it.execute() }
                cb?.execute()
                Value.asValue(null)
              }
              config.onBind(Value.asValue(callbackValue))
              engine.start()
            } else {
              cb?.execute()
            }
            this
          }

          "close" -> ProxyExecutable { args: Array<Value> ->
            val cb = args.getOrNull(0)?.takeIf { it.canExecute() }
            runCatching { engine.stop() }
            started = false
            cb?.execute()
            true
          }

          "address" -> ProxyExecutable { _: Array<Value> ->
            ProxyObject.fromMap(mapOf(
              "port" to config.port,
              "address" to config.host,
              "family" to (if (config.host.contains(":")) "IPv6" else "IPv4")
            ))
          }

          "on" -> ProxyExecutable { args: Array<Value> ->
            val evt = args.getOrNull(0)?.takeIf { it.isString }?.asString()
            val listener = args.getOrNull(1)?.takeIf { it.canExecute() }
            if (evt == "listening" && listener != null) _onListening = listener
            this
          }

          else -> null
        }

        // simple event storage for 'listening'
        private var _onListening: Value? = null
      }
    }

    else -> null
  }
}
