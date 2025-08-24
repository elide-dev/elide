/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.wasi

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyInstantiable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.WASIAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val P_WASI = "WASI"

private val ALL_MEMBERS = arrayOf(
  P_WASI,
)

@Intrinsic internal class NodeWasiModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeWasi.create() }
  internal fun provide(): WASIAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of("wasi")) { singleton }
  }
}

/** Minimal `wasi` module facade. */
internal class NodeWasi private constructor() : ReadOnlyProxyObject, WASIAPI {
  companion object { @JvmStatic fun create(): NodeWasi = NodeWasi() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    P_WASI -> ProxyInstantiable { args ->
      // Accept options but do nothing
      val opts = args.getOrNull(0)
      when {
        opts == null || opts.isNull -> Unit
        opts.hasMembers() -> Unit
        else -> throw IllegalArgumentException("WASI constructor expects an options object")
      }
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = emptyArray()
        override fun getMember(key: String?): Any? = null
        override fun putMember(key: String?, value: Value?): Unit = error("Cannot modify `WASI` instance")
      }
    }
    else -> null
  }
}

