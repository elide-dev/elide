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

package elide.runtime.gvm.internals.js

import io.micronaut.http.HttpRequest
import java.io.InputStream
import java.net.URI
import elide.runtime.gvm.MicronautRequestExecutionInputs
import elide.runtime.intrinsics.js.FetchRequest

/** Implementation of JS execution inputs (a [FetchRequest]), based on a Micronaut [HttpRequest]. */
internal class JsMicronautRequestExecutionInputs private constructor (
  private val request: HttpRequest<Any>,
  state: Any?,
) :
  FetchRequest,
  MicronautRequestExecutionInputs<Any>,
  JsServerRequestExecutionInputs<HttpRequest<Any>>(state) {
  /** @inheritDoc */
  override fun getURL(): URI = request.uri

  /** @inheritDoc */
  override fun request(): HttpRequest<Any> = request

  /** @inheritDoc */
  override fun hasBody(): Boolean = request.body.isPresent

  /** @inheritDoc */
  override fun requestBody(): InputStream = request.getBody(InputStream::class.java).get()

  /** @inheritDoc */
  override fun requestHeaders(): Map<String, List<String>> = request.headers.asMap()

  /** Factory for creating inputs of this type. */
  internal companion object {
    /** @return Micronaut-backed JavaScript request execution inputs. */
    @JvmStatic fun of(request: HttpRequest<Any>, state: Any? = null) = JsMicronautRequestExecutionInputs(
      request,
      state,
    )
  }
}
