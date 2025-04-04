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

import elide.http.MutableResponse

/**
 * ## Platform HTTP Response (Mutable)
 *
 * Specifies an extension point for platform-specific mutable HTTP response types. The underlying request type can be
 * obtained via the [response] property.
 */
public expect sealed interface PlatformMutableHttpResponse<T> : MutableResponse.PlatformMutableResponse {
  /**
   * The underlying HTTP response object.
   */
  public val response: T & Any
}
