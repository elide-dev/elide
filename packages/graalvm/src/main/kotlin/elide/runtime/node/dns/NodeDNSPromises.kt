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
import elide.runtime.exec.GuestExecution
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.JsPromise.Companion.promise
import elide.runtime.intrinsics.js.node.DNSPromisesAPI
import org.graalvm.polyglot.Value
import elide.runtime.lang.javascript.NodeModuleName

// Installs the Node `dns/promises` module into the intrinsic bindings.
@Intrinsic internal class NodeDNSPromisesModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNSPromises.create() }
  internal fun provide(): DNSPromisesAPI = singleton

  override fun install(bindings: MutableIntrinsicBindings) {
    val moduleInfo = ModuleInfo.of(NodeModuleName.DNS_PROMISES)
    ModuleRegistry.deferred(moduleInfo) { singleton }
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



  private fun addressesFor(host: String, family: String? = null): Array<String> {
    val addrs = try { java.net.InetAddress.getAllByName(host).toList() } catch (_: Throwable) { emptyList() }
    val filtered = when (family) {
      "A" -> addrs.filterIsInstance<java.net.Inet4Address>()
      "AAAA" -> addrs.filterIsInstance<java.net.Inet6Address>()
      null -> addrs
      else -> addrs
    }
    val ordered = when (defaultResultOrder) {
      "ipv4first" -> filtered.sortedWith(compareBy({ it is java.net.Inet6Address }))
      else -> filtered
    }
    return ordered.map { it.hostAddress }.toTypedArray()
  }

  override fun getMemberKeys(): Array<String> = arrayOf(
    "Resolver",
    "getServers",
    "lookupService",
    "resolve",
    "resolve4",
    "resolve6",
    "reverse",
    "setDefaultResultOrder",
    "getDefaultResultOrder",
    // unsupported types (present to match Node shape; reject on call)
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
  )

  override fun getMember(key: String?): Any? = when (key) {
    "default" -> this

    "Resolver" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = arrayOf("resolve","resolve4","resolve6","reverse")
      override fun getMember(k: String?): Any? = when (k) {
        "resolve" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          val arr = org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host))
          elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(arr))
        }
        "resolve4" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          val arr = org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "A"))
          elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(arr))
        }
        "resolve6" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          val arr = org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "AAAA"))
          elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(arr))
        }
        "reverse" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val ip = args.getOrNull(0)?.asString() ?: ""
          val name = try { java.net.InetAddress.getByName(ip).hostName } catch (_: Throwable) { "" }
          val arr = if (name.isBlank()) org.graalvm.polyglot.proxy.ProxyArray.fromArray() else org.graalvm.polyglot.proxy.ProxyArray.fromArray(name)
          elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(arr))
        }
        else -> null
      }
    }

    "getServers" -> org.graalvm.polyglot.proxy.ProxyExecutable { _ ->
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(org.graalvm.polyglot.proxy.ProxyArray.fromArray()))
    }

    "lookupService" -> org.graalvm.polyglot.proxy.ProxyExecutable { _ ->
      // Minimal stub: return empty array; Node returns hostnames for service lookup.
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(org.graalvm.polyglot.proxy.ProxyArray.fromArray()))
    }

    "resolve" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host))))
    }

    "resolve4" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "A"))))
    }

    "resolve6" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "AAAA"))))
    }

    "reverse" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val ip = args.getOrNull(0)?.asString() ?: ""
      val name = try { java.net.InetAddress.getByName(ip).hostName } catch (_: Throwable) { "" }
      val arr = if (name.isBlank()) org.graalvm.polyglot.proxy.ProxyArray.fromArray() else org.graalvm.polyglot.proxy.ProxyArray.fromArray(name)
      elide.runtime.intrinsics.js.JsPromise.resolved(Value.asValue(arr))
    }

    "setDefaultResultOrder" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val mode = args.getOrNull(0)?.asString()?.lowercase() ?: "verbatim"
      defaultResultOrder = if (mode == "ipv4first") "ipv4first" else "verbatim"
      // Node's dns/promises setDefaultResultOrder is synchronous and returns void; return the string for convenience.
      defaultResultOrder
    }

    "getDefaultResultOrder" -> org.graalvm.polyglot.proxy.ProxyExecutable { _ ->
      // Node's dns/promises getDefaultResultOrder is synchronous and returns the current order.
      defaultResultOrder
    }

    // Unsupported RR types: reject with ENOTSUP per test expectation (as a JS string)
    "resolveAny" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveCname" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveCaa" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveMx" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveNaptr" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveNs" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolvePtr" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveSoa" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveSrv" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }
    "resolveTxt" -> ProxyExecutable { elide.runtime.intrinsics.js.JsPromise.rejected<Value>("ENOTSUP") }

    else -> null

}
}
