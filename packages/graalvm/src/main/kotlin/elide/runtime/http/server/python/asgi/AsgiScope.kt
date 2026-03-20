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
package elide.runtime.http.server.python.asgi

import io.netty.handler.codec.http.HttpRequest
import org.graalvm.polyglot.proxy.ProxyHashMap
import elide.runtime.http.server.getHeader
import elide.runtime.http.server.hostnameOrDomainPath
import elide.runtime.http.server.netty.HttpApplicationStack.ServiceBinding
import elide.runtime.http.server.portOrNull

/**
 * Builds ASGI scope dictionaries from incoming Netty requests.
 *
 * An ASGI scope is the first argument passed to an ASGI application callable. It describes the incoming connection:
 * its type (`http`, `websocket`, `lifespan`), version, headers, path, query string, and server information.
 *
 * The scope dict follows the ASGI HTTP Connection Scope specification.
 *
 * @see <a href="https://asgi.readthedocs.io/en/latest/specs/www.html#http-connection-scope">ASGI HTTP Spec</a>
 */
public object AsgiScope {
  private const val ASGI_VERSION = "3.0"
  private const val ASGI_SPEC_VERSION = "2.4"

  private const val UNKNOWN_HOST = "localhost"
  private const val DEFAULT_PORT = 80
  private const val DEFAULT_TLS_PORT = 443

  /**
   * Build an ASGI HTTP connection scope from the incoming [request] and resolved [host] binding.
   *
   * The returned [ProxyHashMap] can be passed directly to the guest ASGI application as the `scope` argument.
   */
  @JvmStatic public fun httpScope(
    request: HttpRequest,
    host: ServiceBinding,
    entrypoint: AsgiEntrypoint,
  ): ProxyHashMap {
    val uri = request.uri()
    val queryIndex = uri.indexOf('?')
    val path = if (queryIndex >= 0) uri.substring(0, queryIndex) else uri
    val queryString = if (queryIndex >= 0) uri.substring(queryIndex + 1) else ""

    val hostname = host.address.hostnameOrDomainPath() ?: UNKNOWN_HOST
    val port = host.address.portOrNull() ?: if (host.scheme == "https") DEFAULT_TLS_PORT else DEFAULT_PORT

    val headers = buildHeaderList(request)

    return ProxyHashMap.from(mutableMapOf<Any, Any>(
      // ASGI metadata
      "type" to "http",
      "asgi" to ProxyHashMap.from(mutableMapOf<Any, Any>(
        "version" to ASGI_VERSION,
        "spec_version" to ASGI_SPEC_VERSION,
      )),

      // HTTP specifics
      "http_version" to request.protocolVersion().majorVersion().toString() + "." +
        request.protocolVersion().minorVersion().toString(),
      "method" to request.method().name(),
      "path" to path,
      "raw_path" to path.toByteArray(Charsets.UTF_8),
      "query_string" to queryString.toByteArray(Charsets.UTF_8),
      "root_path" to "",
      "scheme" to host.scheme,

      // Server and client info
      "server" to arrayOf(hostname, port),
      "headers" to headers,
    ))
  }

  /**
   * Build an ASGI lifespan scope used for application startup/shutdown events.
   */
  @JvmStatic public fun lifespanScope(): ProxyHashMap {
    return ProxyHashMap.from(mutableMapOf<Any, Any>(
      "type" to "lifespan",
      "asgi" to ProxyHashMap.from(mutableMapOf<Any, Any>(
        "version" to ASGI_VERSION,
        "spec_version" to ASGI_SPEC_VERSION,
      )),
    ))
  }

  /**
   * Build the ASGI headers list from an incoming Netty [request].
   *
   * ASGI headers are a list of two-element byte-string arrays: `[[name, value], ...]`.
   * Header names are lowercased per the ASGI spec.
   */
  private fun buildHeaderList(request: HttpRequest): Array<Array<ByteArray>> {
    val nettyHeaders = request.headers()
    val result = mutableListOf<Array<ByteArray>>()

    for (entry in nettyHeaders) {
      result.add(arrayOf(
        entry.key.lowercase().toByteArray(Charsets.UTF_8),
        entry.value.toByteArray(Charsets.UTF_8),
      ))
    }

    return result.toTypedArray()
  }
}
