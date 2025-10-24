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
package elide.runtime.intrinsics.server.http.v2.wsgi

import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.util.AsciiString
import elide.runtime.Logger
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.intrinsics.server.http.v2.HttpContentSink
import elide.runtime.intrinsics.server.http.v2.HttpContentSource
import elide.runtime.intrinsics.server.http.v2.HttpContext
import elide.runtime.intrinsics.server.http.v2.HttpSession
import elide.runtime.intrinsics.server.http.v2.wsgi.WsgiHttpContext.Companion.headerMultiValueString
import elide.runtime.intrinsics.server.http.v2.wsgi.WsgiHttpContext.Companion.headerVariableName

public class WsgiHttpContext internal constructor(
  override val request: HttpRequest,
  override val requestBody: HttpContentSource,
  override val response: HttpResponse,
  override val responseBody: HttpContentSink,
  override val session: HttpSession,
  public val input: WsgiInputStream,
) : HttpContext {
  override fun close() {
    input.dispose()
  }

  public companion object {
    public const val HEADER_VALUE_SEPARATOR: String = ";"

    public val excludedHeaderNames: Array<AsciiString> = arrayOf(
      HttpHeaderNames.AUTHORIZATION,
      HttpHeaderNames.CONTENT_LENGTH,
      HttpHeaderNames.CONTENT_TYPE,
      HttpHeaderNames.CONNECTION,
    ).sortedArray()

    public fun headerVariableName(name: String): String {
      return "HTTP_${name.uppercase().replace("-", "_")}"
    }

    public fun headerMultiValueString(values: Iterable<String>): String {
      return values.joinToString(HEADER_VALUE_SEPARATOR)
    }
  }
}

internal fun HttpContext.toWsgiEnviron(
  input: WsgiInputStream,
  errors: WsgiErrorStream,
  version: Pair<Int, Int>,
  multithread: Boolean,
  multiprocess: Boolean,
  runOnce: Boolean,
  urlScheme: String,
  defaultHost: String,
  defaultPort: Int,
  scriptName: String,
): MutableMap<String, Any> = mutableMapOf<String, Any>().apply {
  // base wsgi environ
  put("wsgi.input", input)
  put("wsgi.errors", errors)
  put("wsgi.version", listOf(version.first, version.second))
  put("wsgi.url_scheme", urlScheme)
  put("wsgi.multithread", multithread)
  put("wsgi.multiprocess", multiprocess)
  put("wsgi.run_once", runOnce)

  // CGI variables
  put("REQUEST_METHOD", request.method().name())
  put("SCRIPT_NAME", scriptName)
  put("PATH_INFO", request.uri().substringBefore('?'))
  put("QUERY_STRING", request.uri().substringAfter('?'))
  put("SERVER_NAME", defaultHost)
  put("SERVER_PORT", defaultPort.toString())
  put("SERVER_PROTOCOL", request.protocolVersion().text())
  put("CONTENT_TYPE", request.headers().get(HttpHeaderNames.CONTENT_TYPE).orEmpty())
  put("CONTENT_LENGTH", request.headers().get(HttpHeaderNames.CONTENT_LENGTH).orEmpty())

  // headers
  val requestHeaders = request.headers()
  for (name in requestHeaders.names()) {
    if (WsgiHttpContext.excludedHeaderNames.binarySearch(name) >= 0) continue
    put(headerVariableName(name), headerMultiValueString(requestHeaders.getAll(name)))
  }
}
