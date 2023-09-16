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

package elide.runtime.gvm.internals.jvm

import io.micronaut.http.HttpRequest
import elide.runtime.gvm.MicronautRequestExecutionInputs
import elide.runtime.intrinsics.js.FetchHeaders
import elide.runtime.intrinsics.js.FetchRequest
import elide.runtime.intrinsics.js.ReadableStream

/**
 * TBD.
 */
internal class JvmMicronautRequestExecutionInputs private constructor (
  private val request: HttpRequest<Any>,
  state: Any?,
) : FetchRequest, MicronautRequestExecutionInputs<Any>, JvmServerRequestExecutionInputs<HttpRequest<Any>>(state) {
  override fun request(): HttpRequest<Any> {
    TODO("Not yet implemented")
  }

  override val body: ReadableStream
    get() = TODO("Not yet implemented")
  override val bodyUsed: Boolean
    get() = TODO("Not yet implemented")
  override val destination: String
    get() = TODO("Not yet implemented")
  override val headers: FetchHeaders
    get() = TODO("Not yet implemented")
  override val url: String
    get() = TODO("Not yet implemented")
}
