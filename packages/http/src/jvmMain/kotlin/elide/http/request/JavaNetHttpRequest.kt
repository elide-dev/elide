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

import java.net.http.HttpRequest
import elide.http.Body
import elide.http.Headers
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableRequest
import elide.http.ProtocolVersion
import elide.http.body.PublisherBody
import elide.http.headers.JavaNetHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP request type for `java.net.http`.
@JvmInline internal value class JavaNetHttpRequest(private val backing: HttpRequest): PlatformHttpRequest<HttpRequest> {
  override val request: HttpRequest get() = backing
  override val version: ProtocolVersion get() = backing.toProtocolVersion()
  override val method: Method get() = Method.of(backing.method())
  override val url: HttpUrl get() = JavaNetHttpUri(backing.uri())
  override val headers: Headers get() = JavaNetHttpHeaders(backing.headers())
  override fun toMutable(): MutableRequest = JavaNetMutableHttpRequest.of(backing)
  override val body: Body get() = backing.bodyPublisher().let { pub ->
    when {
      pub == null || pub.isEmpty || pub.get().contentLength() == 0L -> Body.Empty
      else -> PublisherBody(pub.get())
    }
  }
}
