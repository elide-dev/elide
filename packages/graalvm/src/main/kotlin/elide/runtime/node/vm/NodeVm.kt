/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.vm

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.NodeAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_CREATE_CONTEXT = "createContext"
private const val F_IS_CONTEXT = "isContext"
private const val F_RUN_IN_CONTEXT = "runInContext"
private const val F_RUN_IN_NEW_CONTEXT = "runInNewContext"
private const val F_RUN_IN_THIS_CONTEXT = "runInThisContext"

private val ALL_MEMBERS = arrayOf(
  F_CREATE_CONTEXT,
  F_IS_CONTEXT,
  F_RUN_IN_CONTEXT,
  F_RUN_IN_NEW_CONTEXT,
  F_RUN_IN_THIS_CONTEXT,
)

@Intrinsic internal class NodeVmModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeVm.create() }
  internal fun provide(): NodeAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.VM)) { singleton }
  }
}

/** Minimal `vm` module facade. */
internal class NodeVm private constructor() : ReadOnlyProxyObject, NodeAPI {
  companion object { @JvmStatic fun create(): NodeVm = NodeVm() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_CREATE_CONTEXT, F_IS_CONTEXT, F_RUN_IN_CONTEXT, F_RUN_IN_NEW_CONTEXT, F_RUN_IN_THIS_CONTEXT ->
      ProxyExecutable { throw UnsupportedOperationException("vm.$key not yet implemented") }
    else -> null
  }
}

