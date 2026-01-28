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
import elide.runtime.intrinsics.js.JsPromise.Companion.spawn
import elide.runtime.intrinsics.js.node.DNSAPI
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.node.dns.DnsHelpers.dnsError
import elide.runtime.node.dns.DnsHelpers.parseArrayResult
import elide.runtime.node.dns.DnsHelpers.parseStringResult
import elide.runtime.node.dns.DnsHelpers.resolveByType

// -- Module --

@Intrinsic internal class NodeDNSModule @Inject constructor(
  private val executorProvider: GuestExecutorProvider,
) : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeDNS.create(executorProvider.executor()) }
  internal fun provide(): DNSAPI = singleton
  override fun install(bindings: MutableIntrinsicBindings) {
    ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.DNS)) { singleton }
  }
}

internal class NodeDNS private constructor(private val exec: GuestExecutor) : ReadOnlyProxyObject, DNSAPI {
  init { NativeDNS.initialize() }

  companion object {
    @JvmStatic fun create(exec: GuestExecutor): NodeDNS = NodeDNS(exec)
    private val MEMBERS = arrayOf(
      "Resolver", "getServers", "setServers", "lookup", "lookupService",
      "resolve", "resolve4", "resolve6", "resolveAny", "resolveCname", "resolveCaa",
      "resolveMx", "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa",
      "resolveSrv", "resolveTlsa", "resolveTxt", "reverse",
      "setDefaultResultOrder", "getDefaultResultOrder", "promises"
    )
  }

  override fun getMemberKeys(): Array<String> = MEMBERS

  override fun getMember(key: String?): Any? = when (key) {
    "Resolver" -> DNSResolverFactory(exec)
    "getServers" -> ProxyExecutable { NativeDNS.getServers().toProxyArray() }
    "setServers" -> ProxyExecutable { args -> NativeDNS.setServers(args.getOrNull(0).toStringArray()); null }
    "getDefaultResultOrder" -> ProxyExecutable { NativeDNS.getDefaultResultOrder() }
    "setDefaultResultOrder" -> ProxyExecutable { args -> NativeDNS.setDefaultResultOrder(args.getOrNull(0)?.asString() ?: "verbatim"); null }
    "promises" -> NodeDNSPromises.create(exec)

    "lookup" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val opts = args.getOrNull(1)
      val cb = args.getOrNull(2) ?: opts?.takeIf { it.canExecute() }
      val family = when {
        opts?.hasMembers() == true -> opts.getMember("family")?.asInt() ?: 0
        opts?.isNumber == true -> opts.asInt()
        else -> 0
      }
      val all = opts?.hasMembers() == true && opts.getMember("all")?.asBoolean() == true
      asyncOp(cb) {
        val raw = NativeDNS.lookup(hostname, family, all)
        when (val r = parseArrayResult(raw)) {
          is DnsResult.Success -> {
            if (all) {
              val results = r.data.map { entry ->
                val (addr, fam) = entry.split(":").let { it[0] to it[1].toInt() }
                ProxyObject.fromMap(mapOf("address" to addr, "family" to fam))
              }.toProxyArray()
              DnsCallbackResult.Success(results)
            } else {
              val (addr, fam) = r.data.firstOrNull()?.split(":")?.let { it[0] to it[1].toInt() } ?: ("" to 4)
              DnsCallbackResult.SuccessMulti(addr, fam)
            }
          }
          is DnsResult.Error -> DnsCallbackResult.Error(r.code, r.message)
        }
      }
    }

    "lookupService" -> ProxyExecutable { args ->
      val addr = args.getOrNull(0)?.asString() ?: ""
      val port = args.getOrNull(1)?.asInt() ?: 0
      val cb = args.getOrNull(2)
      asyncOp(cb) {
        when (val r = parseStringResult(NativeDNS.lookupService(addr, port))) {
          is DnsResult.Success -> {
            val (host, svc) = r.data.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            DnsCallbackResult.SuccessMulti(host, svc)
          }
          is DnsResult.Error -> DnsCallbackResult.Error(r.code, r.message)
        }
      }
    }

    "resolve" -> ProxyExecutable { args ->
      val hostname = args.getOrNull(0)?.asString() ?: ""
      val rrtype = args.getOrNull(1)?.asString() ?: "A"
      val cb = args.getOrNull(2) ?: args.getOrNull(1)?.takeIf { it.canExecute() }
      if (rrtype.uppercase() == "SOA") return@ProxyExecutable asyncResolveSoa(hostname, cb)
      asyncResolve(cb) { parseArrayResult(resolveByType(hostname, rrtype.uppercase())).map { it.toProxyArray() } }
    }

    "resolve4" -> asyncResolveSimple { NativeDNS.resolve4(it) }
    "resolve6" -> asyncResolveSimple { NativeDNS.resolve6(it) }
    "resolveCname" -> asyncResolveSimple { NativeDNS.resolveCname(it) }
    "resolveNs" -> asyncResolveSimple { NativeDNS.resolveNs(it) }
    "resolvePtr" -> asyncResolveSimple { NativeDNS.resolvePtr(it) }

