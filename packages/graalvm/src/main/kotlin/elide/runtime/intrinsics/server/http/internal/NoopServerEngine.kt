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

import org.graalvm.polyglot.HostAccess.Export
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpRouter
import elide.runtime.intrinsics.server.http.HttpServerConfig
import elide.runtime.intrinsics.server.http.HttpServerEngine

/** A stub implementation that can be used to collect route handler references without starting a new server. */
@DelicateElideApi internal class NoopServerEngine(
  @Export override val config: HttpServerConfig,
  @Export override val router: HttpRouter,
) : HttpServerEngine {
  @get:Export override val running: Boolean = false

  @Export override fun start() {
    // nothing to do here
  }
}
