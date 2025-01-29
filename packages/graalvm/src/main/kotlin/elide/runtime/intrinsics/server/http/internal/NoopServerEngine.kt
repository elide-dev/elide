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
package elide.runtime.intrinsics.server.http.internal

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.intrinsics.server.http.HttpRouter
import elide.runtime.intrinsics.server.http.HttpServerConfig
import elide.runtime.intrinsics.server.http.HttpServerEngine
import elide.vm.annotations.Polyglot

// Properties available to guest code on `NoopServerEngine`.
private val NOOP_ENGINE_PROPS_AND_METHODS = arrayOf(
  "config",
  "router",
  "running",
  "start",
)

/** A stub implementation that can be used to collect route handler references without starting a new server. */
@DelicateElideApi internal class NoopServerEngine(
  @Polyglot override val config: HttpServerConfig,
  @Polyglot override val router: HttpRouter,
) : HttpServerEngine, ProxyObject {
  @get:Polyglot override val running: Boolean = false

  @Polyglot override fun start() {
    // nothing to do here
  }

  override fun getMemberKeys(): Array<String> = NOOP_ENGINE_PROPS_AND_METHODS
  override fun hasMember(key: String?): Boolean = key != null && key in NOOP_ENGINE_PROPS_AND_METHODS

  override fun putMember(key: String?, value: Value?) {
    // no-op
  }

  override fun getMember(key: String?): Any? = when (key) {
    "config" -> config
    "router" -> router
    "running" -> running
    "start" -> ProxyExecutable { start() }
    else -> null
  }
}
