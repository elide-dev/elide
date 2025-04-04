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

import elide.core.api.Symbolic

/**
 * ## Standard HTTP Method
 *
 * Provides an enumeration of standard HTTP method verbs, and their use constraints/disposition.
 */
public expect enum class StandardMethod : Method.PlatformMethod, Symbolic<String> {
  GET,
  HEAD,
  POST,
  PUT,
  DELETE,
  OPTIONS,
  PATCH,
  TRACE,
  CONNECT;

  override val permitsRequestBody: Boolean
  override val permitsResponseBody: Boolean
  override val requiresRequestBody: Boolean
  override val symbol: String

  /** Factory and resolver for HTTP method values. */
  public companion object : Symbolic.SealedResolver<String, StandardMethod> {
    override fun resolve(symbol: String): StandardMethod
  }
}
