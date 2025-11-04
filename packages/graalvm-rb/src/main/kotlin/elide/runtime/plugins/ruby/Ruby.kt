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
package elide.runtime.plugins.ruby

import org.graalvm.polyglot.Source
import java.nio.charset.StandardCharsets
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.enableOptions
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@DelicateElideApi public class Ruby(
  private val config: RubyConfig,
  @Suppress("unused") private val resources: LanguagePluginManifest? = null,
) {
  private fun initializeContext(scope: InstallationScope, context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    context.enter()
    try {
      scope.deferred {
        context.evaluate(rubyInitSrc)
      }
    } finally {
      context.leave()
    }
  }

  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.enableOptions(
      "ruby.cexts",
      "ruby.core-as-internal",
      "ruby.did-you-mean",
      "ruby.embedded",
      "ruby.experimental-engine-caching",
      "ruby.frozen-string-literals",
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
      "ruby.stdlib-as-internal",
      "ruby.virtual-thread-fibers",
      "ruby.use-truffle-regex",
      "ruby.warn-locale",
    )

    // builder.setOptions(
    // "log.level" to "OFF",
    // )
  }

  public companion object Plugin : AbstractLanguagePlugin<RubyConfig, Ruby>() {
    private const val RUBY_LANGUAGE_ID = "ruby"
    private const val RUBY_PLUGIN_ID = "Ruby"
    override val languageId: String = RUBY_LANGUAGE_ID
    override val key: Key<Ruby> = Key(RUBY_PLUGIN_ID)

    @JvmStatic private val rubyInitSrc: Source = requireNotNull(
      Ruby::class.java.classLoader.getResourceAsStream("META-INF/elide/embedded/runtime/ruby/environment.rb")
    ) {
      "Failed to locate `environment.rb`; please check the classpath"
    }.bufferedReader(StandardCharsets.UTF_8).use {
      it.readText().let {
        Source.newBuilder(RUBY_LANGUAGE_ID, it, "<elide>/environment.rb")
          .name("environment.rb")
          .cached(true)
          .internal(true)
          .interactive(false)
          .build()
      }
    }

    override fun install(scope: InstallationScope, configuration: RubyConfig.() -> Unit): Ruby {
      configureLanguageSupport(scope)

      // apply the configuration and create the plugin instance
      val config = RubyConfig().apply(configuration)
      configureSharedBindings(scope, config)

      // val resources = resolveEmbeddedManifest(scope)
      val instance = Ruby(config)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized) { instance.initializeContext(scope, it) }

      // register resources with the VFS
      return instance
    }
  }
}
