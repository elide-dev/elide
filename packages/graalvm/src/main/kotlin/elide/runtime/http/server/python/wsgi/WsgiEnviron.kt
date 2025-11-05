/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.python.wsgi

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.util.AsciiString
import org.graalvm.polyglot.proxy.ProxyHashMap
import elide.runtime.Logging
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.getHeader
import elide.runtime.http.server.hostnameOrDomainPath
import elide.runtime.http.server.netty.HttpApplicationStack.ServiceBinding
import elide.runtime.http.server.portOrNull

/**
 * A WSGI environ container, holding a [map] that can be passed to guest code, and the [input] and [error] streams
 * included in the map. Use [WsgiEnviron.from] to create an instance for an incoming call.
 */
public class WsgiEnviron(
  public val input: WsgiInputStream,
  public val errors: WsgiErrorStream,
  public val map: ProxyHashMap,
) : CallContext {
  override fun callEnded(failure: Throwable?) {
    // cleanup buffers
    input.dispose()
    errors.dispose()
  }

  public companion object {
    private const val HEADER_VALUE_SEPARATOR: String = ";"

    private const val MULTITHREAD = false
    private const val MULTIPROCESS = false
    private const val RUN_ONCE = false

    private const val UNKNOWN_HOST: String = "localhost"
    private const val UNKNOWN_PORT: Int = 0

    private val WSGI_VERSION: Array<Int> = arrayOf(1, 0)

    private val excludedHeaderNames: Array<AsciiString> = arrayOf(
      HttpHeaderNames.AUTHORIZATION,
      HttpHeaderNames.CONTENT_LENGTH,
      HttpHeaderNames.CONTENT_TYPE,
      HttpHeaderNames.CONNECTION,
    ).sortedArray()

    /** Logger used for the error stream of the WSGI environ. */
    private val log = Logging.of(WsgiEnviron::class)

    private fun headerVariableName(name: String): String {
      return "HTTP_${name.uppercase().replace("-", "_")}"
    }

    private fun headerMultiValueString(values: Iterable<String>): String {
      return values.joinToString(HEADER_VALUE_SEPARATOR)
    }

    /**
     * Prepare a [WsgiEnviron] for an incoming [request]; the [entrypoint] is used to resolve the script name field,
     * and the [host] information is used to populate the default scheme, host, and port fields.
     *
     * Once the environ is created, the [input] stream must be attached to a
     * [stream][elide.runtime.http.server.ReadableContentStream] before it can be used.
     */
    @JvmStatic public fun from(
      request: HttpRequest,
      executor: ContextAwareExecutor,
      entrypoint: WsgiEntrypoint,
      host: ServiceBinding,
    ): WsgiEnviron {
      val contentLength = request.getHeader(HttpHeaderNames.CONTENT_LENGTH)?.toLongOrNull() ?: 0
      val inputStream = WsgiInputStream(executor, contentLength)
      val errorStream = WsgiErrorStream(log)

      val map = mutableMapOf<Any, Any>().apply {
        // base wsgi environ
        put("wsgi.input", inputStream)
        put("wsgi.errors", errorStream)
        put("wsgi.url_scheme", host.scheme)
        put("wsgi.version", WSGI_VERSION)
        put("wsgi.multithread", MULTITHREAD)
        put("wsgi.multiprocess", MULTIPROCESS)
        put("wsgi.run_once", RUN_ONCE)

        // CGI variables
        put("SCRIPT_NAME", entrypoint.source.name)
        put("SERVER_NAME", host.address.hostnameOrDomainPath() ?: UNKNOWN_HOST)
        put("SERVER_PORT", (host.address.portOrNull() ?: UNKNOWN_PORT).toString())
        put("REQUEST_METHOD", request.method().name())
        put("PATH_INFO", request.uri().substringBefore('?'))
        put("QUERY_STRING", request.uri().substringAfter('?'))
        put("SERVER_PROTOCOL", request.protocolVersion().text())
        put("CONTENT_TYPE", request.headers().get(HttpHeaderNames.CONTENT_TYPE).orEmpty())
        put("CONTENT_LENGTH", contentLength)

        // headers
        val requestHeaders = request.headers()
        for (name in requestHeaders.names()) {
          if (excludedHeaderNames.binarySearch(name) >= 0) continue
          put(headerVariableName(name), headerMultiValueString(requestHeaders.getAll(name)))
        }
      }.let(ProxyHashMap::from)

      return WsgiEnviron(inputStream, errorStream, map)
    }
  }
}
