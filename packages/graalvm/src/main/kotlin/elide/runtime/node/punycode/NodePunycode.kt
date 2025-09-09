/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.punycode

import java.net.IDN
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject

import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.PunycodeAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_DECODE = "decode"
private const val F_ENCODE = "encode"
private const val F_TO_ASCII = "toASCII"
private const val F_TO_UNICODE = "toUnicode"

private val ALL_MEMBERS = arrayOf(
  F_DECODE,
  F_ENCODE,
  F_TO_ASCII,
  F_TO_UNICODE,
)

@Intrinsic
@elide.annotations.Factory internal class NodePunycodeModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodePunycode.create() }
  @elide.annotations.Singleton internal fun provide(): PunycodeAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    val moduleInfo = ModuleInfo.of(NodeModuleName.PUNYCODE)
    ModuleRegistry.deferred(moduleInfo) { singleton }
  }
}

/** Minimal `punycode` module facade. */
internal class NodePunycode private constructor() : ReadOnlyProxyObject, PunycodeAPI {
  companion object { @JvmStatic fun create(): NodePunycode = NodePunycode() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_TO_ASCII -> ProxyExecutable { args ->
      val input = args.getOrNull(0)?.asString() ?: ""
      try { IDN.toASCII(input) } catch (_: Throwable) { "" }
    }
    F_TO_UNICODE -> ProxyExecutable { args ->
      val input = args.getOrNull(0)?.asString() ?: ""
      try { IDN.toUnicode(input) } catch (_: Throwable) { "" }
    }
    // Placeholders for raw punycode encode/decode (not domain functions)
    F_ENCODE -> ProxyExecutable { args ->
      val input = args.getOrNull(0)?.asString() ?: ""
      try { PunycodeAlgo.encode(input) } catch (_: Throwable) { "" }
    }
    F_DECODE -> ProxyExecutable { args ->
      val input = args.getOrNull(0)?.asString() ?: ""
      try { PunycodeAlgo.decode(input) } catch (_: Throwable) { "" }
    }
    else -> null
  }
}

