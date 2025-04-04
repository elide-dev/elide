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

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpVersion
import elide.http.Body
import elide.http.Headers
import elide.http.MutableResponse
import elide.http.ProtocolVersion
import elide.http.Status
import elide.http.body.MicronautBody
import elide.http.headers.MicronautHttpHeaders
import elide.http.micronaut.MicronautHttpMessage
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP response type for Micronaut.
@JvmInline internal value class MicronautHttpResponse<T> private constructor (
  private val backing: HttpResponse<T>,
) : MicronautHttpMessage, PlatformHttpResponse<HttpResponse<T>> {
  override val response: HttpResponse<T> get() = backing
  override val httpVersion: HttpVersion get() = HttpVersion.HTTP_1_1  // @TODO default value
  override val version: ProtocolVersion get() = httpVersion.toProtocolVersion()
  override val status: Status get() = MicronautHttpStatus(backing.status)
  override val headers: Headers get() = MicronautHttpHeaders(backing.headers)
  override val trailers: Headers? get() = null // @TODO implement trailers
  override fun toMutable(): MutableResponse = MicronautMutableHttpResponse(backing.toMutableResponse())
  override val body: Body get() = when (backing.body.isPresent) {
    false -> Body.Empty
    else -> MicronautBody.of(backing)
  }

  /** Factories for obtaining a [MicronautHttpResponse]. */
  companion object {
    /** @return Wrapped Micronaut [response] type. */
    @JvmStatic fun <T> of(response: HttpResponse<T>): MicronautHttpResponse<T> = MicronautHttpResponse(response)
  }
}
