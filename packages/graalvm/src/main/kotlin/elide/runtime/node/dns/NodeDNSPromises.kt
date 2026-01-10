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
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.DNSPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName

@Intrinsic internal class NodeDNSPromisesModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNSPromises.create() }
  internal fun provide(): DNSPromisesAPI = singleton
  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS_PROMISES)) { singleton }
  }
}

internal class NodeDNSPromises private constructor() : ReadOnlyProxyObject, DNSPromisesAPI {
  init { NativeDNS.initialize() }

  companion object {
    @JvmStatic fun create(): NodeDNSPromises = NodeDNSPromises()
    private val MEMBERS = arrayOf(
      "Resolver", "getServers", "setServers", "lookup", "lookupService",
      "resolve", "resolve4", "resolve6", "resolveAny", "resolveCname", "resolveCaa",
      "resolveMx", "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa",
      "resolveSrv", "resolveTlsa", "resolveTxt", "reverse",
      "setDefaultResultOrder", "getDefaultResultOrder"
    )
  }

  override fun getMemberKeys(): Array<String> = MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    "Resolver" -> DNSPromiseResolver()
    "getServers" -> ProxyExecutable { JsPromise.resolved(NativeDNS.getServers().toProxyArray()) }
    "setServers" -> ProxyExecutable { args -> NativeDNS.setServers(args.getOrNull(0).toStringArray()); JsPromise.resolved(null) }
    "getDefaultResultOrder" -> ProxyExecutable { JsPromise.resolved(NativeDNS.getDefaultResultOrder()) }
    "setDefaultResultOrder" -> ProxyExecutable { args -> NativeDNS.setDefaultResultOrder(args.getOrNull(0)?.asString() ?: "verbatim"); JsPromise.resolved(null) }

    "lookup" -> promisify { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val opts = args.getOrNull(1)
      val family = when {
        opts?.hasMembers() == true -> opts.getMember("family")?.asInt() ?: 0
        opts?.isNumber == true -> opts.asInt()
        else -> 0
      }
      val all = opts?.hasMembers() == true && opts.getMember("all")?.asBoolean() == true
      val raw = NativeDNS.lookup(hostname, family, all)
      parseArrayResult(raw).map { list ->
        if (all) {
          list.map { entry ->
            val (addr, fam) = entry.split(":").let { it[0] to it[1].toInt() }
            ProxyObject.fromMap(mapOf("address" to addr, "family" to fam))
          }.toProxyArray()
        } else {
          val (addr, fam) = list.firstOrNull()?.split(":")?.let { it[0] to it[1].toInt() } ?: ("" to 4)
          ProxyObject.fromMap(mapOf("address" to addr, "family" to fam))
        }
      }
    }

    "lookupService" -> promisify { args ->
      val addr = args.getOrNull(0)?.asString() ?: ""
      val port = args.getOrNull(1)?.asInt() ?: 0
      parseStringResult(NativeDNS.lookupService(addr, port)).map { data ->
        val (host, svc) = data.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        ProxyObject.fromMap(mapOf("hostname" to host, "service" to svc))
      }
    }

    "resolve" -> promisify { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val rrtype = (args.getOrNull(1)?.asString() ?: "A").uppercase()
      if (rrtype == "SOA") {
        parseStringResult(NativeDNS.resolveSoa(hostname)).map { ProxyObject.fromMap(RecordParsers.soa(it)) }
      } else {
        parseArrayResult(resolveByType(hostname, rrtype)).map { it.toProxyArray() }
      }
    }

    "resolve4" -> resolvePromise { NativeDNS.resolve4(it) }
    "resolve6" -> resolvePromise { NativeDNS.resolve6(it) }
    "resolveCname" -> resolvePromise { NativeDNS.resolveCname(it) }
    "resolveNs" -> resolvePromise { NativeDNS.resolveNs(it) }
    "resolvePtr" -> resolvePromise { NativeDNS.resolvePtr(it) }
    "reverse" -> resolvePromise { NativeDNS.reverse(it) }

