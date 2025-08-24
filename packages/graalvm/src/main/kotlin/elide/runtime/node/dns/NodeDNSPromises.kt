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

import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
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
  private var defaultOrder: String = "verbatim"

  internal companion object {
    @JvmStatic fun create(): NodeDNSPromises = NodeDNSPromises()
  }

  private fun addressesFor(host: String, family: String? = null): Array<String> {
    val addrs = try { java.net.InetAddress.getAllByName(host).toList() } catch (_: Throwable) { emptyList() }
    val filtered = when (family) {
      "A" -> addrs.filterIsInstance<java.net.Inet4Address>()
      "AAAA" -> addrs.filterIsInstance<java.net.Inet6Address>()
      else -> addrs
    }
    val ordered = when (defaultOrder) {
      "ipv4first" -> filtered.sortedWith(compareBy({ it is java.net.Inet6Address }))
      else -> filtered
    }
    return ordered.map { it.hostAddress }.toTypedArray()
  }

  override fun getMemberKeys(): Array<String> = arrayOf(
    "Resolver",
    "getServers",
    "resolve",
    "resolve4",
    "resolve6",
    "reverse",
    "setDefaultResultOrder",
    "getDefaultResultOrder",
  )

  override fun getMember(key: String?): Any? = when (key) {
    "Resolver" -> object : ReadOnlyProxyObject {
      override fun getMemberKeys(): Array<String> = arrayOf("resolve","resolve4","resolve6","reverse")
      override fun getMember(k: String?): Any? = when (k) {
        "resolve" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host)))
        }
        "resolve4" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "A")))
        }
        "resolve6" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val host = args.getOrNull(0)?.asString() ?: ""
          elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "AAAA")))
        }
        "reverse" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
          val ip = args.getOrNull(0)?.asString() ?: ""
          val name = try { java.net.InetAddress.getByName(ip).hostName } catch (_: Throwable) { "" }
          val arr = if (name.isBlank()) org.graalvm.polyglot.proxy.ProxyArray.fromArray() else org.graalvm.polyglot.proxy.ProxyArray.fromArray(name)
          elide.runtime.intrinsics.js.JsPromise.resolved(arr)
        }
        else -> null
      }
    }

    "getServers" -> org.graalvm.polyglot.proxy.ProxyExecutable { _ ->
      elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray())
    }

    "resolve" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host)))
    }

    "resolve4" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "A")))
    }

    "resolve6" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val host = args.getOrNull(0)?.asString() ?: ""
      elide.runtime.intrinsics.js.JsPromise.resolved(org.graalvm.polyglot.proxy.ProxyArray.fromArray(*addressesFor(host, "AAAA")))
    }

    "reverse" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val ip = args.getOrNull(0)?.asString() ?: ""
      val name = try { java.net.InetAddress.getByName(ip).hostName } catch (_: Throwable) { "" }
      val arr = if (name.isBlank()) org.graalvm.polyglot.proxy.ProxyArray.fromArray() else org.graalvm.polyglot.proxy.ProxyArray.fromArray(name)
      elide.runtime.intrinsics.js.JsPromise.resolved(arr)
    }

    "setDefaultResultOrder" -> org.graalvm.polyglot.proxy.ProxyExecutable { args ->
      val mode = args.getOrNull(0)?.asString()?.lowercase() ?: "verbatim"
      defaultOrder = if (mode == "ipv4first") "ipv4first" else "verbatim"
      elide.runtime.intrinsics.js.JsPromise.resolved(defaultOrder)
    }

    "getDefaultResultOrder" -> org.graalvm.polyglot.proxy.ProxyExecutable { _ -> elide.runtime.intrinsics.js.JsPromise.resolved(defaultOrder) }

    else -> null
  }
}
