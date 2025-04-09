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
import elide.http.Http
import elide.http.HttpUrl
import elide.http.Method
import elide.http.MutableRequest
import elide.http.ProtocolVersion
import elide.http.body.PublisherBody
import elide.http.headers.JavaNetHttpHeaders
import elide.http.toProtocolVersion

// Implements a platform-specific HTTP request type for `java.net.http`.
@JvmInline internal value class JavaNetHttpRequest(
  private val pair: Pair<HttpRequest, Http.HttpRequestOptions?>,
): PlatformHttpRequest<HttpRequest> {
  constructor(req: HttpRequest): this(req to null)

  override val request: HttpRequest get() = pair.first
  override val version: ProtocolVersion get() = pair.first.toProtocolVersion()
  override val method: Method get() = Method.of(pair.first.method())
  override val url: HttpUrl get() = JavaNetHttpUri(pair.first.uri())
  override val headers: Headers get() = JavaNetHttpHeaders(pair.first.headers())
  override fun toMutable(): MutableRequest = JavaNetMutableHttpRequest.of(pair.first)
  override val body: Body get() = pair.first.bodyPublisher().let { pub ->
    when {
      pub == null || pub.isEmpty || pub.get().contentLength() == 0L -> Body.Empty
      else -> PublisherBody(pub.get())
    }
  }

  companion object {
    @JvmStatic fun from(req: HttpRequest, options: Http.HttpRequestOptions?): JavaNetHttpRequest =
      JavaNetHttpRequest(req to options)
  }
}
