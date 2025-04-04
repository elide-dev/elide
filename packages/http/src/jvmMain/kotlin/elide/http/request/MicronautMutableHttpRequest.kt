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
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableHeaders
import elide.http.ProtocolVersion
import elide.http.Request
import elide.http.body.MicronautBody
import elide.http.headers.MicronautMutableHttpHeaders
import elide.http.micronaut.MicronautMutableHttpMessage
import elide.http.toProtocolVersion

// Implements a `PlatformHttpRequest` backed by a mutable Micronaut HTTP request.
@JvmInline internal value class MicronautMutableHttpRequest<T> (
  private val backing: MutableHttpRequest<T>
) : MicronautMutableHttpMessage, PlatformMutableHttpRequest<HttpRequest<T>> {
  override val request: HttpRequest<T> get() = backing
  override val httpVersion: HttpVersion get() = backing.httpVersion
  override val version: ProtocolVersion get() = backing.toProtocolVersion()
  override val method: Method get() = MicronautHttpMethod(backing.method)
  override val headers: MutableHeaders get() = MicronautMutableHttpHeaders(backing.headers)
  override fun build(): Request = MicronautHttpRequest(backing)

  override var url: HttpUrl
    get() = JavaNetHttpUri(backing.uri)
    set(value) {
      when (value) {
        is JavaNetHttpUri -> backing.uri(value.value)
        else -> error("Unsupported URI type: \"$value\" (${value::class.java.name})")
      }
    }

  override var body: Body
    get() = when (backing.body.isPresent) {
      false -> Body.Empty
      true -> MicronautBody.of(backing)
    }
    set(value) {
      when (value) {
        is Body.Empty -> backing.body(null)
        is MicronautBody<*> -> backing.body(value.unwrap())
        else -> error("Unsupported body type (${value::class.java.name})")
      }
    }
}
