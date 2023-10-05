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

package elide.runtime.plugins.llvm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@Suppress("unused") @DelicateElideApi public class LLVM private constructor(
  private val config: LLVMConfig,
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
  
  public companion object Plugin : AbstractLanguagePlugin<LLVMConfig, LLVM>() {
    private const val LLVM_LANGUAGE_ID = "llvm"
    private const val LLVM_PLUGIN_ID = "LLVM"
    
    override val languageId: String = LLVM_LANGUAGE_ID
    override val key: Key<LLVM> = Key(LLVM_PLUGIN_ID)
    
    override fun install(scope: InstallationScope, configuration: LLVMConfig.() -> Unit): LLVM {
      // apply the configuration and create the plugin instance
      val config = LLVMConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope, lenient = true)
      val instance = LLVM(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}
