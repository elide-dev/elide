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

package elide.runtime.http.server.js.worker

import elide.runtime.http.server.CallContext
import elide.vm.annotations.Polyglot

/** A request context providing general state data to guest handlers. */
public data class JsWorkerEnv(
  @Polyglot @JvmField val scheme: String = DEFAULT_SCHEME,
  @Polyglot @JvmField val host: String = DEFAULT_HOST,
  @Polyglot @JvmField val port: Int = DEFAULT_PORT,
) : CallContext {
  public companion object {
    public const val DEFAULT_SCHEME: String = "http"
    public const val DEFAULT_HOST: String = "localhost"
    public const val DEFAULT_PORT: Int = 8080
  }
}

