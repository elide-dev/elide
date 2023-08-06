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

package elide.runtime.gvm.internals.intrinsics.js.express

import io.netty.handler.codec.http.HttpRequest
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.gvm.internals.intrinsics.js.JsProxy

/** An intrinsic helper used to construct JavaScript request objects around a Netty [HttpRequest]. */
internal object ExpressRequestIntrinsic {
  fun from(request: HttpRequest): ProxyObject = JsProxy.build {
    // members extracted from the request by Reactor Netty
    put("path", request.uri())
    put("method", request.method().name())

    // request parameters are populated by the routing code
    putObject("params")
  }
}
