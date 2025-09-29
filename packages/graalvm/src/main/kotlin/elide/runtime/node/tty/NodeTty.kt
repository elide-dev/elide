/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.tty

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.TtyAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_ISATTY = "isatty"

private val ALL_MEMBERS = arrayOf(F_ISATTY)

@Intrinsic internal class NodeTtyModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeTty.create() }
  internal fun provide(): TtyAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.TTY)) { singleton }
  }
}

/** Minimal `tty` module facade. */
internal class NodeTty private constructor() : ReadOnlyProxyObject, TtyAPI {
  companion object { @JvmStatic fun create(): NodeTty = NodeTty() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_ISATTY -> ProxyExecutable { _ -> false }
    else -> null
  }
}

