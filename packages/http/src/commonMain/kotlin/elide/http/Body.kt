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
 * ## HTTP Body
 *
 * Represents the concept of an HTTP request or response message body, whether present or not; when present, body
 * content always has a content length, and may also specify a content type. Body content is not always synchronous: it
 * may be provided in chunks, or may be voluminous enough to require streaming.
 *
 * As a consequence of these constraints, this body type is fully generic, and "handling" of the body type is left to
 * the calling client and implementation-specific patterns. Like other types for HTTP, this type is sealed into a type
 * hierarchy that can be used to match against specific implementation types.
 *
 * ### Empty Bodies
 *
 * The special object [Body.Empty] can be used in place of `null` or other conventions when specifying an empty body for
 * an HTTP message.
 *
 * ### Sized Body Data
 *
 * Most body implementations extend the [SizedBody] interface, which provides a [SizedBody.contentLength] property. This
 * property must always be consistent with the data emitted over the wire.
 *
 * @see SizedBody Sized body data
 * @see Empty Empty body data
 * @property isPresent Whether body content is present
 */
public sealed interface Body {
  /** Whether body content is present. */
  public val isPresent: Boolean get() = when (this) {
    is Empty -> false
    is PlatformBody -> contentLength > 0uL
  }

  /**
   * ### Empty Body
   *
   * An empty body, which is used to indicate that a body is not present or not available for a given request or
   * response.
   */
  public data object Empty: Body {
    override fun toString(): String = "Body.Empty"
  }

  /**
   * ### Streamed Body
   *
   * Represents a body that has no known content length.
   */
  public sealed interface StreamedBody: Body

  /**
   * ### Sized Body
   *
   * Represents a body that has a known content length; all platform-specific body implementations must provide this
   * value. When this value is `0`, the body is empty.
   */
  public sealed interface SizedBody: Body {
    /**
     * #### Content Length
     *
     * The content length of the body, in bytes. This value is always present when the body is present, and may be
     * representative of this chunk size.
     */
    public val contentLength: ContentLengthValue
  }

  /**
   * ### Platform Body
   *
   * A platform-specific body implementation, which may or may not have present data; this type is implemented in each
   * code unit via Kotlin's expect-actual infrastructure.
   */
  public interface PlatformBody: SizedBody
}
