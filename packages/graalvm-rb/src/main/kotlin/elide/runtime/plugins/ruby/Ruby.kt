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

package elide.runtime.plugins.ruby

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.getOrInstall
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest
import elide.runtime.plugins.llvm.LLVM

@DelicateElideApi public class Ruby(
  private val config: RubyConfig,
  private val resources: LanguagePluginManifest,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }
  
  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.enableOptions(
      "ruby.embedded",
      "ruby.no-home-provided",
      "ruby.platform-native-interrupt",
      "ruby.platform-native",
      "ruby.polyglot-stdio",
      "ruby.rubygems",
      "ruby.lazy-default",
      "ruby.lazy-builtins",
      "ruby.lazy-calltargets",
      "ruby.lazy-rubygems",
      "ruby.lazy-translation-core",
      "ruby.lazy-translation-user",
      "ruby.shared-objects",
      "ruby.experimental-engine-caching",
    )
    
    builder.disableOptions(
      "ruby.virtual-thread-fibers",
      "ruby.cexts",
    )

    builder.option("log.level", "OFF")
  }
  
  public companion object Plugin : AbstractLanguagePlugin<RubyConfig, Ruby>() {
    private const val RUBY_LANGUAGE_ID = "ruby"
    private const val RUBY_PLUGIN_ID = "Ruby"

    override val languageId: String = RUBY_LANGUAGE_ID
    override val key: Key<Ruby> = Key(RUBY_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: RubyConfig.() -> Unit): Ruby {
      configureLanguageSupport(scope)

      // apply the llvm plugin first
      scope.configuration.getOrInstall(LLVM)

      // apply the configuration and create the plugin instance
      val config = RubyConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)
      val instance = Ruby(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}
