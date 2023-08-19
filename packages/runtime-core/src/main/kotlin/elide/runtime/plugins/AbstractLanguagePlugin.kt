package elide.runtime.plugins

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import elide.runtime.core.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.plugins.js.JavaScript
import elide.runtime.plugins.vfs.Vfs
import elide.runtime.plugins.vfs.include

/**
 * Abstract base class for language plugins.
 */
@DelicateElideApi public abstract class AbstractLanguagePlugin<C : Any, I : Any> : EnginePlugin<C, I>, GuestLanguage {
  /** Provides information about resources embedded into the runtime, used by language plugins. */
  @Serializable public data class EmbeddedLanguageResources(
    /** The engine version these resources are meant to be used with. Used for legibility. */
    val engine: String,
    /** The language these resources are meant to be used with. Used for legibility. */
    val language: String,
    /** A list of URIs representing bundles to be preloaded into the VFS by the plugin. */
    val bundles: List<String>,
    /** Source code snippets to be evaluated on context initialization. */
    val setupScripts: List<String>,
  )

  /**
   * Resolve and deserialize the runtime manifest for this plugin, embedded as a resource.
   *
   * @return The [EmbeddedLanguageResources] for this plugin's language.
   * @see installEmbeddedBundles
   */
  @OptIn(ExperimentalSerializationApi::class)
  protected fun resolveLanguageResources(): EmbeddedLanguageResources = runCatching {
    // resolve path relative to the common root
    val resourcesRoot = "$EMBEDDED_RESOURCES_ROOT/${languageId}"
    fun relativeToRoot(path: String) = "$resourcesRoot/$path"

    // read and deserialize manifest
    val manifest = AbstractLanguagePlugin::class.java.getResourceAsStream(relativeToRoot(RUNTIME_MANIFEST))?.let {
      // deserialize the manifest
      Json.decodeFromStream<EmbeddedLanguageResources>(it)
    } ?: error(
      "Failed to locate embedded runtime manifest at path '$resourcesRoot'",
    )

    // resolve resource paths relative to the manifest
    manifest.copy(
      bundles = manifest.bundles.map(::relativeToRoot),
      setupScripts = manifest.setupScripts.map(::relativeToRoot),
    )
  }.getOrElse { cause ->
    // rethrow with a more meaningful message
    throw Exception("Failed to resolve embedded language resources for language $languageId", cause)
  }

  /**
   * Install the embedded bundles specified in the plugin's [resources] into the VFS. This method installs the VFS
   * plugin if it is not present in the installation [scope].
   *
   * This function will typically be called by plugins during [install].
   *
   * @param scope The installation scope for the plugin, used to resolve the VFS.
   * @param resources The embedded resources for this plugin, providing the list of bundles to install.
   */
  protected fun installEmbeddedBundles(scope: InstallationScope, resources: EmbeddedLanguageResources) {
    // resolve the VFS plugin (install it if not present to avoid explicit installation requirements)
    scope.configuration.getOrInstall(Vfs).config.apply {
      // add embedded bundles to the VFS
      resources.bundles.forEach {
        include(AbstractLanguagePlugin::class.java.getResource(it) ?: error("Failed to load embedded resource: $it"))
      }
    }
  }

  /**
   * Run the setup scripts specified in this plugin's [resources] in the provided [context].
   *
   * This function will typically be called by plugins in response to the
   * [ContextInitialized][elide.runtime.core.EngineLifecycleEvent.ContextInitialized] event.
   *
   * @param context A [PolyglotContext] used to execute the initialization scripts.
   * @param resources The embedded resources for this plugin, providing the script sources.
   */
  protected fun initializeEmbeddedScripts(context: PolyglotContext, resources: EmbeddedLanguageResources) {
    resources.setupScripts.forEach { source ->
      // read the script from resources
      val script = AbstractLanguagePlugin::class.java.getResourceAsStream(source)
        ?: error("Failed to load embedded resource: $source")

      context.execute(this, script.bufferedReader().use { it.readText() })
    }
  }

  protected companion object {
    /** Elide version used for resolving embedded resource paths. */
    private const val ENGINE_VERSION = "v4"

    /** Root resources path where embedded language resources are placed. */
    private const val EMBEDDED_RESOURCES_ROOT = "/META-INF/elide/$ENGINE_VERSION/embedded/runtime"

    /** Name of the manifest file for embedded language resources */
    private const val RUNTIME_MANIFEST = "runtime.json"
  }
}
