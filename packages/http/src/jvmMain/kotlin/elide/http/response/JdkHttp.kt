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

package elide.http.response

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpResponse
import java.util.Optional
import javax.net.ssl.SSLSession
import elide.http.Body
import elide.http.Headers
import elide.http.ProtocolVersion
import elide.http.Response
import elide.http.Status
import elide.http.StatusCode
import elide.http.body.PublisherBody
import elide.http.defaultJdkProtocol
import elide.http.headers.JavaNetHttpHeaders
import elide.http.toProtocolVersion

// Build a suite of JDK headers.
private fun Headers.toJdkHeaders(): HttpHeaders {
  val map: MutableMap<String, List<String>> = asOrdered().map {
    it.name.name to it.value.values.toList()
  }.toMap().toMutableMap()

  return HttpHeaders.of(map) { _, _ -> true }
}

// Internal utility for creating JDK HTTP responses.
internal object JdkHttp {
  /**
   * Wraps a pure Kotlin [Response.HttpResponse] in a compliant JDK HTTP response.
   *
   * Objects of this type can be created through the [JdkHttp] utility.
   */
  internal class JdkResponse<T> internal constructor (
    private val response: Response.HttpResponse,
    private val headers: HttpHeaders = response.headers.toJdkHeaders(),
    private val request: HttpRequest? = null,
    private val sslSession: Optional<SSLSession> = Optional.empty(),
    private val previousResponse: Optional<HttpResponse<T>> = Optional.empty(),
  ) : HttpResponse<T> {
    override fun uri(): URI? = request?.uri()
    override fun statusCode(): Int = response.status.code.symbol.toInt()
    override fun request(): HttpRequest? = request
    override fun sslSession(): Optional<SSLSession> = sslSession
    override fun headers(): HttpHeaders = headers
    @Suppress("UNCHECKED_CAST")
    override fun body(): T = response.body as T
    override fun previousResponse(): Optional<HttpResponse<T>> = previousResponse
    override fun version(): HttpClient.Version? = when (response.version) {
      ProtocolVersion.HTTP_2 -> HttpClient.Version.HTTP_2
      else -> HttpClient.Version.HTTP_1_1
    }
  }

  /**
   * Builder for a JDK HTTP response.
   */
  class JdkResponseBuilder internal constructor (
    var body: BodyPublisher? = null,
    var request: HttpRequest? = null,
    var version: HttpClient.Version = defaultJdkProtocol,
    var statusCode: Int,
    var headers: MutableMap<String, List<String>>,
    var sslSession: Optional<SSLSession> = Optional.empty(),
  ) {
    /**
     * Build this builder into a compliant JDK HTTP response.
     *
     * @return A JDK HTTP response.
     */
    fun <T> build(): JdkResponse<T> = JdkResponse<T>(
      request = request,
      sslSession = sslSession,
      previousResponse = Optional.empty(),
      response = Response.of(
        body = body?.let { PublisherBody(it) } ?: Body.Empty,
        version = version.toProtocolVersion(),
        status = Status.of(StatusCode.resolve(statusCode.toUShort())),
        headers = JavaNetHttpHeaders(HttpHeaders.of(headers) { _, _ -> true }),
      ),
    )
  }

  /**
   * Create a new (empty) JDK HTTP response builder; the default status is 200 OK.
   *
   * @return A new JDK HTTP response builder.
   */
  fun builder(): JdkResponseBuilder = JdkResponseBuilder(
    statusCode = 200,
    headers = mutableMapOf(),
  )

  /**
   * Create a new (empty) JDK HTTP response builder; the default status is 200 OK.
   *
   * @param status The status code to use for the response.
   * @return A new JDK HTTP response builder.
   */
  fun builder(status: StatusCode): JdkResponseBuilder = JdkResponseBuilder(
    statusCode = status.symbol.toInt(),
    headers = mutableMapOf(),
  )
}
