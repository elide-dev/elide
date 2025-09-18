/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.repl

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.ReplAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_START = "start"

private val ALL_MEMBERS = arrayOf(F_START)

@Intrinsic internal class NodeReplModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeRepl.create() }
  internal fun provide(): ReplAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.REPL)) { singleton }
  }
}

/** Minimal `repl` module facade. */
internal class NodeRepl private constructor() : ReadOnlyProxyObject, ReplAPI {
  companion object { @JvmStatic fun create(): NodeRepl = NodeRepl() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_START -> ProxyExecutable { args ->
      val opts = args.getOrNull(0)
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("close","write")
        override fun getMember(k: String?): Any? = when (k) {
          "close" -> ProxyExecutable { _: Array<org.graalvm.polyglot.Value> -> null }
          "write" -> ProxyExecutable { argv: Array<org.graalvm.polyglot.Value> ->
            val code = argv.getOrNull(0)?.asString() ?: return@ProxyExecutable null
            Context.getCurrent().eval("js", code)
          }
          else -> null
        }
      }
    }
    else -> null
  }
}

