/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

import kotlin.jvm.JvmInline
import elide.http.api.StandardHttpMethod
import elide.http.api.HttpMethod as HttpMethodAPI

/**
 *
 */
public sealed interface HttpMethod : HttpMethodAPI {
  /**
   * ## Standard HTTP method.
   */
  @JvmInline public value class Standard(public val method: StandardHttpMethod): HttpMethodAPI by method {
    override val name: String get() = method.name
  }

  /**
   * ## Custom HTTP method.
   */
  public data class Custom(
    override val name: String,
    override val body: Boolean = false,
    override val idempotent: Boolean = false,
    override val write: Boolean = false,
  ): HttpMethod
}
