/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.worker

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.WorkerThreadsAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_IS_MAIN_THREAD = "isMainThread"
private const val F_WORKER = "Worker"
private const val P_PARENT_PORT = "parentPort"

private val ALL_MEMBERS = arrayOf(
  F_IS_MAIN_THREAD,
  F_WORKER,
  P_PARENT_PORT,
)

@Intrinsic internal class NodeWorkerThreadsModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeWorkerThreads.create() }
  internal fun provide(): WorkerThreadsAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.WORKER_THREADS)) { singleton }
  }
}

/** Minimal `worker_threads` module facade. */
internal class NodeWorkerThreads private constructor() : ReadOnlyProxyObject, WorkerThreadsAPI {
  companion object { @JvmStatic fun create(): NodeWorkerThreads = NodeWorkerThreads() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_IS_MAIN_THREAD -> true
    F_WORKER -> ProxyExecutable { args ->
      val _script = args.getOrNull(0)
      object : ReadOnlyProxyObject {
        private var onmessage: Any? = null
        override fun getMemberKeys(): Array<String> = arrayOf("terminate","postMessage","onmessage")
        override fun getMember(k: String?): Any? = when (k) {
          "terminate" -> ProxyExecutable { _: Array<org.graalvm.polyglot.Value> -> 0 }
          "postMessage" -> ProxyExecutable { argv: Array<org.graalvm.polyglot.Value> ->
            val msg = argv.getOrNull(0)
            // Immediately deliver to handler if set
            val handler = onmessage
            if (handler != null && handler is org.graalvm.polyglot.proxy.ProxyExecutable) {
              handler.execute(msg)
            }
            null
          }
          "onmessage" -> onmessage
          else -> null
        }
        override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
          if (key == "onmessage") onmessage = value
          else super.putMember(key, value)
        }
      }
    }
    P_PARENT_PORT -> object : ReadOnlyProxyObject {
      private var onmessage: Any? = null
      override fun getMemberKeys(): Array<String> = arrayOf("postMessage","onmessage")
      override fun getMember(k: String?): Any? = when (k) {
        "postMessage" -> ProxyExecutable { argv: Array<org.graalvm.polyglot.Value> ->
          val handler = onmessage
          if (handler is org.graalvm.polyglot.proxy.ProxyExecutable) handler.execute(argv.getOrNull(0))
          null
        }
        "onmessage" -> onmessage
        else -> null
      }
      override fun putMember(key: String?, value: org.graalvm.polyglot.Value?) {
        if (key == "onmessage") onmessage = value else super.putMember(key, value)
      }
    }
    else -> null
  }
}

