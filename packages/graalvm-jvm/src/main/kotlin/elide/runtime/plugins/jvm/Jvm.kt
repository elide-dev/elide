package elide.runtime.plugins.jvm

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@Suppress("unused") @DelicateElideApi public class Jvm private constructor(
  private val config: JvmConfig,
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
      "java.EnablePreview",
      "java.BuiltInPolyglotCollections",
      "java.BytecodeLevelInlining",
      "java.CHA",
      "java.HotSwapAPI",
      "java.InlineMethodHandle",
      "java.MultiThreaded",
      "java.Polyglot",
      "java.SoftExit",
      "java.SplitMethodHandles",
    )
    
    builder.disableOptions(
      "java.EnableAgents",
      "java.EnableManagement",
      "java.ExposeNativeJavaVM",
    )
  }
  
  public companion object Plugin : AbstractLanguagePlugin<JvmConfig, Jvm>() {
    private const val JVM_LANGUAGE_ID = "jvm"
    private const val JVM_PLUGIN_ID = "JVM"
    
    override val languageId: String = JVM_LANGUAGE_ID
    override val key: Key<Jvm> = Key(JVM_PLUGIN_ID)
    
    override fun install(scope: InstallationScope, configuration: JvmConfig.() -> Unit): Jvm {
      // apply the configuration and create the plugin instance
      val config = JvmConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)
      val instance = Jvm(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}