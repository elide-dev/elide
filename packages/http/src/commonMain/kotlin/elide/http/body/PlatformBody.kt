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

package elide.http.body

/**
 * Platform-specific body interface for HTTP requests and responses.
 *
 * Implements body types which can be modeled by Java types. See [unwrap].
 */
public expect sealed interface PlatformBody<T>: elide.http.Body.PlatformBody {
  /**
   * Unwrap the body to its underlying type.
   *
   * @return The unwrapped body.
   */
  public fun unwrap(): T & Any
}
