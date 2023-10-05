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

package elide.runtime.intrinsics.server.http.internal

import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpContext
import elide.runtime.intrinsics.server.http.HttpRequest
import elide.runtime.intrinsics.server.http.HttpResponse

/**
 * Represents the signature of a method used as request handler. The [GuestHandler] wrapper implements this signature
 * by executing a guest value.
 */
@DelicateElideApi internal fun interface GuestHandlerFunction {
  /**
   * Handle an incoming HTTP [request] by accessing the outgoing [response] and a [context] shared between all the
   * handlers in the pipeline. The return value indicates whether the next handler will be invoked.
   *
   * @param request The incoming [HttpRequest] being handled.
   * @param response The outgoing [HttpResponse] to be sent back to the client.
   * @param context A generic container used to pass data between handlers in the pipeline.
   * @return Whether the next handler in the pipeline (if any) will be invoked after this one.
   */
  operator fun invoke(request: HttpRequest, response: HttpResponse, context: HttpContext): Boolean
}

/** Internal alias used for a list of handler references. */
@DelicateElideApi internal typealias HandlerStack = MutableList<GuestHandler>
