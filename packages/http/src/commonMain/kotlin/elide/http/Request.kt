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
 * ## HTTP Request
 *
 * Core type for an HTTP request message; this type is the base of all pure Kotlin request types, as well as platform
 * specific type implementations via [PlatformRequest].
 *
 * HTTP requests carry a standardized set of properties and data, all of which are represented here. Implementations
 * typically tie these properties to the underlying equivalents for a given class.
 *
 * ### HTTP Request Properties
 *
 * - [method]: The HTTP method which is specified by this request; the [Method] class starts the type hierarchy there.
 * - [version]: Requests carry an HTTP version, which should match the corresponding response(s); see [ProtocolVersion].
 * - [url]: The URL to which this request is directed; this is a full URL, including scheme, host, port, and path.
 * - [headers]: The HTTP headers which are specified by this request; these are represented as a [Headers] object.
 * - [body]: The body of the request, if any; this is represented as a [Body] object. Note that this may be empty.
 *
 * HTTP requests always have a [type] of [Message.Type.REQUEST].
 *
 * ### Mutability
 *
 * HTTP requests are, by default, completely immutable; a request can typically be converted to a mutable form through
 * the [toMutable] function, which produces a [MutableRequest]. Platform-specific implementations may either perform
 * copy operations in this case, or provide `this` if the request is already mutable.
 *
 * ### Request Bodies
 *
 * HTTP message bodies may be large or need streaming functions. As a result, HTTP message bodies are typed in an open
 * hierarchy which platform-specific implementations can leverage.
 *
 * @see elide.http.Method HTTP methods
 * @see elide.http.ProtocolVersion HTTP protocol versions
 * @see elide.http.HttpUrl HTTP URLs
 * @see elide.http.Headers HTTP headers
 * @see elide.http.Body HTTP message bodies
 * @see elide.http.Message HTTP messages
 * @see elide.http.MutableRequest Mutable HTTP requests
 * @see elide.http.Response HTTP responses
 */
public sealed interface Request: Message, HttpPrintable {
  /**
   * ### HTTP method.
   *
   * The HTTP method which is specified by this request; the [Method] class represents the type hierarchy for all HTTP
   * method specification types. Methods are typically standardized as GET/POST/PUT/DELETE, but may also be custom verbs
   * or other HTTP methods defined by future specifications.
   */
  public val method: Method

  /**
   * ### HTTP protocol version.
   *
   * The HTTP protocol version which is specified by this request; the [ProtocolVersion] class represents the type
   * hierarchy for all known protocol versions. Effectively, a protocol version is a major and minor version number.
   */
  override val version: ProtocolVersion

  /**
   * ### HTTP URL.
   *
   * The URL which is under invocation as specified by this HTTP request; this URL type is expected to parse and express
   * all information contained in the URL, including the scheme, host, port, path, and query parameters.
   */
  public val url: HttpUrl

  /**
   * ### HTTP request headers.
   *
   * The HTTP request message headers, which are specified by this request; these headers are represented as a [Headers]
   * object and may contain both standard and custom headers. Headers can be repeated.
   */
  override val headers: Headers

  /**
   * ### HTTP request body.
   *
   * The HTTP request message body content which is affixed to this request, if any; when body content is unavailable, a
   * non-null object representing an empty body is provided.
   */
  public val body: Body

  // This class represents an HTTP request.
  override val type: Message.Type get() = Message.Type.REQUEST

  /**
   * Convert this request to a mutable form.
   *
   * This function produces a [MutableRequest] built from this object; if this object is already mutable, it may return
   * itself.
   *
   * @return A mutable request object, which may be this object if it is already mutable.
   */
  override fun toMutable(): MutableRequest

  /**
   * Platform request: Extension point for platform-specific request implementations.
   */
  public interface PlatformRequest: Request

  override val components: Sequence<HttpToken> get() = sequence {
    // HTTP/1.1 ...
    yield(version)
    // ...
    yield(HttpToken.Space)
    // HTTP/1.1 ... GET
    yield(method)
    // ...
    yield(HttpToken.Space)
    // HTTP/1.1 ... GET /path
    yield(HttpToken.of(url.path))
    // (newline)
    yield(HttpToken.Newline)
    // x-header: value
    yieldAll(headers.asSequence())
    // (double newline)
    yield(HttpToken.DoubleNewline)

    when (val data = body) {
      is Body.Empty -> yield(HttpToken.of("(No body)"))
      is Body.SizedBody -> yield(HttpToken.of("(Body of size ${data.contentLength})"))
      is Body.StreamedBody -> yield(HttpToken.of("(Body present)"))
    }
  }
}
