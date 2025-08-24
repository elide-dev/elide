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
import elide.runtime.intrinsics.js.node.NodeAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_IS_MAIN_THREAD = "isMainThread"
private const val F_WORKER = "Worker"

private val ALL_MEMBERS = arrayOf(
  F_IS_MAIN_THREAD,
  F_WORKER,
)

@Intrinsic internal class NodeWorkerThreadsModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeWorkerThreads.create() }
  internal fun provide(): NodeAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.WORKER_THREADS)) { singleton }
  }
}

/** Minimal `worker_threads` module facade. */
internal class NodeWorkerThreads private constructor() : ReadOnlyProxyObject, NodeAPI {
  companion object { @JvmStatic fun create(): NodeWorkerThreads = NodeWorkerThreads() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_IS_MAIN_THREAD -> true
    F_WORKER -> ProxyExecutable { throw UnsupportedOperationException("Worker not yet implemented") }
    else -> null
  }
}

