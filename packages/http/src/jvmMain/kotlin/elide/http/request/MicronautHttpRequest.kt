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

import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpVersion
import io.micronaut.http.MutableHttpRequest
import elide.http.Body
import elide.http.Headers
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableRequest
import elide.http.ProtocolVersion
import elide.http.body.MicronautBody
import elide.http.headers.MicronautHttpHeaders
import elide.http.micronaut.MicronautHttpMessage
import elide.http.toProtocolVersion

// Implements a `PlatformHttpRequest` backed by a Micronaut HTTP request.
@JvmInline internal value class MicronautHttpRequest<T>(private val backing: HttpRequest<T>)
  : MicronautHttpMessage, PlatformHttpRequest<HttpRequest<T>> {
  override val request: HttpRequest<T> get() = backing
  override val method: Method get() = MicronautHttpMethod(backing.method)
  override val url: HttpUrl get() = JavaNetHttpUri(backing.uri)
  override val headers: Headers get() = MicronautHttpHeaders(backing.headers)
  override val httpVersion: HttpVersion get() = backing.httpVersion
  override val version: ProtocolVersion get() = backing.toProtocolVersion()

  override val body: Body get() = when (backing.body.isPresent) {
    false -> Body.Empty
    true -> MicronautBody.of(backing)
  }

  override fun toMutable(): MutableRequest = when (backing) {
    is MutableHttpRequest<*> -> MicronautMutableHttpRequest(backing)
    else -> MicronautMutableHttpRequest(backing.toMutableRequest())
  }
}
