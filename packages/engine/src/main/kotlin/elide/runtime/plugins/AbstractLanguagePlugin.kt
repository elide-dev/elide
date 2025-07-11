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
package elide.runtime.plugins

import org.graalvm.nativeimage.ImageInfo
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.PolyglotAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.nio.charset.StandardCharsets
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
import kotlin.io.bufferedReader
import elide.runtime.core.*
import elide.runtime.core.EnginePlugin.InstallationScope
import elide.runtime.core.extensions.enableOptions
import elide.runtime.core.extensions.setOptions
import elide.runtime.plugins.AbstractLanguagePlugin.LanguagePluginManifest.EmbeddedResource
import elide.runtime.plugins.bindings.Bindings

// Default engine's resident builder.
private val defaultEngineBuilder = Engine.newBuilder()
  .initializeDefaultEngine()

// Default engine for pre-warming.
private val defaultEngine = defaultEngineBuilder.build()

// Default context's resident builder.
private val defaultContextBuilder = Context.newBuilder()
  .engine(defaultEngine)
  .initializeDefaultContext(defaults = true)

// Default context for pre-warming.
private val defaultContext = defaultContextBuilder.build()

// JIT compilation enabled by default in GraalVM.
private val truffleJitEnabled = (
  System.getProperty("truffle.TruffleRuntime") != "com.oracle.truffle.api.impl.DefaultTruffleRuntime"
  )

// Initialize default engine settings.
@OptIn(DelicateElideApi::class)
private fun Engine.Builder.initializeDefaultEngine(): Engine.Builder = apply {
  allowExperimentalOptions(true)

  // on jvm, we need to honor system properties
  if (ImageInfo.inImageCode()) {
    useSystemProperties(false)
  }

  // in non-optimizing runtime circumstances, we can't enable jit features
  if (truffleJitEnabled) {
    setOptions(
      "engine.Mode" to "latency",
    )
    enableOptions(
      "engine.BackgroundCompilation",
      "engine.UsePreInitializedContext",
      "engine.Compilation",
      "engine.MultiTier",
      "engine.Splitting",
      "engine.OSR",
    )
  }
}

// Default host access privileges.
private val contextHostAccess = HostAccess.newBuilder(HostAccess.ALL)
  .allowImplementationsAnnotatedBy(HostAccess.Implementable::class.java)
  .allowAccessAnnotatedBy(HostAccess.Export::class.java)
  .allowArrayAccess(true)
  .allowBufferAccess(true)
  .allowAccessInheritance(true)
  .allowIterableAccess(true)
  .allowIteratorAccess(true)
  .allowListAccess(true)
  .allowMapAccess(true)
  .build()

// Initialize default context settings.
@OptIn(DelicateElideApi::class)
public fun Context.Builder.initializeDefaultContext(defaults: Boolean = false): Context.Builder = apply {
  allowExperimentalOptions(true)
  allowValueSharing(true)
  allowCreateThread(true)
  allowCreateProcess(true)
  allowHostClassLoading(true)
  allowAllAccess(true)
  allowNativeAccess(true)
  allowHostClassLookup { true }
  allowInnerContextOptions(true)
  allowHostAccess(contextHostAccess)
  allowPolyglotAccess(PolyglotAccess.ALL)
  useSystemExit(false)

  if (!defaults) {
    setOptions(
      "js.ecmascript-version" to "2024",
    )
    enableOptions(
      "js.atomics",
      "js.class-fields",
      "js.direct-byte-buffer",
      "js.global-property",
      "js.error-cause",
      "js.foreign-hash-properties",
      "js.foreign-object-prototype",
      "js.import-attributes",
      "js.intl-402",
      "js.iterator-helpers",
      "js.json-modules",
      "js.lazy-translation",
      "js.new-set-methods",
      "js.performance",
      "js.shared-array-buffer",
      "js.strict",
      "js.temporal",
      "js.webassembly",
      // Experimental:
      "js.async-context",
      "js.async-iterator-helpers",
      "js.async-stack-traces",
      "js.atomics-wait-async",
      "js.bind-member-functions",
      "js.esm-eval-returns-exports",
      "js.scope-optimization",
    )
  }
}

/**
 * @return The default polyglot engine builder.
 */
public fun defaultPolyglotEngineBuilder(): Engine.Builder = defaultEngineBuilder

/**
 * @return The default polyglot context builder.
 */
public fun defaultPolyglotContextBuilder(): Context.Builder = defaultContextBuilder

/**
 * @return The default polyglot engine.
 */
public fun defaultPolyglotEngine(): Engine = defaultEngine

/**
 * @return The default polyglot context.
 */
