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
 * ## HTTP Response (Mutable)
 *
 * Represents an HTTP response which is held in mutable form; for more information, see the main [Request] type and its
 * associated types.
 *
 * @see Response Immutable responses
 */
public sealed interface MutableResponse: MutableMessage, Response {
  /**
   * ### HTTP response status.
   */
  override var status: Status

  /**
   * ### HTTP response headers.
   */
  override val headers: MutableHeaders

  /**
   * ### HTTP response body.
   */
  override var body: Body

  /**
   * ### HTTP response trailers.
   */
  override val trailers: MutableHeaders?

  /**
   * ### Pure HTTP Response (Mutable)
   *
   * Mutable HTTP response structure formed from compliant implementations of each HTTP response part; these responses
   * are used internally by Elide and also to model JDK HTTP responses.
   *
   * @property version HTTP protocol version of this response.
   * @property status HTTP response status of this response.
   * @property headers HTTP headers of this response.
   * @property trailers HTTP trailers of this response, if any.
   * @property body HTTP message body of this response.
   */
  public data class MutableHttpResponse internal constructor (
    override var version: ProtocolVersion,
    override var status: Status,
    override val headers: MutableHeaders,
    override val trailers: MutableHeaders?,
    override var body: Body,
  ) : MutableResponse {
    override fun toMutable(): MutableResponse = this

    override fun build(): Response = Response.of(
      version = version,
      status = status,
      headers = headers,
      trailers = trailers,
      body = body,
    )
  }

  // Default implementation of `toMutable` which just returns self.
  override fun toMutable(): MutableResponse = this

  /**
   * Build this mutable response into an immutable form; if the response is already immutable, this is a no-op.
   *
   * @return An immutable response object.
   */
  override fun build(): Response

  /**
   * Platform response: Extension point for platform-specific mutable HTTP response implementations.
   */
  public interface PlatformMutableResponse: MutableResponse
}
