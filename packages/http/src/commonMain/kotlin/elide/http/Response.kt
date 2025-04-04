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

package elide.http

/**
 * ## HTTP Response
 *
 * Core type for an HTTP response message; this is the base type for all pure Kotlin response types, as well as platform
 * specific implementations (via [PlatformResponse]).
 *
 * HTTP responses are standardized and carry a suite of properties to match, as well as other properties which make
 * sense in a server-side context. Like requests, HTTP responses are [Message] instances and implement [HttpPrintable].
 *
 * ### HTTP Response Properties
 *
 * - [version]: The HTTP protocol version of this response.
 * - [status]: The response status code and message.
 * - [headers]: The HTTP headers of this response.
 * - [body]: The HTTP message body of this response.
 * - [trailers]: The HTTP trailers of this response, if any.
 *
 * ### Mutability
 *
 * HTTP responses are, by default, completely immutable; a response can typically be converted to a mutable form through
 * the [toMutable] function, which produces a [MutableResponse]. Platform-specific implementations may either perform
 * copy operations in this case, or provide `this` if the response object is already mutable.
 *
 * ### Response Bodies
 *
 * HTTP message bodies may be large or need streaming functions. As a result, HTTP message bodies are typed in an open
 * hierarchy which platform-specific implementations can leverage.
 *
 * @see ProtocolVersion HTTP protocol versions
 * @see Status HTTP statuses
 * @see Headers HTTP headers
 * @see Body HTTP message bodies
 * @see Message HTTP messages
 * @see MutableResponse Mutable HTTP responses
 * @see Request HTTP requests
 */
public sealed interface Response: Message {
  /**
   * ### HTTP protocol version.
   *
   * The HTTP protocol version which is specified by this request; the [ProtocolVersion] class represents the type
   * hierarchy for all known protocol versions. Effectively, a protocol version is a major and minor version number.
   */
  override val version: ProtocolVersion

  /**
   * ### HTTP response status.
   *
   * The terminal status indicated by this response; this consists of a status code (a non-zero number) and an optional
   * status reason (a short string). Statuses (in standard form) typically fall into one of five categories:
   *
   * - `1xx`: Informational
   * - `2xx`: Success
   * - `3xx`: Redirection
   * - `4xx`: Client-side errors
   * - `5xx`: Server-side errors
   *
   * The disposition and other metadata for a response code is included automatically for standard response statuses.
   * Custom response statuses can specify their own (required) numeric status code and optional reason string.
   */
  public val status: Status

  /**
   * ### HTTP response headers.
   *
   * HTTP headers which are affixed to this response, and which are sent to the client before the response body, if any.
   * Some headers are managed by the runtime (for example, `Content-Length`) and cannot be modified by hand. Most other
   * headers (and custom headers) are under the full control of the developer.
   */
  override val headers: Headers

  /**
   * ### HTTP response body.
   *
   * Response body which is carried with this response, if any. If no response body is present, a non-null singleton
   * instance of [Body.Empty] is returned. Body implementations may be platform-specific.
   */
  public val body: Body

  /**
   * ### HTTP response trailers.
   *
   * HTTP trailers which are affixed to the end of this response, if any. Trailers are similar to headers, but are sent
   * after the response body. Trailers are only supported in HTTP/2 and later.
   */
  public val trailers: Headers?

  // This class represents an HTTP request.
  override val type: Message.Type get() = Message.Type.RESPONSE

  /**
   * ### Pure HTTP Response
   *
   * Immutable HTTP response structure formed from compliant implementations of each HTTP response part; these responses
   * are used internally by Elide and also to model JDK HTTP responses.
   *
   * @property version HTTP protocol version of this response.
   * @property status HTTP response status of this response.
   * @property headers HTTP headers of this response.
   * @property trailers HTTP trailers of this response, if any.
   * @property body HTTP message body of this response.
   */
  @JvmRecord public data class HttpResponse internal constructor (
    override val version: ProtocolVersion,
    override val status: Status,
    override val headers: Headers,
    override val trailers: Headers?,
    override val body: Body,
  ) : Response {
    override fun toMutable(): MutableResponse = MutableResponse.MutableHttpResponse(
      version = version,
      status = status,
      headers = headers.toMutable(),
      trailers = trailers?.toMutable(),
      body = body,
    )
  }

  /**
   * Convert this response to a mutable form.
   *
   * This function produces a [MutableResponse] built from this object; if this object is already mutable, it may return
   * itself.
   *
   * @return A mutable response object, which may be this object if it is already mutable.
   */
  override fun toMutable(): MutableResponse

  /**
   * Platform response: Extension point for platform-specific HTTP response implementations.
   */
  public interface PlatformResponse: Response

  override val components: Sequence<HttpToken> get() = sequence {
    // HTTP/1.1
    yield(version)
    // ...
    yield(HttpToken.Space)
    // HTTP/1.1 ... 200 OK
    yield(status)
    // (newline)
    yield(HttpToken.Newline)
    // x-response-header: value
    yieldAll(headers.asSequence())
    // (double newline)
    yield(HttpToken.DoubleNewline)

    when (val data = body) {
      is Body.Empty -> yield(HttpToken.of("(No body)"))
      is Body.SizedBody -> yield(HttpToken.of("(Body of size ${data.contentLength})"))
    }
  }

  /** Factories for obtaining a [Response]. */
  public companion object {
    /**
     * Create an HTTP response object from scratch.
     *
     * @param version HTTP protocol version of this response.
     * @param status HTTP response status of this response.
     * @param headers HTTP headers of this response.
     * @param trailers HTTP trailers of this response, if any.
     * @param body HTTP message body of this response.
     * @return A new HTTP response object.
     */
    @JvmStatic public fun of(
      version: ProtocolVersion,
      status: Status,
      headers: Headers,
      trailers: Headers? = null,
      body: Body = Body.Empty,
    ): HttpResponse = HttpResponse(
      version,
      status,
      headers,
      trailers,
      body,
    )
  }
}
