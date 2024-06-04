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

package elide.runtime.plugins.typescript

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@DelicateElideApi public class TypeScript(
  @Suppress("unused") private val config: TypeScriptConfig,
  @Suppress("unused") private val resources: LanguagePluginManifest,
) {
  @Suppress("unused", "unused_parameter")
  private fun configureContext(builder: PolyglotContextBuilder) {
    // nothing at this time
  }

  public companion object Plugin : AbstractLanguagePlugin<TypeScriptConfig, TypeScript>() {
    private const val TS_LANGUAGE_ID = "ts"
    private const val TS_PLUGIN_ID = "TypeScript"
    override val languageId: String = TS_LANGUAGE_ID
    override val key: Key<TypeScript> = Key(TS_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: TypeScriptConfig.() -> Unit): TypeScript {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      return TypeScriptConfig().apply(configuration).let { config ->
        configureSharedBindings(scope, config)
        TypeScript(config, resolveEmbeddedManifest(scope)).apply {
          // subscribe to lifecycle events
          scope.lifecycle.on(ContextCreated, this::configureContext)
        }
      }
    }
  }
}