public fun defaultPolyglotContext(): Context = defaultContext

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

  /**
   * Pre-initialization script built by [initializePreambleScripts], which returns a suite of sources which are
   * initialized at build time; these scripts should be passed verbatim to [executePreambleScripts] at runtime.
   *
   * @property name Given name of the preamble script.
   * @property source The source of the preamble script.
   * @property entry Pre-parsed entrypoint for the script.
   */
  @JvmRecord public data class PreinitScript(
    val name: String,
    val source: Source,
    val entry: Value? = null,
  )

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
   * Configure the [scope] to enable support for this guest language. This method will automatically call
   * [PolyglotEngineConfiguration.enableLanguage].
   *
   * @param scope The installation scope during an [install] call.
   */
  protected fun configureLanguageSupport(scope: InstallationScope) {
    scope.configuration.enableLanguage(this)
  }

  /**
   * Configure the shared intrinsics provided by the [Bindings] plugin, adding them to this plugin's [config].
   *
   * This method can be used by language plugins to opt into the shared bindings feature, which allows dinamic
   * resolution of common language intrinsics at configuration time.
   *
   * If the [Bindings] plugin is not installed at the time of this call, no changes will be applied. This is a current
   * limitation that will be lifted with future updates to the plugins API.
   */
  protected fun configureSharedBindings(scope: InstallationScope, config: AbstractLanguageConfig) {
    scope.configuration.plugin(Bindings)?.let {
      config.bindings { it.applyTo(this, scope) }
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
  protected fun resolveEmbeddedManifest(platform: HostPlatform, lenient: Boolean = true): LanguagePluginManifest =
    runCatching {
      // resolve path relative to the common root
      val resourcesRoot = "$EMBEDDED_RESOURCES_ROOT/$manifestKey"
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
      throw IllegalStateException(
        "Failed to resolve language resources with key $manifestKey for language $languageId",
        cause,
      )
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
    // add embedded bundles to the VFS
    for (it in resources.bundles) scope.registerBundle(
      AbstractLanguagePlugin::class.java.getResource(it.path) ?: error("Failed to load embedded resource: $it")
    )
  }

  /**
   * Run the setup scripts specified in this plugin's [resources] in the provided [context].
   *
   * This function will typically be called by plugins in response to the
   * [ContextInitialized][elide.runtime.core.EngineLifecycleEvent.ContextInitialized] event.
   *
   * @param context A [PolyglotContext] used to execute the initialization scripts.
   * @param sources Sources prepared by [initializePreambleScripts].
   */
  @Suppress("TooGenericExceptionCaught")
  protected fun executePreambleScripts(context: PolyglotContext, sources: List<PreinitScript> = emptyList(),) {
    sources.forEach { script ->
      context.evaluate(script.source)
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
  @Deprecated("Use initializePreambleScripts instead.")
  @Suppress("TooGenericExceptionCaught")
  protected fun initializeEmbeddedScripts(context: PolyglotContext, resources: LanguagePluginManifest) {
    try {
      context.enter()
      resources.scripts.forEach { source ->
        // read the script from resources
        val script = AbstractLanguagePlugin::class.java.getResourceAsStream(source.path)
          ?: error("Failed to load embedded resource: $source")

        try {
          context.evaluate(
            this,
            script.bufferedReader().use { it.readText() },
            name = source.path.substringAfterLast('/'),
            internals = true,
            cached = true,
          )
        } catch (err: RuntimeException) {
          if (System.getProperty("elide.strict") == "true") {
            throw IllegalStateException(
              "Embedded init evaluation failed at unit '${source.path}'. This is a bug in Elide.",
              err,
            )
          }
        }
      }
    } finally {
      context.leave()
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

    /**
     * Run the setup scripts specified in this plugin's [resources].
     *
     * This function will typically be called by plugins in response to the
     * [ContextInitialized][elide.runtime.core.EngineLifecycleEvent.ContextInitialized] event.
     *
     * @param langId ID of the owning language.
     * @param prewarm Whether to activate pre-warming for these scripts.
     * @param resources The embedded resources for this plugin, providing the script sources.
     */
    @Suppress("TooGenericExceptionCaught")
    @JvmStatic protected fun initializePreambleScripts(
      langId: String,
      vararg scripts: String,
      prewarm: Boolean = true,
    ): List<PreinitScript> {
      val path = "META-INF/elide/embedded/runtime/$langId"
      if (prewarm) defaultContext.enter()

      try {
        return scripts.map { script ->
          val source = this::class.java.classLoader.getResourceAsStream("$path/$script")
            ?: error("Failed to load embedded resource: $script")

          val src = Source.newBuilder(
            langId,
            source.bufferedReader(StandardCharsets.UTF_8),
            script.substringBefore('.')
          ).apply {
            internal(true)
            cached(true)
          }.build()

          PreinitScript(
            name = script,
            source = src,
            entry = if (prewarm) defaultContext.parse(src) else null,
          )
        }
      } finally {
        if (prewarm) defaultContext.leave()
      }
    }
  }
}
