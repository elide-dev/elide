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