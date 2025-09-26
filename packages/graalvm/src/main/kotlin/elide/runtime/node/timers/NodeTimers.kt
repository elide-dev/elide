/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.timers

import elide.annotations.Factory
import elide.annotations.Singleton
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.internals.js.JsTimersIntrinsic

private const val SET_TIMEOUT = "setTimeout"
private const val CLEAR_TIMEOUT = "clearTimeout"
private const val SET_INTERVAL = "setInterval"
private const val CLEAR_INTERVAL = "clearInterval"
private const val SET_IMMEDIATE = "setImmediate"
private const val CLEAR_IMMEDIATE = "clearImmediate"

private val ALL_MEMBERS = arrayOf(
  SET_TIMEOUT,
  CLEAR_TIMEOUT,
  SET_INTERVAL,
  CLEAR_INTERVAL,
  SET_IMMEDIATE,
  CLEAR_IMMEDIATE,
)

@Intrinsic
@Factory internal class NodeTimersModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): NodeTimers = NodeTimers.obtain()
  override fun install(bindings: MutableIntrinsicBindings) {
    // Ensure JS global timer intrinsics (setTimeout, clearTimeout, setInterval, clearInterval) are installed
    JsTimersIntrinsic().install(bindings)
    val moduleInfo = ModuleInfo.of(NodeModuleName.TIMERS)
    ModuleRegistry.deferred(moduleInfo) { provide() }
  }
}

/** Node API: `timers` */
internal class NodeTimers private constructor() : ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  private fun jsBinding(name: String): Value = Context.getCurrent()
    .getBindings("js")
    .getMember(name)
    ?: Value.asValue(null)

  override fun getMember(key: String?): Any? = when (key) {
    SET_TIMEOUT, SET_INTERVAL, CLEAR_TIMEOUT, CLEAR_INTERVAL -> jsBinding(key!!).takeIf { !it.isNull }
      ?: ProxyExecutable { args -> /* noop placeholder if timers not mounted yet */ null }

    SET_IMMEDIATE -> ProxyExecutable { args ->
      // Implement as setTimeout(cb, 0, ...args)
      val cb = args.getOrNull(0) ?: return@ProxyExecutable null
      val rest = if (args.size > 1) args.copyOfRange(1, args.size) else emptyArray()
      val setTimeout = jsBinding(SET_TIMEOUT)
      setTimeout.execute(0, cb, *rest)
    }

    CLEAR_IMMEDIATE -> ProxyExecutable { args ->
      // Implement as clearTimeout(id)
      val id = args.getOrNull(0) ?: return@ProxyExecutable null
      val clearTimeout = jsBinding(CLEAR_TIMEOUT)
      clearTimeout.executeVoid(id)
      null
    }

    else -> null
  }

  companion object {
    private val SINGLETON = NodeTimers()
    fun obtain(): NodeTimers = SINGLETON
  }
}

