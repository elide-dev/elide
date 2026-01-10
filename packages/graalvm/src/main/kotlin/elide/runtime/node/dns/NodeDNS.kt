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
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.JsPromise.Companion.spawn
import elide.runtime.intrinsics.js.node.DNSAPI
import elide.runtime.lang.javascript.NodeModuleName

// -- Result Parsing --

internal sealed class DnsResult<out T> {
  data class Success<T>(val data: T) : DnsResult<T>()
  data class Error(val code: String, val message: String) : DnsResult<Nothing>()
}

internal fun parseArrayResult(result: Array<String>): DnsResult<List<String>> {
  if (result.isEmpty()) return DnsResult.Error("ENOTFOUND", "No results")
  val first = result[0]
  return when {
    first == "OK" -> DnsResult.Success(result.drop(1))
    ':' in first -> DnsResult.Error(first.substringBefore(':'), first.substringAfter(':'))
    else -> DnsResult.Error("EFORMERR", "Unexpected response format")
  }
}

internal fun parseStringResult(result: String): DnsResult<String> {
  if (result.isEmpty()) return DnsResult.Error("ENOTFOUND", "No results")
  val idx = result.indexOf(':')
  if (idx == -1) return DnsResult.Error("EFORMERR", "Unexpected response format")
  val prefix = result.substring(0, idx)
  val data = result.substring(idx + 1)
  return if (prefix == "OK") DnsResult.Success(data) else DnsResult.Error(prefix, data)
}

// -- Proxy Helpers --

internal fun List<*>.toProxyArray(): ProxyArray = object : ProxyArray {
  override fun get(index: Long): Any? = this@toProxyArray[index.toInt()]
  override fun set(index: Long, value: Value?) {}
  override fun getSize(): Long = this@toProxyArray.size.toLong()
}

internal fun Array<String>.toProxyArray(): ProxyArray = toList().toProxyArray()

internal fun Value?.toStringArray(): Array<String> {
  if (this == null || !hasArrayElements()) return emptyArray()
  return Array(arraySize.toInt()) { getArrayElement(it.toLong()).asString() }
}

internal fun dnsError(code: String, message: String): ProxyObject =
  ProxyObject.fromMap(mapOf("code" to code, "message" to message, "name" to "Error"))

internal fun <T> DnsResult<T>.map(transform: (T) -> Any): DnsResult<Any> = when (this) {
  is DnsResult.Success -> DnsResult.Success(transform(data))
  is DnsResult.Error -> this
}

internal class DnsException(val code: String, override val message: String) : Exception("$code: $message")

internal fun resolveByType(hostname: String, type: String): Array<String> = when (type) {
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

// -- Record Parsers --

internal object RecordParsers {
  fun mx(raw: String): Map<String, Any> = raw.split(":").let {
    mapOf("priority" to (it.getOrNull(0)?.toIntOrNull() ?: 0), "exchange" to (it.getOrNull(1) ?: ""))
  }

  fun srv(raw: String): Map<String, Any> = raw.split(":").let {
    mapOf(
      "priority" to (it.getOrNull(0)?.toIntOrNull() ?: 0),
      "weight" to (it.getOrNull(1)?.toIntOrNull() ?: 0),
      "port" to (it.getOrNull(2)?.toIntOrNull() ?: 0),
      "name" to (it.getOrNull(3) ?: "")
    )
  }

  fun naptr(raw: String): Map<String, Any> = raw.split(":").let {
    mapOf(
      "order" to (it.getOrNull(0)?.toIntOrNull() ?: 0),
      "preference" to (it.getOrNull(1)?.toIntOrNull() ?: 0),
      "flags" to (it.getOrNull(2) ?: ""),
      "service" to (it.getOrNull(3) ?: ""),
      "regexp" to (it.getOrNull(4) ?: ""),
      "replacement" to (it.getOrNull(5) ?: "")
    )
  }

  fun caa(raw: String): Map<String, Any> = raw.split(":", limit = 3).let {
    val critical = it.getOrNull(0)?.toIntOrNull() ?: 0
    val tag = it.getOrNull(1) ?: ""
    val value = it.getOrNull(2) ?: ""
    mapOf("critical" to critical, tag to value)
  }

  fun soa(raw: String): Map<String, Any> = raw.split(":").let {
    mapOf(
      "nsname" to (it.getOrNull(0) ?: ""),
      "hostmaster" to (it.getOrNull(1) ?: ""),
      "serial" to (it.getOrNull(2)?.toLongOrNull() ?: 0L),
      "refresh" to (it.getOrNull(3)?.toIntOrNull() ?: 0),
      "retry" to (it.getOrNull(4)?.toIntOrNull() ?: 0),
      "expire" to (it.getOrNull(5)?.toIntOrNull() ?: 0),
      "minttl" to (it.getOrNull(6)?.toIntOrNull() ?: 0)
    )
  }

  fun tlsa(raw: String): Map<String, Any> = raw.split(":", limit = 4).let {
    val hex = it.getOrNull(3) ?: ""
    val data = if (hex.isNotEmpty()) hex.chunked(2).map { b -> b.toInt(16).toByte() }.toByteArray() else byteArrayOf()
    mapOf(
      "certUsage" to (it.getOrNull(0)?.toIntOrNull() ?: 0),
      "selector" to (it.getOrNull(1)?.toIntOrNull() ?: 0),
      "match" to (it.getOrNull(2)?.toIntOrNull() ?: 0),
      "data" to data
    )
  }

  fun any(record: String): Map<String, Any> {
    val idx = record.indexOf(':')
    if (idx == -1) return mapOf("type" to "UNKNOWN", "value" to record)
    val type = record.substring(0, idx)
    val data = record.substring(idx + 1)
    return when (type) {
      "A" -> data.split(":").let { mapOf("type" to "A", "address" to it[0], "ttl" to (it.getOrNull(1)?.toIntOrNull() ?: 0)) }
      "AAAA" -> {
        val lastColon = data.lastIndexOf(':')
        mapOf("type" to "AAAA", "address" to data.substring(0, lastColon), "ttl" to (data.substring(lastColon + 1).toIntOrNull() ?: 0))
      }
      "MX" -> mx(data) + ("type" to "MX")
      "TXT" -> mapOf("type" to "TXT", "entries" to listOf(data))
      "NS" -> mapOf("type" to "NS", "value" to data)
      "CNAME" -> mapOf("type" to "CNAME", "value" to data)
      "SOA" -> soa(data) + ("type" to "SOA")
      else -> mapOf("type" to type, "value" to data)
    }
  }
}

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
