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

package elide.ssr.type

import io.micronaut.http.HttpRequest
import java.security.Principal

/**
 * Request state container which is passed to methods which need access to request state.
 *
 * @param request HTTP request bound to this request state.
 * @param principal Security principal detected for this request, or `null`.
 * @param path Request path, made available to the VM. Unless specified, derived from [request].
 */
public data class RequestState(
  val request: HttpRequest<*>,
  val principal: Principal?,
  val path: String = request.path,
)
