/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.repl

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.NodeAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_START = "start"

private val ALL_MEMBERS = arrayOf(F_START)

@Intrinsic internal class NodeReplModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeRepl.create() }
  internal fun provide(): NodeAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.REPL)) { singleton }
  }
}

/** Minimal `repl` module facade. */
internal class NodeRepl private constructor() : ReadOnlyProxyObject, NodeAPI {
  companion object { @JvmStatic fun create(): NodeRepl = NodeRepl() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_START -> ProxyExecutable { _ ->
      throw UnsupportedOperationException("repl.start not yet implemented")
    }
    else -> null
  }
}

