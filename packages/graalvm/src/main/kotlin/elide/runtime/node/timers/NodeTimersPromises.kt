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
import elide.runtime.intrinsics.js.JsPromise.Companion.promise
import elide.runtime.lang.javascript.NodeModuleName
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable

private const val P_SET_TIMEOUT = "setTimeout"
private const val P_SET_IMMEDIATE = "setImmediate"

private val ALL_MEMBERS = arrayOf(
  P_SET_TIMEOUT,
  P_SET_IMMEDIATE,
)

@Intrinsic
@Factory internal class NodeTimersPromisesModule : AbstractNodeBuiltinModule() {
  @Singleton fun provide(): NodeTimersPromises = NodeTimersPromises.obtain()
  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.TIMERS_PROMISES)) { provide() }
  }
}

/** Node API: `timers/promises` */
internal class NodeTimersPromises private constructor() : ReadOnlyProxyObject {
  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  private fun jsBinding(name: String): Value = Context.getCurrent()
    .getBindings("js")
    .getMember(name)

  override fun getMember(key: String?): Any? = when (key) {
    P_SET_TIMEOUT -> ProxyExecutable { args ->
      // setTimeout(delay, value?, options?) — emulate Node semantics
      val ms = args.getOrNull(0)?.asLong()?.coerceAtLeast(0) ?: 0L
      val value = args.getOrNull(1)
      val options = args.getOrNull(2)
      val signal = options?.getMember("signal")?.takeIf { it.hasMembers() }
      promise<Value> {
        if (signal != null && signal.getMember("aborted")?.asBoolean() == true) {
          val reason = signal.getMember("reason")?.takeIf { !it.isNull }
          reject(reason ?: Value.asValue(elide.runtime.gvm.js.JsError.valueError("Aborted")))
          return@promise
        }
        val setTimeout = jsBinding("setTimeout")
        val timeoutCb = ProxyExecutable {
          if (signal != null && signal.getMember("aborted")?.asBoolean() == true) {
            val reason = signal.getMember("reason")?.takeIf { !it.isNull }
            reject(reason ?: Value.asValue(elide.runtime.gvm.js.JsError.valueError("Aborted")))
          } else {
            resolve(value ?: Value.asValue(null))
          }
        }
        // schedule
        setTimeout.execute(ms, timeoutCb)
      }
    }

    P_SET_IMMEDIATE -> ProxyExecutable { args ->
      val value = args.getOrNull(0)
      promise<Value> {
        val setTimeout = jsBinding("setTimeout")
        setTimeout.execute(0, ProxyExecutable { resolve(value ?: Value.asValue(null)) })
      }
    }

    else -> null
  }

  companion object {
    private val SINGLETON = NodeTimersPromises()
    fun obtain(): NodeTimersPromises = SINGLETON
  }
}

