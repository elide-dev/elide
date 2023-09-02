package elide.runtime.plugins.python

import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EngineLifecycleEvent.ContextCreated
import elide.runtime.core.EngineLifecycleEvent.ContextInitialized
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.EnginePlugin.Key
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotContextBuilder
import elide.runtime.core.extensions.disableOptions
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOptions
import elide.runtime.plugins.AbstractLanguagePlugin
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest

@DelicateElideApi public class Python(
  private val config: PythonConfig,
  private val resources: LanguagePluginManifest,
) {
  private fun initializeContext(context: PolyglotContext) {
    // apply init-time settings
    config.applyTo(context)

    // run embedded initialization code
    initializeEmbeddedScripts(context, resources)
  }
  
  private fun configureContext(builder: PolyglotContextBuilder) {
    builder.disableOptions(
      "python.UsePanama",
      "python.EmulateJython",
    )
    
    builder.enableOptions(
      "llvm.AOTCacheStore",
      "llvm.AOTCacheLoad",
      "llvm.C++Interop",
      "llvm.lazyParsing",
      "python.NativeModules",
      "python.LazyStrings",
      "python.WithCachedSources",
      "python.WithTRegex",
    )
    
    builder.setOptions(
      "llvm.OSR" to "BYTECODE",
      "python.PosixModuleBackend" to "java",
      "python.CoreHome" to "/python/lib/graalpy23.1",
      "python.PythonHome" to "/python",
    )
  }
  
  public companion object Plugin : AbstractLanguagePlugin<PythonConfig, Python>() {
    private const val PYTHON_LANGUAGE_ID = "python"
    private const val PYTHON_PLUGIN_ID = "Python"

    override val languageId: String = PYTHON_LANGUAGE_ID
    override val key: Key<Python> = Key(PYTHON_PLUGIN_ID)

    override fun install(scope: InstallationScope, configuration: PythonConfig.() -> Unit): Python {
      // apply the configuration and create the plugin instance
      val config = PythonConfig().apply(configuration)
      val resources = resolveEmbeddedManifest(scope)
      val instance = Python(config, resources)

      // subscribe to lifecycle events
      scope.lifecycle.on(ContextCreated, instance::configureContext)
      scope.lifecycle.on(ContextInitialized, instance::initializeContext)

      // register resources with the VFS
      installEmbeddedBundles(scope, resources)

      return instance
    }
  }
}