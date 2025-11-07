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
package elide.runtime.http.server.python.flask

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import elide.runtime.http.server.CallContext

public class FlaskContext(request: HttpRequest) : CallContext {
  internal val queryParams: Map<String, String?> by lazy {
    QueryStringDecoder.builder().build(request.uri())
      .parameters()
      .mapValues { it.value.firstOrNull() }
  }
}
