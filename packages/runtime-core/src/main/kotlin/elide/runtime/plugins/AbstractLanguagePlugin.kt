package elide.runtime.plugins

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.EnginePlugin
import elide.runtime.core.GuestLanguage

/**
 * Abstract base class for language plugins.
 */
@DelicateElideApi public abstract class AbstractLanguagePlugin<C : Any, I : Any> : EnginePlugin<C, I>, GuestLanguage {
  /** Provides information about resources embedded into the runtime, used by language plugins. */
  @Serializable public data class EmbeddedLanguageResources(
    /** A list of URIs representing bundles to be preloaded into the VFS by the plugin. */
    val bundles: List<String>,
    /** Source code snippets to be evaluated on context initialization. */
    val setupScripts: List<String>,
  )

  @OptIn(ExperimentalSerializationApi::class)
  protected fun resolveLanguageResources(): EmbeddedLanguageResources = runCatching {
    // resolve path relative to the common root
    val manifestPath = "$EMBEDDED_RESOURCES_ROOT/${languageId}/$RUNTIME_MANIFEST"
    fun relativeToManifest(path: String) = "$manifestPath/$path"

    // read and deserialize manifest
    val manifest = AbstractLanguagePlugin::class.java.getResourceAsStream(manifestPath)?.let { stream ->
      // deserialize the manifest
      Json.decodeFromStream<EmbeddedLanguageResources>(stream)
    } ?: error(
      "Failed to locate embedded runtime manifest at path '$manifestPath'",
    )

    // resolve resource paths relative to the manifest
    EmbeddedLanguageResources(
      bundles = manifest.bundles.map(::relativeToManifest),
      setupScripts = manifest.setupScripts.map(::relativeToManifest),
    )
  }.getOrElse { cause ->
    // rethrow with a more meaningful message
    throw Exception("Failed to resolve embedded language resources for language $languageId", cause)
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
