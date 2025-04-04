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

import io.micronaut.http.HttpMethod
import elide.http.Method.PlatformMethod

// Implements native HTTP method support for Micronaut.
@JvmInline internal value class MicronautHttpMethod(private val http: HttpMethod): PlatformMethod {
  override val symbol: String get() = http.name
  override val permitsRequestBody: Boolean get() = http.permitsRequestBody()
  override val permitsResponseBody: Boolean get() = http.permitsResponseBody()
  override val requiresRequestBody: Boolean get() = http.requiresRequestBody()
}
