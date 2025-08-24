/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.v8

import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.NodeAPI
import elide.runtime.lang.javascript.NodeModuleName

@Intrinsic internal class NodeV8Module : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeV8.create() }
  internal fun provide(): NodeAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.V8)) { singleton }
  }
}

/** Minimal `v8` module facade. */
internal class NodeV8 private constructor() : ReadOnlyProxyObject, NodeAPI {
  companion object { @JvmStatic fun create(): NodeV8 = NodeV8() }

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
  override fun toString(): String = "[object v8]"
}

