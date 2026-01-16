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

import org.graalvm.polyglot.proxy.ProxyObject

/** Result wrapper for DNS operations. */
internal sealed class DnsResult<out T> {
  data class Success<T>(val data: T) : DnsResult<T>()
  data class Error(val code: String, val message: String) : DnsResult<Nothing>()
}

/** Exception thrown for DNS errors in promise-based operations. */
internal class DnsException(val code: String, override val message: String) : Exception("$code: $message")

/** Helpers for DNS result parsing and error handling. */
internal object DnsHelpers {
  /** Parse an array result from native DNS operations. */
  fun parseArrayResult(result: Array<String>): DnsResult<List<String>> {
    if (result.isEmpty()) return DnsResult.Error("ENOTFOUND", "No results")
    val first = result[0]
    return when {
      first == "OK" -> DnsResult.Success(result.drop(1))
      ':' in first -> DnsResult.Error(first.substringBefore(':'), first.substringAfter(':'))
      else -> DnsResult.Error("EFORMERR", "Unexpected response format")
    }
  }

  /** Parse a string result from native DNS operations. */
  fun parseStringResult(result: String): DnsResult<String> {
    if (result.isEmpty()) return DnsResult.Error("ENOTFOUND", "No results")
    val idx = result.indexOf(':')
    if (idx == -1) return DnsResult.Error("EFORMERR", "Unexpected response format")
    val prefix = result.substring(0, idx)
    val data = result.substring(idx + 1)
    return if (prefix == "OK") DnsResult.Success(data) else DnsResult.Error(prefix, data)
  }

  /** Create a DNS error proxy object for callbacks. */
  fun dnsError(code: String, message: String): ProxyObject =
    ProxyObject.fromMap(mapOf("code" to code, "message" to message, "name" to "Error"))

  /** Resolve a hostname by DNS record type. */
  fun resolveByType(hostname: String, type: String): Array<String> = when (type) {
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
}

/** Map a successful DNS result to another type. */
internal fun <T> DnsResult<T>.map(transform: (T) -> Any): DnsResult<Any> = when (this) {
  is DnsResult.Success -> DnsResult.Success(transform(data))
  is DnsResult.Error -> this
}

/** Parsers for structured DNS record types. */
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
