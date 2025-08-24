/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.trace

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.TraceEventsAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.lang.javascript.asJsSymbolString

private val MODULE_SYMBOL = "node_${NodeModuleName.TRACE_EVENTS.asJsSymbolString()}"

private const val F_CREATE_TRACING = "createTracing"
private const val F_GET_ENABLED_CATEGORIES = "getEnabledCategories"

private val ALL_MEMBERS = arrayOf(
  F_CREATE_TRACING,
  F_GET_ENABLED_CATEGORIES,
)

@Intrinsic internal class NodeTraceEventsModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeTraceEvents.create() }
  internal fun provide(): TraceEventsAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.TRACE_EVENTS)) { singleton }
  }
}

/** Minimal `trace_events` module facade. */
internal class NodeTraceEvents private constructor() : ReadOnlyProxyObject, TraceEventsAPI {
  companion object { @JvmStatic fun create(): NodeTraceEvents = NodeTraceEvents() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_CREATE_TRACING -> ProxyExecutable { args ->
      val opts = args.getOrNull(0)
      val categories = opts?.getMember("categories")?.takeIf { it.isString }?.asString() ?: ""
      object : ReadOnlyProxyObject {
        private var enabled = false
        override fun getMemberKeys(): Array<String> = arrayOf("enable","disable","enabled","categories")
        override fun getMember(k: String?): Any? = when (k) {
          "enable" -> ProxyExecutable { _: Array<org.graalvm.polyglot.Value> -> enabled = true; null }
          "disable" -> ProxyExecutable { _: Array<org.graalvm.polyglot.Value> -> enabled = false; null }
          "enabled" -> enabled
          "categories" -> categories
          else -> null
        }
      }
    }
    F_GET_ENABLED_CATEGORIES -> ProxyExecutable { _ -> "" }
    else -> null
  }
}

