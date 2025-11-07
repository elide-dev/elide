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

import org.graalvm.polyglot.proxy.ProxyExecutable
import jakarta.inject.Provider
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.RuntimeExecutor
import elide.runtime.core.RuntimeLatch
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.CreateServerOptions
import elide.runtime.intrinsics.js.node.HttpAPI
import elide.runtime.intrinsics.js.node.ServerRequestListener
import elide.runtime.intrinsics.js.node.http.HttpServerAPI
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node `http` module into the intrinsic bindings.
@Intrinsic internal class NodeHttpModule(
  private val entrypointProvider: Provider<EntrypointRegistry>,
  private val runtimeLatch: Provider<RuntimeLatch>,
  private val executorProvider: Provider<RuntimeExecutor>,
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeHttp(executorProvider, runtimeLatch, entrypointProvider) }

  internal fun provide(): HttpAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.HTTP)) { provide() }
  }
}

/**
 * # Node API: `http`
 */
internal class NodeHttp(
  private val executor: Provider<RuntimeExecutor>,
  private val runtimeLatch: Provider<RuntimeLatch>,
  private val entrypoint: Provider<EntrypointRegistry>,
) : ReadOnlyProxyObject, HttpAPI {
  private val serverAdapter by lazy {
    NodeHttpServerHolder(
      entrypoint = entrypoint.get().acquire() ?: error("No entrypoint available"),
      runtimeLatch = runtimeLatch.get(),
      executor = executor.get().acquire(),
    )
  }

  override fun getMemberKeys(): Array<String> = MemberKeys
  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_CREATE_SERVER -> ProxyExecutable { args ->
      when (args.size) {
        1 -> if (args[0].canExecute()) createServer(null, args[0])
        else createServer(args[0], null)

        2 -> createServer(args[0], args[1])
        else -> createServer(args.getOrNull(0))
      }
    }

    else -> null
  }

  override fun createServer(options: CreateServerOptions?, listener: ServerRequestListener?): HttpServerAPI {
    val instance = NodeHttpServerInstance(serverAdapter, NodeHttpServerOptions.serverOptions(options))
    listener?.let { instance.addEventListener(HttpServerAPI.EVENT_REQUEST, it, null) }

    // if the application was bound in a different context, this sets the instance
    // as the handler for the current context; if this is a duplicate call within
    // the same context, it has no effect anyway
    serverAdapter.registerLocalInstance(instance)
    return instance
  }

  internal companion object {
    private const val MEMBER_CREATE_SERVER = "createServer"

    private val MemberKeys = arrayOf(MEMBER_CREATE_SERVER)
  }
}