    "resolveAny" -> resolvePromiseTransform({ NativeDNS.resolveAny(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.any(it)) }.toProxyArray()
    }
    "resolveMx" -> resolvePromiseTransform({ NativeDNS.resolveMx(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.mx(it)) }.toProxyArray()
    }
    "resolveSrv" -> resolvePromiseTransform({ NativeDNS.resolveSrv(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.srv(it)) }.toProxyArray()
    }
    "resolveNaptr" -> resolvePromiseTransform({ NativeDNS.resolveNaptr(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.naptr(it)) }.toProxyArray()
    }
    "resolveCaa" -> resolvePromiseTransform({ NativeDNS.resolveCaa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.caa(it)) }.toProxyArray()
    }
    "resolveTlsa" -> resolvePromiseTransform({ NativeDNS.resolveTlsa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.tlsa(it)) }.toProxyArray()
    }
    "resolveTxt" -> resolvePromiseTransform({ NativeDNS.resolveTxt(it) }) { list ->
      list.map { listOf(it).toProxyArray() }.toProxyArray()
    }
    "resolveSoa" -> promisify { args ->
      parseStringResult(NativeDNS.resolveSoa(args.getOrNull(0)?.asString() ?: ""))
        .map { ProxyObject.fromMap(RecordParsers.soa(it)) }
    }

    else -> null
  }

  private fun resolveByType(hostname: String, type: String): Array<String> = when (type) {
    "A" -> NativeDNS.resolve4(hostname)
    "AAAA" -> NativeDNS.resolve6(hostname)
    "ANY" -> NativeDNS.resolveAny(hostname)
    "CNAME" -> NativeDNS.resolveCname(hostname)
    "CAA" -> NativeDNS.resolveCaa(hostname)
    "MX" -> NativeDNS.resolveMx(hostname)
    "NAPTR" -> NativeDNS.resolveNaptr(hostname)
    "NS" -> NativeDNS.resolveNs(hostname)
    "PTR" -> NativeDNS.resolvePtr(hostname)
    "SRV" -> NativeDNS.resolveSrv(hostname)
    "TLSA" -> NativeDNS.resolveTlsa(hostname)
    "TXT" -> NativeDNS.resolveTxt(hostname)
    else -> NativeDNS.resolve4(hostname)
  }

  private fun <T> DnsResult<T>.map(transform: (T) -> Any): DnsResult<Any> = when (this) {
    is DnsResult.Success -> DnsResult.Success(transform(data))
    is DnsResult.Error -> this
  }

  private fun promisify(block: (Array<out org.graalvm.polyglot.Value>) -> DnsResult<Any>): ProxyExecutable =
    ProxyExecutable { args ->
      when (val r = block(args)) {
        is DnsResult.Success -> JsPromise.resolved(r.data)
        is DnsResult.Error -> JsPromise.rejected(Exception("${r.code}: ${r.message}"))
      }
    }

  private fun resolvePromise(resolver: (String) -> Array<String>): ProxyExecutable = promisify { args ->
    parseArrayResult(resolver(args.getOrNull(0)?.asString() ?: "")).map { it.toProxyArray() }
  }

  private fun resolvePromiseTransform(
    resolver: (String) -> Array<String>,
    transform: (List<String>) -> Any
  ): ProxyExecutable = promisify { args ->
    parseArrayResult(resolver(args.getOrNull(0)?.asString() ?: "")).map(transform)
  }
}

internal class DNSPromiseResolver : ReadOnlyProxyObject {
  private val dns = NodeDNSPromises.create()
  override fun getMemberKeys(): Array<String> = arrayOf(
    "getServers", "resolve", "resolve4", "resolve6", "resolveAny", "resolveCname",
    "resolveCaa", "resolveMx", "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa",
    "resolveSrv", "resolveTlsa", "resolveTxt", "reverse", "setServers"
  )
  override fun getMember(key: String?): Any? = dns.getMember(key)
}
