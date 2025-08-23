/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.node.dns

import org.graalvm.polyglot.proxy.ProxyExecutable
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.DNSPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node `dns/promises` module into the intrinsic bindings.
@Intrinsic internal class NodeDNSPromisesModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNSPromises.create() }
  internal fun provide(): DNSPromisesAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS_PROMISES)) { singleton }
  }
}

/**
 * # Node API: `dns/promises`
 */
internal class NodeDNSPromises private constructor () : ReadOnlyProxyObject, DNSPromisesAPI {
  private var defaultResultOrder: String = "verbatim"

  private fun resolveNow(hostname: String, ipv6: Boolean?): List<String> {
    val all = InetAddress.getAllByName(hostname)
    val filtered = when (ipv6) {
      true -> all.filterIsInstance<Inet6Address>()
      false -> all.filterIsInstance<Inet4Address>()
      else -> all.toList()
    }
    val addresses = filtered.map { it.hostAddress }
    return when (defaultResultOrder) {
      "ipv4first" -> addresses.sortedBy { if (it.contains(':')) 1 else 0 }
      else -> addresses
    }
  }

  private fun reverseNow(ip: String): List<String> = try {
    listOf(InetAddress.getByName(ip).hostName)
  } catch (_: Throwable) { emptyList() }

  internal companion object {
    @JvmStatic fun create(): NodeDNSPromises = NodeDNSPromises()
  }

  override fun getMemberKeys(): Array<String> = arrayOf(
    "Resolver",
    "getServers",
    "lookupService",
    "resolve",
    "resolve4",
    "resolve6",
    "resolveAny",
    "resolveCname",
    "resolveCaa",
    "resolveMx",
    "resolveNaptr",
    "resolveNs",
    "resolvePtr",
    "resolveSoa",
    "resolveSrv",
    "resolveTxt",
    "reverse",
    "setDefaultResultOrder",
    "getDefaultResultOrder",
  )
  override fun getMember(key: String?): Any? = when (key) {
    // Object/ctor stubs
    "Resolver" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = emptyArray()
      override fun getMember(key: String?): Any? = null
    }

    // Config
    "getServers" -> ProxyExecutable { emptyList<String>() }
    "setDefaultResultOrder" -> ProxyExecutable { args ->
      defaultResultOrder = args.getOrNull(0)?.asString() ?: "verbatim"
      null
    }
    "getDefaultResultOrder" -> ProxyExecutable { defaultResultOrder }

    // Promises API
    "resolve" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable JsPromise.resolved(emptyList<String>())
      try {
        JsPromise.resolved(resolveNow(hostname, null))
      } catch (t: Throwable) {
        JsPromise.rejected(t)
      }
    }
    "resolve4" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable JsPromise.resolved(emptyList<String>())
      try {
        JsPromise.resolved(resolveNow(hostname, false))
      } catch (t: Throwable) {
        JsPromise.rejected(t)
      }
    }
    "resolve6" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable JsPromise.resolved(emptyList<String>())
      try {
        JsPromise.resolved(resolveNow(hostname, true))
      } catch (t: Throwable) {
        JsPromise.rejected(t)
      }
    }
    "reverse" -> ProxyExecutable { args ->
      val ip = args.getOrNull(0)?.asString() ?: return@ProxyExecutable JsPromise.resolved(emptyList<String>())
      JsPromise.resolved(reverseNow(ip))
    }

    // Stubs for other rrtypes
    "lookupService", "resolveAny", "resolveCname", "resolveCaa", "resolveMx",
    "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa", "resolveSrv", "resolveTxt" ->
      ProxyExecutable { JsPromise.rejected("ENOTSUP") }

    else -> null
  }
}
