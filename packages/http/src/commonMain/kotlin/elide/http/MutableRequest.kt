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
 * ## HTTP Request (Mutable)
 *
 * Represents an HTTP request which is held in mutable form; for more information, see the main [Request] type and its
 * associated types.
 *
 * @see Request Immutable requests
 */
public sealed interface MutableRequest : MutableMessage, Request {
  /**
   * ### HTTP URL.
   *
   * The URL which is under invocation as specified by this HTTP request; this URL type is expected to parse and express
   * all information contained in the URL, including the scheme, host, port, path, and query parameters.
   *
   * This property is mutable.
   */
  override var url: HttpUrl

  /**
   * ### HTTP request headers.
   *
   * The HTTP request message headers, which are specified by this request; these headers are represented as a [Headers]
   * object and may contain both standard and custom headers. Headers can be repeated.
   *
   * This property is mutable via [MutableHeaders].
   */
  override val headers: MutableHeaders

  /**
   * ### HTTP request body.
   *
   * The HTTP request message body content which is affixed to this request, if any; when body content is unavailable, a
   * non-null object representing an empty body is provided.
   *
   * This property is mutable.
   */
  override var body: Body

  // Default implementation of `toMutable` which just returns self.
  override fun toMutable(): MutableRequest = this

  /**
   * Build this mutable request into an immutable form; if the request is already immutable, this is a no-op.
   *
   * @return An immutable request object.
   */
  override fun build(): Request

  /**
   * Platform request: Extension point for platform-specific mutable request implementations.
   */
  public interface PlatformMutableRequest: MutableRequest
}
