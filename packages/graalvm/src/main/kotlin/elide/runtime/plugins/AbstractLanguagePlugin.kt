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

package elide.runtime.plugins

import java.util.zip.GZIPInputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import elide.runtime.core.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest.EmbeddedResource
import elide.runtime.plugins.vfs.Vfs
import elide.runtime.plugins.vfs.include

/**
 * Abstract base class for language plugins.
 */
@DelicateElideApi public abstract class AbstractLanguagePlugin<C : Any, I : Any> : EnginePlugin<C, I>, GuestLanguage {
  /**
   * A key used to resolve the [LanguagePluginManifest] from resources. This is the name of the directory containing
   * the manifest. Defaults to [languageId].
   *
   * @see resolveEmbeddedManifest
   */
  protected open val manifestKey: String get() = languageId

  /** Provides information about resources embedded into the runtime, used by language plugins. */
  @Serializable public data class LanguagePluginManifest(
    /** The engine version these resources are meant to be used with. */
    val engine: String,
    /** The language these resources are meant to be used with. */
    val language: String,
    /** A list of URIs representing bundles to be preloaded into the VFS by the plugin. */
    val bundles: List<EmbeddedResource> = emptyList(),
    /** Guest scripts to be evaluated on context initialization. */
    val scripts: List<EmbeddedResource> = emptyList(),
    /** A collection of plugin-specific resource entries. */
    val resources: Map<String, List<String>> = emptyMap(),
    /** The path to the directory containing the manifest in the embedded resources. */
    val root: String? = null,
  ) {
    /** Represents an embedded resource that should be loaded by the language plugin. */
    @Serializable public data class EmbeddedResource(
      /** Path to the resource, relative to the plugin manifest. */
      val path: String,
      /** Optionally sets a target platform for this resource. If not present, the resource will always be loaded. */
      @Serializable(with = EmbeddedHostPlatformSerializer::class)
      val platform: HostPlatform? = null,
    )

    /** Custom [HostPlatform] serializer using a primitive string form instead of an object. */
    internal object EmbeddedHostPlatformSerializer : KSerializer<HostPlatform> {
      override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "HostPlatform",
        kind = PrimitiveKind.STRING,
      )

      override fun deserialize(decoder: Decoder): HostPlatform {
        return HostPlatform.parsePlatform(decoder.decodeString())
      }

      override fun serialize(encoder: Encoder, value: HostPlatform) {
        encoder.encodeString(value.platformString())
      }
    }
  }

  /**
   * Resolve and deserialize the runtime manifest for this plugin from resources. Any embedded resources listed in the
   * manifest are filtered to match the current platform information provided in the [scope].
   *
   * @param scope The installation scope used to resolve host platform information.
   * @param lenient Whether to load the manifest in a backwards-compatible manner, e.g. ignoring unknown keys.
   * @return The [LanguagePluginManifest] for this plugin's language.
   * @see installEmbeddedBundles
   */
  protected fun resolveEmbeddedManifest(scope: InstallationScope, lenient: Boolean = true): LanguagePluginManifest {
    return resolveEmbeddedManifest(scope.configuration.hostPlatform, lenient)
  }

  /**
   * Resolve and deserialize the runtime manifest for this plugin from resources. Any embedded resources listed in the
   * manifest are filtered to match the given [platform].
   *
   * @param platform the host platform used to filter embedded resources.
   * @param lenient Whether to load the manifest in a backwards-compatible manner, e.g. ignoring unknown keys.
   * @return The [LanguagePluginManifest] for this plugin's language.
   * @see installEmbeddedBundles
   */
  @OptIn(ExperimentalSerializationApi::class)
  protected fun resolveEmbeddedManifest(
    platform: HostPlatform,
    lenient: Boolean = true
  ): LanguagePluginManifest = runCatching {
    // resolve path relative to the common root
    val resourcesRoot = "$EMBEDDED_RESOURCES_ROOT/${manifestKey}"
    fun relativeToRoot(path: String) = "$resourcesRoot/$path"

    // read and deserialize manifest
    val manifest = AbstractLanguagePlugin::class.java.getResourceAsStream(relativeToRoot(RUNTIME_MANIFEST))?.let {
      // deserialize the manifest
      GZIPInputStream(it).use { gzipStream ->
        (if (lenient) LenientJson else Json).decodeFromStream<LanguagePluginManifest>(gzipStream)
      }
    } ?: error(
      "Failed to locate embedded runtime manifest at path '$resourcesRoot'",
    )

    // resolve resource paths relative to the manifest, and filter out resources for other platforms
    fun mapResource(resource: EmbeddedResource) = resource.takeUnless {
      it.platform != null && it.platform != platform
    }?.copy(path = relativeToRoot(resource.path))

    manifest.copy(
      bundles = manifest.bundles.mapNotNull(::mapResource),
      scripts = manifest.scripts.mapNotNull(::mapResource),
      root = resourcesRoot,
    )
  }.getOrElse { cause ->
    // rethrow with a more meaningful message
    throw Exception("Failed to resolve language resources with key $manifestKey for language $languageId", cause)
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
  protected fun installEmbeddedBundles(scope: InstallationScope, resources: LanguagePluginManifest) {
    // resolve the VFS plugin (install it if not present to avoid explicit installation requirements)
    val vfs = scope.configuration.getOrInstall(Vfs).config

    // add embedded bundles to the VFS
    for (it in resources.bundles) vfs.include(
      AbstractLanguagePlugin::class.java.getResource(it.path) ?: error("Failed to load embedded resource: $it"),
    )
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
  protected fun initializeEmbeddedScripts(context: PolyglotContext, resources: LanguagePluginManifest) {
    resources.scripts.forEach { source ->
      // read the script from resources
      val script = AbstractLanguagePlugin::class.java.getResourceAsStream(source.path)
        ?: error("Failed to load embedded resource: $source")

      try {
        context.evaluate(
          this,
          script.bufferedReader().use { it.readText() },
          name = source.path.split("/").last(),
          internal = true,
        )
      } catch (err: Throwable) {
        // swallow @TODO(sgammon)
      }
    }
  }

  protected companion object {
    /** Root resources path where embedded language resources are placed. */
    private const val EMBEDDED_RESOURCES_ROOT = "/META-INF/elide/embedded/runtime"

    /** Name of the manifest file for embedded language resources */
    private const val RUNTIME_MANIFEST = "runtime.json.gz"

    /** Lenient variant of the [Json] codec, used for backwards-compatibility reasons. */
    private val LenientJson by lazy {
      Json { ignoreUnknownKeys = true }
    }
  }
}
