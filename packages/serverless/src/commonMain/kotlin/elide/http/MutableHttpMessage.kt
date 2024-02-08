/*
 * Copyright (c) 2024 Elide Ventures, LLC.
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

package elide.http

import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import elide.http.api.HttpVersion
import elide.http.api.HttpVersion.HTTP_2
import elide.http.api.MutableHttpMessage as HttpMessageAPI

/**
 *
 */
public abstract class MutableHttpMessage(
  override var version: HttpVersion = HTTP_2,
  override var headers: HttpHeaders = HttpHeaders.create(),
) : HttpMessageAPI {
  private val payload: AtomicRef<HttpPayload> = atomic(HttpPayload.Empty)

  public var body: HttpPayload
    get() = payload.value
    set(value) { payload.value = value }
}