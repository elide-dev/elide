/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.async

import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.AsyncHooksAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_CREATE_HOOKS = "createHook"
private const val F_EXECUTION_ASYNC_ID = "executionAsyncId"
private const val F_TRIGGER_ASYNC_ID = "triggerAsyncId"

private val ALL_MEMBERS = arrayOf(
  F_CREATE_HOOKS,
  F_EXECUTION_ASYNC_ID,
  F_TRIGGER_ASYNC_ID,
)

@Intrinsic internal class NodeAsyncHooksModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeAsyncHooks.create() }
  internal fun provide(): AsyncHooksAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.ASYNC_HOOKS)) { singleton }
  }
}

/** Minimal `async_hooks` module facade. */
internal class NodeAsyncHooks private constructor() : ReadOnlyProxyObject, AsyncHooksAPI {
  companion object { @JvmStatic fun create(): NodeAsyncHooks = NodeAsyncHooks() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_CREATE_HOOKS, F_EXECUTION_ASYNC_ID, F_TRIGGER_ASYNC_ID -> ProxyExecutable { _ -> 0 }
    else -> null
  }
}

