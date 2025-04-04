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
 * ## HTTP Message
 *
 * Generic base interface for both types of HTTP messages: requests and responses. This interface is used to define
 * common properties like the HTTP version in use.
 */
public sealed interface Message: HttpPrintable {
  /**
   * ### HTTP protocol version.
   *
   * The HTTP protocol version which is specified by this request; the [ProtocolVersion] class represents the type
   * hierarchy for all known protocol versions. Effectively, a protocol version is a major and minor version number.
   */
  public val version: ProtocolVersion

  /**
   * ### HTTP message type.
   *
   * Indicates the type of HTTP message this object represents. This is either a request or a response.
   */
  public val type: Type

  /**
   * ### HTTP headers.
   *
   * Provides access to the headers associated with this HTTP message. When this object is an HTTP request, these are
   * request headers, and when this object is a response, these are response headers.
   */
  public val headers: Headers

  /**
   * ### HTTP message components.
   *
   * Produce a sequence of [HttpToken] values which constitute the parts of this HTTP message; when combined (joined
   * without a separator) these components should effectively render to a well-formed HTTP request or response message.
   *
   * To render a message, see `renderToHttp`.
   */
  public val components: Sequence<HttpToken>

  /**
   * ### HTTP Message Type
   *
   * Specifies the type of HTTP message this object represents.
   */
  public enum class Type {
    /**
     * The message is an HTTP request.
     */
    REQUEST,

    /**
     * The message is an HTTP response.
     */
    RESPONSE,
  }

  /**
   * Convert this message to a mutable form.
   *
   * This function produces a [MutableMessage] built from this object; if this object is already mutable, it may return
   * itself.
   *
   * @return A mutable message object, which may be this object if it is already mutable.
   */
  public fun toMutable(): MutableMessage

  /**
   * Platform extension point for HTTP message common logic.
   */
  public interface PlatformMessage: Message

  override fun renderToHttp(): StringBuilder = StringBuilder().apply {
    components.forEach {
      append(it.asString())
    }
  }
}
