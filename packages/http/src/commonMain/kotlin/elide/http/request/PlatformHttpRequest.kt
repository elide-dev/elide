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

package elide.http.request

/**
 * ## Platform HTTP Request
 *
 * Specifies an extension point for platform-specific HTTP request types. The underlying request type can be obtained
 * via the [request] property.
 */
public expect sealed interface PlatformHttpRequest<T> : elide.http.Request.PlatformRequest {
  /**
   * The underlying HTTP request object.
   */
  public val request: T & Any
}
