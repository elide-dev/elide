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
package elide.runtime.plugins.bindings

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key

/**
 * Engine plugin providing support for shared language bindings.
 *
 * Common intrinsics are dynamically resolved by the plugin and can be used by language plugins during configuration.
 * Note that applying the bindings is an opt-in feature for language plugins, and as such certain implementations may
 * choose to ignore it.
 */
@DelicateElideApi public class Bindings private constructor(
  private val resolvedBindings: List<BindingsInstaller>,
) {
  /** Apply the shared bindings using a [registrar], e.g. the bindings container of a language plugin. */
  public fun applyTo(registrar: BindingsRegistrar, scope: InstallationScope) {
    return resolvedBindings.forEach { it.install(registrar, scope) }
  }

  /** Engine plugin providing support for shared language bindings. */
  public companion object Plugin : EnginePlugin<BindingsConfig, Bindings> {
    private const val BINDINGS_PLUGIN_ID = "bindings"

    override val key: Key<Bindings> = Key(BINDINGS_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: BindingsConfig.() -> Unit): Bindings {
      val config = BindingsConfig().apply(configuration)

      // resolve and collect bindings
      val bindings = config.resolver.resolveBindings()
      return Bindings(bindings.toList())
    }
  }
}
