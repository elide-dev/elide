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

package elide.runtime.plugins.wasm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@Suppress("unused") @DelicateElideApi public class Wasm private constructor(
  private val config: WasmConfig,
  private val resources: LanguagePluginManifest,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }
  
  private fun configureContext(builder: PolyglotContextBuilder) {
    // nothing to configure
  }
  
  public companion object Plugin : AbstractLanguagePlugin<WasmConfig, Wasm>() {
    private const val WASM_LANGUAGE_ID = "wasm"
    private const val WASM_PLUGIN_ID = "WASM"
    
    override val languageId: String = WASM_LANGUAGE_ID
    override val key: Key<Wasm> = Key(WASM_PLUGIN_ID)
    
    override fun install(scope: InstallationScope, configuration: WasmConfig.() -> Unit): Wasm {
      // apply the configuration and create the plugin instance
      val config = WasmConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope, lenient = true)
      val instance = Wasm(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}
