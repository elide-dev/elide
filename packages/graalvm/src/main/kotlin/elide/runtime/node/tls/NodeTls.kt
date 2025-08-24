/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 * Licensed under the MIT license.
 */
package elide.runtime.node.tls

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyArray
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.TLSAPI
import elide.runtime.lang.javascript.NodeModuleName

private const val F_CREATE_SERVER = "createServer"
private const val F_CONNECT = "connect"
private const val F_CREATE_SECURE_CONTEXT = "createSecureContext"
private const val F_GET_CIPHERS = "getCiphers"
private const val P_DEFAULT_MIN_VERSION = "DEFAULT_MIN_VERSION"
private const val P_DEFAULT_MAX_VERSION = "DEFAULT_MAX_VERSION"

private val ALL_MEMBERS = arrayOf(
  F_CREATE_SERVER,
  F_CONNECT,
  F_CREATE_SECURE_CONTEXT,
  F_GET_CIPHERS,
  P_DEFAULT_MIN_VERSION,
  P_DEFAULT_MAX_VERSION,
)

@Intrinsic internal class NodeTlsModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeTls.create() }
  internal fun provide(): TLSAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.TLS)) { singleton }
  }
}

/** Minimal `tls` module facade. */
internal class NodeTls private constructor() : ReadOnlyProxyObject, TLSAPI {
  companion object { @JvmStatic fun create(): NodeTls = NodeTls() }

  override fun getMemberKeys(): Array<String> = ALL_MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    F_CREATE_SERVER -> ProxyExecutable { _: Array<Value> ->
      // Return a minimal server-like object
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("close")
        override fun getMember(k: String?): Any? = when (k) {
          "close" -> ProxyExecutable { _: Array<Value> -> null }
          else -> null
        }
      }
    }
    F_CONNECT -> ProxyExecutable { _: Array<Value> ->
      // Return a minimal socket-like object
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("end")
        override fun getMember(k: String?): Any? = when (k) {
          "end" -> ProxyExecutable { _: Array<Value> -> null }
          else -> null
        }
      }
    }
    F_CREATE_SECURE_CONTEXT -> ProxyExecutable { args ->
      val opts = args.getOrNull(0)
      // Return options echo object for now
      object : ReadOnlyProxyObject {
        override fun getMemberKeys(): Array<String> = arrayOf("context")
        override fun getMember(k: String?): Any? = when (k) {
          "context" -> opts ?: Value.asValue(null)
          else -> null
        }
      }
    }
    F_GET_CIPHERS -> ProxyExecutable { _: Array<Value> ->
      val ciphers = arrayOf(
        "TLS_AES_256_GCM_SHA384",
        "TLS_AES_128_GCM_SHA256",
        "TLS_CHACHA20_POLY1305_SHA256",
      )
      ProxyArray.fromArray(*ciphers)
    }
    P_DEFAULT_MIN_VERSION -> "TLSv1.2"
    P_DEFAULT_MAX_VERSION -> "TLSv1.3"
    else -> null
  }
}

