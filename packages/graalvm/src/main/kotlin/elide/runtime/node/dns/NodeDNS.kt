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
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.node.DNSAPI
import elide.runtime.lang.javascript.NodeModuleName

// Member names for the dns module.
private const val DNS_RESOLVER = "Resolver"
private const val DNS_GET_SERVERS = "getServers"
private const val DNS_SET_SERVERS = "setServers"
private const val DNS_LOOKUP = "lookup"
private const val DNS_LOOKUP_SERVICE = "lookupService"
private const val DNS_RESOLVE = "resolve"
private const val DNS_RESOLVE4 = "resolve4"
private const val DNS_RESOLVE6 = "resolve6"
private const val DNS_RESOLVE_ANY = "resolveAny"
private const val DNS_RESOLVE_CNAME = "resolveCname"
private const val DNS_RESOLVE_CAA = "resolveCaa"
private const val DNS_RESOLVE_MX = "resolveMx"
private const val DNS_RESOLVE_NAPTR = "resolveNaptr"
private const val DNS_RESOLVE_NS = "resolveNs"
private const val DNS_RESOLVE_PTR = "resolvePtr"
private const val DNS_RESOLVE_SOA = "resolveSoa"
private const val DNS_RESOLVE_SRV = "resolveSrv"
private const val DNS_RESOLVE_TLSA = "resolveTlsa"
private const val DNS_RESOLVE_TXT = "resolveTxt"
private const val DNS_REVERSE = "reverse"
private const val DNS_SET_DEFAULT_RESULT_ORDER = "setDefaultResultOrder"
private const val DNS_GET_DEFAULT_RESULT_ORDER = "getDefaultResultOrder"
private const val DNS_PROMISES = "promises"

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
 *
 * Implements the Node.js DNS module using native Rust hickory-dns resolver.
 */
internal class NodeDNS private constructor() : ReadOnlyProxyObject, DNSAPI {
  init {
    NativeDNS.initialize()
  }

  internal companion object {
    @JvmStatic fun create(): NodeDNS = NodeDNS()

    private val MEMBER_KEYS = arrayOf(
      DNS_RESOLVER,
      DNS_GET_SERVERS,
      DNS_SET_SERVERS,
      DNS_LOOKUP,
      DNS_LOOKUP_SERVICE,
      DNS_RESOLVE,
      DNS_RESOLVE4,
      DNS_RESOLVE6,
      DNS_RESOLVE_ANY,
      DNS_RESOLVE_CNAME,
      DNS_RESOLVE_CAA,
      DNS_RESOLVE_MX,
      DNS_RESOLVE_NAPTR,
      DNS_RESOLVE_NS,
      DNS_RESOLVE_PTR,
      DNS_RESOLVE_SOA,
      DNS_RESOLVE_SRV,
      DNS_RESOLVE_TLSA,
      DNS_RESOLVE_TXT,
      DNS_REVERSE,
      DNS_SET_DEFAULT_RESULT_ORDER,
      DNS_GET_DEFAULT_RESULT_ORDER,
      DNS_PROMISES,
    )
  }

  override fun getMemberKeys(): Array<String> = MEMBER_KEYS

