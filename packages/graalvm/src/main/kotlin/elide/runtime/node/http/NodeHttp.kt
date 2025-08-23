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

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.HTTPAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.intrinsics.server.http.internal.ThreadLocalHandlerRegistry
import elide.runtime.intrinsics.server.http.internal.PipelineRouter
import elide.runtime.intrinsics.server.http.netty.NettyServerEngine
import elide.runtime.intrinsics.server.http.netty.NettyServerConfig
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject

// Installs the Node `http` module into the intrinsic bindings.
@Intrinsic internal class NodeHttpModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp.create() }
  internal fun provide(): HTTPAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP)) { provide() }
  }
}

/**
 * # Node API: `http`
 */
internal class NodeHttp private constructor () : ReadOnlyProxyObject, HTTPAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeHttp = NodeHttp()
  }

  override fun getMemberKeys(): Array<String> = arrayOf("createServer")
  override fun getMember(key: String?): Any? = when (key) {
    "createServer" -> ProxyExecutable { args ->
      val initialListener = args.getOrNull(0)
      createServerObject(initialListener)
    }
    else -> null
  }

  private fun createServerObject(initialListener: Value?): ProxyObject {
    val registry = ThreadLocalHandlerRegistry(preInitialized = true) { /* no-op for now */ }
    val router = PipelineRouter(registry)
    val engine = NettyServerEngine(NettyServerConfig(), router, GuestExecutorProvider { GuestExecution.workStealing() })

    var listener: Value? = initialListener?.takeIf { it.canExecute() }

    return ProxyObject.fromMap(mutableMapOf(
      "on" to ProxyExecutable { a ->
        val event = a.getOrNull(0)?.asString()
        val cb = a.getOrNull(1)
        if (event == "request" && cb != null && cb.canExecute()) listener = cb
        null
      },
      "listen" to ProxyExecutable { a ->
        val port = a.getOrNull(0)?.asInt() ?: 0
        val host = a.getOrNull(1)?.takeIf { it.isString }?.asString()
        val cb = (if (a.size >= 3) a[2] else a.getOrNull(1))?.takeIf { it.canExecute() }
        cb?.let { (engine.config as NettyServerConfig).onBind(it) }
        router.handle(ProxyExecutable { argv ->
          val req = argv[0]
          val res = argv[1]
          val ctx = argv[2]
          val l = listener ?: return@ProxyExecutable true
          // Wrap res for Node-like surface
          val nodeRes = ProxyObject.fromMap(mutableMapOf(
            "setHeader" to ProxyExecutable { aa -> res.invokeMember("header", aa[0].asString(), aa[1].asString()); null },
            "getHeader" to ProxyExecutable { aa -> res.invokeMember("get", aa[0].asString()) },
            "removeHeader" to ProxyExecutable { aa -> res.invokeMember("remove", aa[0].asString()); null },
            "writeHead" to ProxyExecutable { aa -> res.invokeMember("status", aa[0].asInt()); null },
            "write" to ProxyExecutable { aa -> /* buffer not implemented; no-op */ null },
            "end" to ProxyExecutable { aa ->
              val status = res.getMember("status")?.asInt() ?: 200
              val body = aa.getOrNull(0)
              res.invokeMember("send", status, body)
              null
            },
            "headersSent" to false,
            "statusCode" to 200,
            "statusMessage" to "OK"
          ))
          l.executeVoid(req, nodeRes)
          true
        })
        (engine.config as NettyServerConfig).port = port
        host?.let { (engine.config as NettyServerConfig).host = it }
        engine.start()
        null
      },
      "close" to ProxyExecutable { _ ->
        // TODO: implement close when engine supports it
        null
      },
      "address" to ProxyExecutable { _ ->
        val cfg = engine.config as NettyServerConfig
        ProxyObject.fromMap(mapOf("port" to cfg.port, "address" to cfg.host, "family" to "IPv4"))
      }
    ))
  }
}
