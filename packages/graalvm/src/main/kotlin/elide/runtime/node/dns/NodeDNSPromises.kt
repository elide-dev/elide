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
import org.graalvm.polyglot.proxy.ProxyInstantiable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.annotations.Inject
import elide.runtime.exec.GuestExecutor
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.loader.ModuleInfo
import elide.runtime.gvm.loader.ModuleRegistry
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.interop.toProxyArray
import elide.runtime.interop.toStringArray
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.JsPromise.Companion.spawn
import elide.runtime.intrinsics.js.node.DNSPromisesAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.node.dns.DnsHelpers.parseArrayResult
import elide.runtime.node.dns.DnsHelpers.parseStringResult
import elide.runtime.node.dns.DnsHelpers.resolveByType

@Intrinsic internal class NodeDNSPromisesModule @Inject constructor(
  private val executorProvider: GuestExecutorProvider,
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNSPromises.create(executorProvider.executor()) }
  internal fun provide(): DNSPromisesAPI = singleton
  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS_PROMISES)) { singleton }
  }
}

internal class NodeDNSPromises private constructor(private val exec: GuestExecutor) : ReadOnlyProxyObject, DNSPromisesAPI {
  init { NativeDNS.initialize() }

  companion object {
    @JvmStatic fun create(exec: GuestExecutor): NodeDNSPromises = NodeDNSPromises(exec)
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
    "Resolver" -> DNSPromiseResolverFactory(exec)
    "getServers" -> ProxyExecutable { JsPromise.resolved(NativeDNS.getServers().toProxyArray()) }
    "setServers" -> ProxyExecutable { args -> NativeDNS.setServers(args.getOrNull(0).toStringArray()); JsPromise.resolved(null) }
    "getDefaultResultOrder" -> ProxyExecutable { JsPromise.resolved(NativeDNS.getDefaultResultOrder()) }
    "setDefaultResultOrder" -> ProxyExecutable { args -> NativeDNS.setDefaultResultOrder(args.getOrNull(0)?.asString() ?: "verbatim"); JsPromise.resolved(null) }

    "lookup" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val opts = args.getOrNull(1)
      val family = when {
        opts?.hasMembers() == true -> opts.getMember("family")?.asInt() ?: 0
        opts?.isNumber == true -> opts.asInt()
        else -> 0
      }
      val all = opts?.hasMembers() == true && opts.getMember("all")?.asBoolean() == true
      asyncPromise {
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
    }

    "lookupService" -> ProxyExecutable { args ->
      val addr = args.getOrNull(0)?.asString() ?: ""
      val port = args.getOrNull(1)?.asInt() ?: 0
      asyncPromise {
        parseStringResult(NativeDNS.lookupService(addr, port)).map { data ->
          val (host, svc) = data.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
          ProxyObject.fromMap(mapOf("hostname" to host, "service" to svc))
        }
      }
    }

    "resolve" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val rrtype = (args.getOrNull(1)?.asString() ?: "A").uppercase()
      asyncPromise {
        if (rrtype == "SOA") {
          parseStringResult(NativeDNS.resolveSoa(hostname)).map { ProxyObject.fromMap(RecordParsers.soa(it)) }
        } else {
          parseArrayResult(resolveByType(hostname, rrtype)).map { it.toProxyArray() }
        }
      }
    }

    "resolve4" -> asyncResolvePromise { NativeDNS.resolve4(it) }
    "resolve6" -> asyncResolvePromise { NativeDNS.resolve6(it) }
    "resolveCname" -> asyncResolvePromise { NativeDNS.resolveCname(it) }
    "resolveNs" -> asyncResolvePromise { NativeDNS.resolveNs(it) }
    "resolvePtr" -> asyncResolvePromise { NativeDNS.resolvePtr(it) }
    "reverse" -> asyncResolvePromise { NativeDNS.reverse(it) }

    "resolveAny" -> asyncResolveTransform({ NativeDNS.resolveAny(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.any(it)) }.toProxyArray()
    }
    "resolveMx" -> asyncResolveTransform({ NativeDNS.resolveMx(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.mx(it)) }.toProxyArray()
    }
    "resolveSrv" -> asyncResolveTransform({ NativeDNS.resolveSrv(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.srv(it)) }.toProxyArray()
    }
    "resolveNaptr" -> asyncResolveTransform({ NativeDNS.resolveNaptr(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.naptr(it)) }.toProxyArray()
    }
    "resolveCaa" -> asyncResolveTransform({ NativeDNS.resolveCaa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.caa(it)) }.toProxyArray()
    }
    "resolveTlsa" -> asyncResolveTransform({ NativeDNS.resolveTlsa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.tlsa(it)) }.toProxyArray()
    }
    "resolveTxt" -> asyncResolveTransform({ NativeDNS.resolveTxt(it) }) { list ->
      list.map { listOf(it).toProxyArray() }.toProxyArray()
    }
    "resolveSoa" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      asyncPromise {
        parseStringResult(NativeDNS.resolveSoa(hostname)).map { ProxyObject.fromMap(RecordParsers.soa(it)) }
      }
    }

    else -> null
  }

  // Execute operation asynchronously and return a promise
  private fun asyncPromise(op: () -> DnsResult<Any>): JsPromise<Any?> = exec.spawn {
    when (val r = op()) {
      is DnsResult.Success -> r.data
      is DnsResult.Error -> throw DnsException(r.code, r.message)
    }
  }

  private fun asyncResolvePromise(resolver: (String) -> Array<String>): ProxyExecutable = ProxyExecutable { args ->
    val hostname = args.getOrNull(0)?.asString() ?: ""
    asyncPromise { parseArrayResult(resolver(hostname)).map { it.toProxyArray() } }
  }

  private fun asyncResolveTransform(
    resolver: (String) -> Array<String>,
    transform: (List<String>) -> Any
  ): ProxyExecutable = ProxyExecutable { args ->
    val hostname = args.getOrNull(0)?.asString() ?: ""
    asyncPromise { parseArrayResult(resolver(hostname)).map(transform) }
  }
}

// Factory for creating DNS Promise Resolver instances (supports `new dns.promises.Resolver()`)
internal class DNSPromiseResolverFactory(private val exec: GuestExecutor) : ProxyInstantiable {
  override fun newInstance(vararg arguments: Value?): Any = DNSPromiseResolver(exec)
}

internal class DNSPromiseResolver(private val exec: GuestExecutor) : ReadOnlyProxyObject {
  private val dns = NodeDNSPromises.create(exec)
  override fun getMemberKeys(): Array<String> = arrayOf(
    "getServers", "resolve", "resolve4", "resolve6", "resolveAny", "resolveCname",
    "resolveCaa", "resolveMx", "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa",
    "resolveSrv", "resolveTlsa", "resolveTxt", "reverse", "setServers"
  )
  override fun getMember(key: String?): Any? = dns.getMember(key)
}
