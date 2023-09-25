package elide.runtime.plugins.kotlin

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.getOrInstall
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest
import elide.runtime.plugins.jvm.Jvm

@Suppress("unused") @DelicateElideApi public class Kotlin private constructor(
  private val config: KotlinConfig,
  private val resources: LanguagePluginManifest,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }
  
  private fun configureContext(builder: PolyglotContextBuilder) {
    // nothing to do here
  }
  
  public companion object Plugin : AbstractLanguagePlugin<KotlinConfig, Kotlin>() {
    private const val KOTLIN_LANGUAGE_ID = "kt"
    private const val KOTLIN_PLUGIN_ID = "Kotlin"
    
    override val languageId: String = KOTLIN_LANGUAGE_ID
    override val key: Key<Kotlin> = Key(KOTLIN_PLUGIN_ID)
    
    override fun install(scope: InstallationScope, configuration: KotlinConfig.() -> Unit): Kotlin {
      // apply the JVM plugin first
      scope.configuration.getOrInstall(Jvm)
      
      // apply the configuration and create the plugin instance
      val config = KotlinConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)
      val instance = Kotlin(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}