  override fun getMember(key: String?): Any? = when (key) {
    DNS_RESOLVER -> DNSResolver()

    DNS_GET_SERVERS -> ProxyExecutable { _ ->
      arrayToProxyArray(NativeDNS.getServers())
    }

    DNS_SET_SERVERS -> ProxyExecutable { args ->
      val servers = valueToStringArray(args.getOrNull(0))
      NativeDNS.setServers(servers)
      null
    }

    DNS_LOOKUP -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val options = args.getOrNull(1)
      val callback = args.getOrNull(2) ?: args.getOrNull(1)?.takeIf { it.canExecute() }

      // Determine IP family from options
      val family = when {
        options?.hasMembers() == true -> options.getMember("family")?.asInt() ?: 0
        options?.isNumber == true -> options.asInt()
        else -> 0
      }

      val addresses = when (family) {
        4 -> NativeDNS.resolve4(hostname)
        6 -> NativeDNS.resolve6(hostname)
        else -> NativeDNS.resolveAny(hostname)
      }

      val address = addresses.firstOrNull() ?: ""
      val resolvedFamily = if (address.contains(":")) 6 else 4

      if (callback?.canExecute() == true) {
        if (address.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "getaddrinfo ENOTFOUND $hostname"), null, null)
        } else {
          callback.executeVoid(null, address, resolvedFamily)
        }
        null
      } else {
        // Return result object for promise-style usage
        createLookupResult(address, resolvedFamily)
      }
    }

    DNS_LOOKUP_SERVICE -> ProxyExecutable { args ->
      val address = args.getOrNull(0)?.asString() ?: ""
      val port = args.getOrNull(1)?.asInt() ?: 0
      val callback = args.getOrNull(2)

      val result = NativeDNS.lookupService(address, port)
      val parts = result.split(":")
      val hostname = parts.getOrNull(0) ?: ""
      val service = parts.getOrNull(1) ?: ""

      if (callback?.canExecute() == true) {
        if (result.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "getnameinfo ENOTFOUND $address"), null, null)
        } else {
          callback.executeVoid(null, hostname, service)
        }
        null
      } else {
        createServiceResult(hostname, service)
      }
    }

    DNS_RESOLVE -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val rrtype = args.getOrNull(1)?.asString() ?: "A"
      val callback = args.getOrNull(2) ?: args.getOrNull(1)?.takeIf { it.canExecute() }

      val results = when (rrtype.uppercase()) {
        "A" -> NativeDNS.resolve4(hostname)
        "AAAA" -> NativeDNS.resolve6(hostname)
        "ANY" -> NativeDNS.resolveAny(hostname)
        "CNAME" -> NativeDNS.resolveCname(hostname)
        "CAA" -> NativeDNS.resolveCaa(hostname)
        "MX" -> NativeDNS.resolveMx(hostname)
        "NAPTR" -> NativeDNS.resolveNaptr(hostname)
        "NS" -> NativeDNS.resolveNs(hostname)
        "PTR" -> NativeDNS.resolvePtr(hostname)
        "SOA" -> arrayOf(NativeDNS.resolveSoa(hostname)).filter { it.isNotEmpty() }.toTypedArray()
        "SRV" -> NativeDNS.resolveSrv(hostname)
        "TLSA" -> NativeDNS.resolveTlsa(hostname)
        "TXT" -> NativeDNS.resolveTxt(hostname)
        else -> NativeDNS.resolve4(hostname)
      }

      if (callback?.canExecute() == true) {
        if (results.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "queryA ENOTFOUND $hostname"), null)
        } else {
          callback.executeVoid(null, arrayToProxyArray(results))
        }
        null
      } else {
        arrayToProxyArray(results)
      }
    }

    DNS_RESOLVE4 -> createResolveFunction { hostname -> NativeDNS.resolve4(hostname) }
    DNS_RESOLVE6 -> createResolveFunction { hostname -> NativeDNS.resolve6(hostname) }
    DNS_RESOLVE_ANY -> createResolveFunction { hostname -> NativeDNS.resolveAny(hostname) }
    DNS_RESOLVE_CNAME -> createResolveFunction { hostname -> NativeDNS.resolveCname(hostname) }
    DNS_RESOLVE_CAA -> createResolveFunctionWithObjects { hostname ->
      NativeDNS.resolveCaa(hostname).map { caa ->
        // Format from Rust: "critical:tag:value"
        val parts = caa.split(":", limit = 3)
        val critical = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val tag = parts.getOrNull(1) ?: ""
        val value = parts.getOrNull(2) ?: ""
        mapOf(
          "critical" to critical,
          tag to value
        )
      }
    }
    DNS_RESOLVE_MX -> createResolveFunctionWithObjects { hostname ->
      NativeDNS.resolveMx(hostname).map { mx ->
        val parts = mx.split(":")
        mapOf(
          "priority" to (parts.getOrNull(0)?.toIntOrNull() ?: 0),
          "exchange" to (parts.getOrNull(1) ?: "")
        )
      }
    }
    DNS_RESOLVE_NAPTR -> createResolveFunctionWithObjects { hostname ->
      NativeDNS.resolveNaptr(hostname).map { naptr ->
        val parts = naptr.split(":")
        mapOf(
          "order" to (parts.getOrNull(0)?.toIntOrNull() ?: 0),
          "preference" to (parts.getOrNull(1)?.toIntOrNull() ?: 0),
          "flags" to (parts.getOrNull(2) ?: ""),
          "service" to (parts.getOrNull(3) ?: ""),
          "regexp" to (parts.getOrNull(4) ?: ""),
          "replacement" to (parts.getOrNull(5) ?: "")
        )
      }
    }
    DNS_RESOLVE_NS -> createResolveFunction { hostname -> NativeDNS.resolveNs(hostname) }
    DNS_RESOLVE_PTR -> createResolveFunction { hostname -> NativeDNS.resolvePtr(hostname) }
    DNS_RESOLVE_SOA -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val callback = args.getOrNull(1)

      val result = NativeDNS.resolveSoa(hostname)
      val parts = result.split(":")

      val soaObject = if (result.isNotEmpty()) {
        ProxyObject.fromMap(mapOf(
          "nsname" to (parts.getOrNull(0) ?: ""),
          "hostmaster" to (parts.getOrNull(1) ?: ""),
          "serial" to (parts.getOrNull(2)?.toLongOrNull() ?: 0L),
          "refresh" to (parts.getOrNull(3)?.toIntOrNull() ?: 0),
          "retry" to (parts.getOrNull(4)?.toIntOrNull() ?: 0),
          "expire" to (parts.getOrNull(5)?.toIntOrNull() ?: 0),
          "minttl" to (parts.getOrNull(6)?.toIntOrNull() ?: 0)
        ))
      } else null

      if (callback?.canExecute() == true) {
        if (soaObject == null) {
          callback.executeVoid(createDnsError("ENOTFOUND", "querySOA ENOTFOUND $hostname"), null)
        } else {
          callback.executeVoid(null, soaObject)
        }
        null
      } else {
        soaObject
      }
    }
    DNS_RESOLVE_SRV -> createResolveFunctionWithObjects { hostname ->
      NativeDNS.resolveSrv(hostname).map { srv ->
        val parts = srv.split(":")
        mapOf(
          "priority" to (parts.getOrNull(0)?.toIntOrNull() ?: 0),
          "weight" to (parts.getOrNull(1)?.toIntOrNull() ?: 0),
          "port" to (parts.getOrNull(2)?.toIntOrNull() ?: 0),
          "name" to (parts.getOrNull(3) ?: "")
        )
      }
    }
    DNS_RESOLVE_TLSA -> createResolveFunctionWithObjects { hostname ->
      NativeDNS.resolveTlsa(hostname).map { tlsa ->
        // Format from Rust: "certUsage:selector:matchingType:dataHex"
        val parts = tlsa.split(":", limit = 4)
        val dataHex = parts.getOrNull(3) ?: ""
        // Convert hex string to byte array for ArrayBuffer-like representation
        val dataBytes = dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        mapOf(
          "certUsage" to (parts.getOrNull(0)?.toIntOrNull() ?: 0),
          "selector" to (parts.getOrNull(1)?.toIntOrNull() ?: 0),
          "match" to (parts.getOrNull(2)?.toIntOrNull() ?: 0),
          "data" to dataBytes
        )
      }
    }
    DNS_RESOLVE_TXT -> createResolveFunctionWithArrays { hostname ->
      NativeDNS.resolveTxt(hostname).map { listOf(it) }
    }

    DNS_REVERSE -> ProxyExecutable { args ->
      val ip = args.getOrNull(0)?.asString() ?: ""
      val callback = args.getOrNull(1)

      val hostnames = NativeDNS.reverse(ip)

      if (callback?.canExecute() == true) {
        if (hostnames.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "getHostByAddr ENOTFOUND $ip"), null)
        } else {
          callback.executeVoid(null, arrayToProxyArray(hostnames))
        }
        null
      } else {
        arrayToProxyArray(hostnames)
      }
    }

    DNS_SET_DEFAULT_RESULT_ORDER -> ProxyExecutable { args ->
      val order = args.getOrNull(0)?.asString() ?: "verbatim"
      NativeDNS.setDefaultResultOrder(order)
      null
    }

    DNS_GET_DEFAULT_RESULT_ORDER -> ProxyExecutable { _ ->
      NativeDNS.getDefaultResultOrder()
    }

    DNS_PROMISES -> NodeDNSPromises.create()

    else -> null
  }

  private fun createResolveFunction(resolver: (String) -> Array<String>): ProxyExecutable {
    return ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val callback = args.getOrNull(1)

      val results = resolver(hostname)

      if (callback?.canExecute() == true) {
        if (results.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "query ENOTFOUND $hostname"), null)
        } else {
          callback.executeVoid(null, arrayToProxyArray(results))
        }
        null
      } else {
        arrayToProxyArray(results)
      }
    }
  }

  private fun createResolveFunctionWithObjects(resolver: (String) -> List<Map<String, Any>>): ProxyExecutable {
    return ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val callback = args.getOrNull(1)

      val results = resolver(hostname)
      val proxyResults = results.map { ProxyObject.fromMap(it) }

      if (callback?.canExecute() == true) {
        if (results.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "query ENOTFOUND $hostname"), null)
        } else {
          callback.executeVoid(null, listToProxyArray(proxyResults))
        }
        null
      } else {
        listToProxyArray(proxyResults)
      }
    }
  }

  private fun createResolveFunctionWithArrays(resolver: (String) -> List<List<String>>): ProxyExecutable {
    return ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val callback = args.getOrNull(1)

      val results = resolver(hostname)
      val proxyResults = results.map { inner -> listToProxyArray(inner) }

      if (callback?.canExecute() == true) {
        if (results.isEmpty()) {
          callback.executeVoid(createDnsError("ENOTFOUND", "query ENOTFOUND $hostname"), null)
        } else {
          callback.executeVoid(null, listToProxyArray(proxyResults))
        }
        null
      } else {
        listToProxyArray(proxyResults)
      }
    }
  }

  private fun arrayToProxyArray(arr: Array<String>): ProxyArray {
    return object : ProxyArray {
      override fun get(index: Long): Any = arr[index.toInt()]
      override fun set(index: Long, value: Value?) { /* read-only */ }
      override fun getSize(): Long = arr.size.toLong()
    }
  }

  private fun listToProxyArray(list: List<Any>): ProxyArray {
    return object : ProxyArray {
      override fun get(index: Long): Any = list[index.toInt()]
      override fun set(index: Long, value: Value?) { /* read-only */ }
      override fun getSize(): Long = list.size.toLong()
    }
  }

  private fun valueToStringArray(value: Value?): Array<String> {
    if (value == null || !value.hasArrayElements()) return emptyArray()
    val size = value.arraySize.toInt()
    return Array(size) { i -> value.getArrayElement(i.toLong()).asString() }
  }

  private fun createLookupResult(address: String, family: Int): ProxyObject {
    return ProxyObject.fromMap(mapOf(
      "address" to address,
      "family" to family
    ))
  }

  private fun createServiceResult(hostname: String, service: String): ProxyObject {
    return ProxyObject.fromMap(mapOf(
      "hostname" to hostname,
      "service" to service
    ))
  }

  private fun createDnsError(code: String, message: String): ProxyObject {
    return ProxyObject.fromMap(mapOf(
      "code" to code,
      "message" to message,
      "name" to "Error"
    ))
  }
}

/**
 * DNS Resolver class that can be instantiated with custom options.
 */
internal class DNSResolver : ReadOnlyProxyObject {
  private val memberKeys = arrayOf(
    "getServers",
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
    "resolveTlsa",
    "resolveTxt",
    "reverse",
    "setServers"
  )

  override fun getMemberKeys(): Array<String> = memberKeys

  override fun getMember(key: String?): Any? {
    // The Resolver class provides the same methods as the main dns module
    // For now, delegate to the global NativeDNS implementation
    return NodeDNS.create().getMember(key)
  }
}
