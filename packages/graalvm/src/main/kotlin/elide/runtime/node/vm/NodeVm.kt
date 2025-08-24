/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.vm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.VMAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_CREATE_CONTEXT = "createContext"
private const val F_IS_CONTEXT = "isContext"
private const val F_RUN_IN_CONTEXT = "runInContext"
private const val F_RUN_IN_NEW_CONTEXT = "runInNewContext"
private const val F_RUN_IN_THIS_CONTEXT = "runInThisContext"

// Internal symbol to brand VM contexts
private const val VM_CONTEXT_BRAND = "__elide_vm_context__"

private val ALL_MEMBERS = arrayOf(
  F_CREATE_CONTEXT,
  F_IS_CONTEXT,
  F_RUN_IN_CONTEXT,
  F_RUN_IN_NEW_CONTEXT,
  F_RUN_IN_THIS_CONTEXT,
)

@Intrinsic internal class NodeVmModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeVm.create() }
  internal fun provide(): VMAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.VM)) { singleton }
  }
}

/** Minimal `vm` module facade. */
internal class NodeVm private constructor() : ReadOnlyProxyObject, VMAPI {
  companion object { @JvmStatic fun create(): NodeVm = NodeVm() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_CREATE_CONTEXT -> ProxyExecutable { args ->
      // Return a shallow copy of the provided sandbox (object literal) or an empty object; brand it
      val sandbox = args.getOrNull(0)
      val ctxObj = if (sandbox != null && sandbox.hasMembers()) {
        val map = mutableMapOf<String, Any?>()
        sandbox.memberKeys?.forEach { k ->
          val v = sandbox.getMember(k)
          if (v != null && !v.isNull) map[k] = v
        }
        ProxyObject.fromMap(map)
      } else ProxyObject.fromMap(mutableMapOf())
      (ctxObj as ProxyObject).putMember(VM_CONTEXT_BRAND, Value.asValue(true))
      ctxObj
    }
    F_IS_CONTEXT -> ProxyExecutable { args ->
      val obj = args.getOrNull(0)
      obj != null && obj.hasMembers() && (obj.getMember(VM_CONTEXT_BRAND)?.asBoolean() == true)
    }
    F_RUN_IN_THIS_CONTEXT -> ProxyExecutable { args ->
      val code = args.getOrNull(0)?.asString() ?: ""
      if (code.isEmpty()) return@ProxyExecutable null
      Context.getCurrent().eval("js", code)
    }
    F_RUN_IN_NEW_CONTEXT -> ProxyExecutable { args ->
      val code = args.getOrNull(0)?.asString() ?: ""
      if (code.isEmpty()) return@ProxyExecutable null
      val sandbox = args.getOrNull(1)
      val fresh = Context.newBuilder("js").allowAllAccess(true).build()
      try {
        val bindings = fresh.getBindings("js")
        if (sandbox != null && sandbox.hasMembers()) {
          sandbox.memberKeys?.forEach { k ->
            val v = sandbox.getMember(k)
            bindings.putMember(k, v ?: Value.asValue(null))
          }
        }
        fresh.eval("js", code)
      } finally { fresh.close() }
    }
    F_RUN_IN_CONTEXT -> ProxyExecutable { args ->
      val code = args.getOrNull(0)?.asString() ?: ""
      if (code.isEmpty()) return@ProxyExecutable null
      val ctx = args.getOrNull(1)
      val fresh = Context.newBuilder("js").allowAllAccess(true).build()
      try {
        val bindings = fresh.getBindings("js")
        if (ctx != null && ctx.hasMembers()) {
          ctx.memberKeys?.forEach { k ->
            val v = ctx.getMember(k)
            bindings.putMember(k, v ?: Value.asValue(null))
          }
        }
        fresh.eval("js", code)
      } finally { fresh.close() }
    }
    else -> null
  }
}

