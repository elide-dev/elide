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

import org.graalvm.polyglot.Value
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
import elide.runtime.intrinsics.js.node.DNSAPI
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node `dns` module into the intrinsic bindings.
@Intrinsic internal class NodeDNSModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNS.create() }
  internal fun provide(): DNSAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS)) { singleton }
  }
}

/**
 * # Node API: `dns`
 */
internal class NodeDNS private constructor () : ReadOnlyProxyObject, DNSAPI {
  // simple default result order handling ("verbatim" | "ipv4first")
  private var defaultResultOrder: String = "verbatim"

  private fun resolve(hostname: String, ipv6: Boolean?): List<String> {
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

  private fun reverseLookup(ip: String): List<String> = try {
    listOf(InetAddress.getByName(ip).hostName)
  } catch (_: Throwable) {
    emptyList()
  }

  internal companion object {
    @JvmStatic fun create(): NodeDNS = NodeDNS()
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
    // minimal constructor/object stubs
    "Resolver" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = emptyArray()
      override fun getMember(key: String?): Any? = null
    }

    // DNS configuration
    "getServers" -> ProxyExecutable { emptyList<String>() }
    "setDefaultResultOrder" -> ProxyExecutable { args ->
      defaultResultOrder = args.getOrNull(0)?.asString() ?: "verbatim"
      null
    }
    "getDefaultResultOrder" -> ProxyExecutable { defaultResultOrder }

    // generic resolve(hostname[, rrtype][, callback]) -> return addresses; if callback provided, node-style
    "resolve" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable null
      val callback = args.getOrNull(1)?.takeIf { it.canExecute() } ?: args.getOrNull(2)?.takeIf { it?.canExecute() == true }
      return@ProxyExecutable try {
        val res = resolve(hostname, null)
        if (callback != null) {
          callback.execute(null, res)
          null
        } else res
      } catch (t: Throwable) {
        if (callback != null) {
          callback.execute(t.message ?: t.toString(), null)
          null
        } else null
      }
    }

    // resolve4(hostname[, callback])
    "resolve4" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable null
      val callback = args.getOrNull(1)?.takeIf { it.canExecute() }
      return@ProxyExecutable try {
        val res = resolve(hostname, false)
        if (callback != null) {
          callback.execute(null, res)
          null
        } else res
      } catch (t: Throwable) {
        if (callback != null) {
          callback.execute(t.message ?: t.toString(), null)
          null
        } else null
      }
    }

    // resolve6(hostname[, callback])
    "resolve6" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: return@ProxyExecutable null
      val callback = args.getOrNull(1)?.takeIf { it.canExecute() }
      return@ProxyExecutable try {
        val res = resolve(hostname, true)
        if (callback != null) {
          callback.execute(null, res)
          null
        } else res
      } catch (t: Throwable) {
        if (callback != null) {
          callback.execute(t.message ?: t.toString(), null)
          null
        } else null
      }
    }

    // reverse(ip[, callback])
    "reverse" -> ProxyExecutable { args ->
      val ip = args.getOrNull(0)?.asString() ?: return@ProxyExecutable null
      val callback = args.getOrNull(1)?.takeIf { it.canExecute() }
      val res = reverseLookup(ip)
      if (callback != null) {
        callback.execute(null, res)
        null
      } else res
    }

    // stubs for other rrtypes and methods
    "lookupService", "resolveAny", "resolveCname", "resolveCaa", "resolveMx",
    "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa", "resolveSrv", "resolveTxt" ->
      ProxyExecutable { args ->
        val cb = args.lastOrNull()?.takeIf { it?.canExecute() == true }
        if (cb != null) {
          cb.execute("ENOTSUP", null)
          null
        } else emptyList<Any>()
      }

    else -> null
  }
}
