/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.http.api

/**
 * # HTTP: Method
 *
 * Describes the interface guaranteed by HTTP method description classes or objects; this includes standard HTTP methods
 * described within Elide, and interfaces which allow extension for custom methods.
 */
public interface HttpMethod {
  /**
   * Describes the name of the HTTP method; this should be the exact string value of the method, as it would appear in
   * an HTTP request.
   */
  public val name: HttpString

  /**
   * Whether this operation, by spec, is allowed to carry a body payload.
   */
  public val body: Boolean

  /**
   * Whether this operation is expected to modify the state of the server or its resources; if `true`, the operation is
   * considered a write operation, and is not expected to be idempotent by default.
   */
  public val write: Boolean

  /**
   * Whether this operation is expected to perform idempotent work; if `true`, the operation is considered safe for
   * certain use cases.
   */
  public val idempotent: Boolean
}
