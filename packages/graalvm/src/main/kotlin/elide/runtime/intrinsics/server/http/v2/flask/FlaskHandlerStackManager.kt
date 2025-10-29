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
package elide.runtime.intrinsics.server.http.v2.flask

import org.graalvm.polyglot.Source
import elide.runtime.core.EntrypointRegistry
import elide.runtime.core.SharedContextFactory
import elide.runtime.intrinsics.server.http.v2.GuestHandlerStackManager

public class FlaskHandlerStackManager(
  override val entrypointProvider: EntrypointRegistry,
  private val contextProvider: SharedContextFactory,
) : GuestHandlerStackManager<FlaskRouter>() {
  override fun initializeStack(stack: FlaskRouter, entrypoint: Source) {
    contextProvider.acquire()?.eval(entrypoint) ?: error("No context provider available")
  }

  override fun newStack(): FlaskRouter {
    return FlaskRouter()
  }
}
