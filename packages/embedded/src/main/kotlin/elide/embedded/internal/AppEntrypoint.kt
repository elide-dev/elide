/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.embedded.internal

import elide.embedded.http.EmbeddedRequest
import elide.embedded.http.EmbeddedResponse
import elide.runtime.core.PolyglotValue

/**
 * Represents an abstract guest entrypoint resolved from an evaluated source, usually an executable [PolyglotValue].
 * Implementations of this interface provide the specific methods to interact with the entrypoint.
 */
internal sealed interface AppEntrypoint

/**
 * A [fetch-style][FETCH] entrypoint, using an executable [PolyglotValue] (the 'fetch' function) exported from the
 * entrypoint module. The function must accept a request object as an argument and return a response.
 *
 * Basic validation is performed on the wrapped [value] to ensure it is executable, no further validation is
 * possible since the signature of the function is not available to the host.
 */
@JvmInline internal value class FetchEntrypoint(val value: PolyglotValue) : AppEntrypoint {
  init {
    // basic validation, the signature is opaque
    require(value.canExecute()) { "Entrypoint must be executable" }
  }

  operator fun invoke(request: EmbeddedRequest): EmbeddedResponse {
    val result = value.execute(request)

    return runCatching { result.asHostObject<EmbeddedResponse>() }.getOrElse {
      if (it !is UnsupportedOperationException) throw it
      error("Unexpected response type returned by guest entrypoint, expected <EmbeddedResponse>, found $result")
    }
  }

  internal companion object {
    fun resolve(module: PolyglotValue): FetchEntrypoint {
      check(module.hasMember("fetch")) { "No 'fetch' function exported from entrypoint module" }
      return FetchEntrypoint(module.getMember("fetch"))
    }
  }
}
