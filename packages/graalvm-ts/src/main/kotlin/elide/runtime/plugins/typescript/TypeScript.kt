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
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@DelicateElideApi public class TypeScript(
  private val config: TypeScriptConfig,
  private val resources: LanguagePluginManifest,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }

  private fun configureContext(builder: PolyglotContextBuilder) {
    // nothing yet
  }

  public companion object Plugin : AbstractLanguagePlugin<TypeScriptConfig, TypeScript>() {
    private const val PYTHON_LANGUAGE_ID = "typescript"
    private const val PYTHON_PLUGIN_ID = "TypeScript"
    override val languageId: String = PYTHON_LANGUAGE_ID
    override val key: Key<TypeScript> = Key(PYTHON_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: TypeScriptConfig.() -> Unit): TypeScript {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = TypeScriptConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)
      val instance = TypeScript(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}