    "resolveAny" -> asyncResolveWithTransform({ NativeDNS.resolveAny(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.any(it)) }.toProxyArray()
    }
    "resolveMx" -> asyncResolveWithTransform({ NativeDNS.resolveMx(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.mx(it)) }.toProxyArray()
    }
    "resolveSrv" -> asyncResolveWithTransform({ NativeDNS.resolveSrv(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.srv(it)) }.toProxyArray()
    }
    "resolveNaptr" -> asyncResolveWithTransform({ NativeDNS.resolveNaptr(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.naptr(it)) }.toProxyArray()
    }
    "resolveCaa" -> asyncResolveWithTransform({ NativeDNS.resolveCaa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.caa(it)) }.toProxyArray()
    }
    "resolveTlsa" -> asyncResolveWithTransform({ NativeDNS.resolveTlsa(it) }) { list ->
      list.map { ProxyObject.fromMap(RecordParsers.tlsa(it)) }.toProxyArray()
    }
    "resolveTxt" -> asyncResolveWithTransform({ NativeDNS.resolveTxt(it) }) { list ->
      list.map { listOf(it).toProxyArray() }.toProxyArray()
    }
    "resolveSoa" -> ProxyExecutable { args -> asyncResolveSoa(args.getOrNull(0)?.asString() ?: "", args.getOrNull(1)) }

    "reverse" -> asyncResolveSimple { NativeDNS.reverse(it) }

    else -> null
  }

  // Callback result types for async operations
  private sealed class DnsCallbackResult {
    data class Success(val data: Any) : DnsCallbackResult()
    data class SuccessMulti(val arg1: Any, val arg2: Any) : DnsCallbackResult()
    data class Error(val code: String, val message: String) : DnsCallbackResult()
  }

  // Execute operation asynchronously and invoke callback when done
  private fun asyncOp(cb: Value?, op: () -> DnsCallbackResult): Any? {
    if (cb == null) {
      // Synchronous fallback when no callback provided
      return when (val r = op()) {
        is DnsCallbackResult.Success -> r.data
        is DnsCallbackResult.SuccessMulti -> ProxyObject.fromMap(mapOf("arg1" to r.arg1, "arg2" to r.arg2))
        is DnsCallbackResult.Error -> dnsError(r.code, r.message)
      }
    }
    exec.spawn {
      when (val r = op()) {
        is DnsCallbackResult.Success -> cb.executeVoid(null, r.data)
        is DnsCallbackResult.SuccessMulti -> cb.executeVoid(null, r.arg1, r.arg2)
        is DnsCallbackResult.Error -> cb.executeVoid(dnsError(r.code, r.message), null)
      }
    }
    return null
  }

  private fun asyncResolve(cb: Value?, op: () -> DnsResult<Any>): Any? {
    if (cb == null) {
      return when (val r = op()) {
        is DnsResult.Success -> r.data
        is DnsResult.Error -> dnsError(r.code, r.message)
      }
    }
    exec.spawn {
      when (val r = op()) {
        is DnsResult.Success -> cb.executeVoid(null, r.data)
        is DnsResult.Error -> cb.executeVoid(dnsError(r.code, r.message), null)
      }
    }
    return null
  }

  private fun asyncResolveSoa(hostname: String, cb: Value?): Any? {
    return asyncResolve(cb) {
      parseStringResult(NativeDNS.resolveSoa(hostname)).map { ProxyObject.fromMap(RecordParsers.soa(it)) }
    }
  }

  private fun asyncResolveSimple(resolver: (String) -> Array<String>): ProxyExecutable = ProxyExecutable { args ->
    val hostname = args.getOrNull(0)?.asString() ?: ""
    val cb = args.getOrNull(1)
    asyncResolve(cb) { parseArrayResult(resolver(hostname)).map { it.toProxyArray() } }
  }

  private fun asyncResolveWithTransform(
    resolver: (String) -> Array<String>,
    transform: (List<String>) -> ProxyArray
  ): ProxyExecutable = ProxyExecutable { args ->
    val hostname = args.getOrNull(0)?.asString() ?: ""
    val cb = args.getOrNull(1)
    asyncResolve(cb) { parseArrayResult(resolver(hostname)).map(transform) }
  }
}

// Factory for creating DNS Resolver instances (supports `new dns.Resolver()`)
internal class DNSResolverFactory(private val exec: GuestExecutor) : ProxyInstantiable {
  override fun newInstance(vararg arguments: Value?): Any = DNSResolver(exec)
}

internal class DNSResolver(private val exec: GuestExecutor) : ReadOnlyProxyObject {
  private val dns = NodeDNS.create(exec)
  override fun getMemberKeys(): Array<String> = arrayOf(
    "getServers", "resolve", "resolve4", "resolve6", "resolveAny", "resolveCname",
    "resolveCaa", "resolveMx", "resolveNaptr", "resolveNs", "resolvePtr", "resolveSoa",
    "resolveSrv", "resolveTlsa", "resolveTxt", "reverse", "setServers"
  )
  override fun getMember(key: String?): Any? = dns.getMember(key)
}
