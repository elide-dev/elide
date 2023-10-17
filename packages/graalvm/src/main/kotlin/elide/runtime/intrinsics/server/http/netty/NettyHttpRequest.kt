/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.intrinsics.server.http.netty

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpMethod
import elide.runtime.intrinsics.server.http.HttpRequest
import io.netty.handler.codec.http.HttpRequest as NettyRequest

/** [HttpRequest] implementation wrapping a Netty request object. */
@DelicateElideApi internal class NettyHttpRequest(private val request: NettyRequest) : HttpRequest {
  @get:Export override val uri: String get() = request.uri()
  @get:Export override val method: HttpMethod get() = HttpMethod.valueOf(request.method().name())
  @get:Export override val params: MutableMap<String, Any> = mutableMapOf()
}
