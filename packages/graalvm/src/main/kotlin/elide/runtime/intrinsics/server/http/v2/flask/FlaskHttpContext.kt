/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.intrinsics.server.http.v2.flask

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import elide.runtime.gvm.internals.intrinsics.js.struct.map.JsMultiMap
import elide.runtime.intrinsics.server.http.v2.HttpContentSink
import elide.runtime.intrinsics.server.http.v2.HttpContentSource
import elide.runtime.intrinsics.server.http.v2.HttpContext
import elide.runtime.intrinsics.server.http.v2.HttpSession
import elide.vm.annotations.Polyglot

public class FlaskHttpContext(
  override val request: HttpRequest,
  override val requestBody: HttpContentSource,
  override val response: HttpResponse,
  override val responseBody: HttpContentSink,
  override val session: HttpSession
) : HttpContext {
  internal val queryParams: Map<String, String?> by lazy {
    // TODO(@darvld): replace with a proper multi-map
    QueryStringDecoder.builder().build(request.uri())
      .parameters()
      .mapValues { it.value.firstOrNull() }
  }

  @Polyglot public fun status(code: Int) {
    response.status = HttpResponseStatus.valueOf(code)
  }

  override fun close() {
    // noop
  }
}
