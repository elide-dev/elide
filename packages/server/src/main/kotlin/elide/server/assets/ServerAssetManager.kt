package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.common.graph.ElementOrder
import com.google.common.graph.ImmutableNetwork
import com.google.common.graph.Network
import com.google.common.graph.NetworkBuilder
import com.google.common.util.concurrent.Futures
import com.google.protobuf.util.JsonFormat
import elide.annotations.API
import elide.server.AssetModuleId
import elide.server.AssetTag
import elide.server.StreamedAsset
import elide.server.StreamedAssetResponse
import elide.server.cfg.ServerConfig
import elide.server.runtime.AppExecutor
import io.micronaut.caffeine.cache.Cache
import io.micronaut.caffeine.cache.Caffeine
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.http.HttpResponse
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.asDeferred
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tools.elide.assets.AssetBundle
import tools.elide.assets.ManifestFormat
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Built-in asset manager implementation for use with Elide applications.
 *
 * Resolves and loads assets embedded in the application at build-time, based on binary-encoded protocol buffer messages
 * which define the dependency structure and metadata of each embedded asset.
 *
 * @param exec Application executor that should be used for asset-related background work.
 * @param reader Active asset reader implementation for this server run.
 * @param resolver Active asset resolver implementation for this server run.
 */
@Suppress("UnstableApiUsage")
@Context
@Singleton
public class ServerAssetManager @Inject constructor(
  private val manifestLoader: AssetManifestLoader,
  private val exec: AppExecutor,
  override val reader: AssetReader,
  override val resolver: AssetResolver,
  private val config: ServerConfig,
) : AssetManager {
  public companion object {
    // Wait timeout in seconds for initialization.
    public const val waitTimeout: Long = 10L
    private const val assetRoot = "/assets"
    private val assetManifestCandidates = listOf(
      ManifestFormat.BINARY to "$assetRoot/assets.assets.pb",
      ManifestFormat.JSON to "$assetRoot/assets.assets.pb.json",
    )
  }

  /** Listens for server startup and runs hooks. */
  @Singleton public class AssetStartupListener : ApplicationEventListener<ServerStartupEvent> {
    @Inject private lateinit var beanContext: BeanContext

    /** @inheritDoc */
    override fun onApplicationEvent(event: ServerStartupEvent) {
      // initialize the asset manager
      beanContext.getBean(AssetManager::class.java).initialize()
    }
  }

  // Pointer to a specific asset within the live asset bundle.
  internal data class AssetPointer(
    val moduleId: AssetModuleId,
    val type: AssetType,
    val index: Int?,
  )

  // Dependency info for a relationship between artifacts.
  internal data class AssetDependency(
    val optional: Boolean = false,
  )

  // Loaded and interpreted manifest structure.
  internal class InterpretedAssetManifest(
    internal val bundle: AssetBundle,
    internal val moduleIndex: SortedMap<AssetModuleId, AssetPointer>,
    internal val tagIndex: SortedMap<AssetTag, Int>,
  )

  // Swappable manifest loader.
  @API public interface AssetManifestLoader {
    /**
     * Find and load an asset manifest embedded within the scope of the current application;
     */
    public fun findLoadManifest(): AssetBundle?

    /**
     *
     */
    public fun findManifest(): Pair<ManifestFormat, InputStream>?
  }

  @Singleton internal class ServerManifestLoader : AssetManifestLoader {
    private val logging: Logger = LoggerFactory.getLogger(ServerManifestLoader::class.java)

    @VisibleForTesting
    @Suppress("TooGenericExceptionCaught")
    internal fun deserializeLoadManifest(subject: Pair<ManifestFormat, InputStream>): AssetBundle? {
      val (format, stream) = subject
      logging.debug(
        "Decoding manifest from detected format '${format.name}'"
      )
      val result = try {
        when (format) {
          ManifestFormat.BINARY -> stream.buffered().use {
            AssetBundle.parseFrom(it)
          }

          ManifestFormat.JSON -> stream.bufferedReader(StandardCharsets.UTF_8).use { buf ->
            val builder = AssetBundle.newBuilder()
            JsonFormat.parser().ignoringUnknownFields().merge(
              buf,
              builder,
            )
            builder.build()
          }

          else -> {
            logging.warn(
              "Cannot de-serialize asset manifest with format: '${format.name}'. Asset loading disabled."
            )
            null
          }
        }
      } catch (thr: Throwable) {
        logging.error("Failed to load asset manifest", thr)
        null
      }
      return if (result == null) {
        null
      } else {
        val algo = result.settings.digestSettings.algorithm
        val encoded = Base64.getEncoder().withoutPadding()
          .encodeToString(result.digest.toByteArray())
        logging.debug(
          "Resolved asset manifest with fingerprint ${algo.name}($encoded)"
        )
        result
      }
    }

    /** @inheritDoc */
    override fun findLoadManifest(): AssetBundle? {
      val found = findManifest()
      logging.debug(
        if (found != null) {
          "Located asset manifest: loading"
        } else {
          "No asset manifest located. Asset loading will be disabled."
        }
      )
      return if (found == null) {
        // we couldn't locate a manifest.
        null
      } else deserializeLoadManifest(
        found
      )
    }

    /** @inheritDoc */
    override fun findManifest(): Pair<ManifestFormat, InputStream>? {
      // find the first manifest that exists
      return assetManifestCandidates.firstNotNullOfOrNull {
        val (format, path) = it
        logging.trace(
          "Checking for manifest at resource location '$path'"
        )
        val result = ServerAssetManager::class.java.getResourceAsStream(path)
        logging.trace(
          if (result != null) {
            "Found manifest at resource location '$path'"
          } else {
            "No manifest found at resource location '$path'"
          }
        )
        if (result == null) {
          null
        } else {
          format to result
        }
      }
    }
  }

  // Whether the manager has started initializing yet.
  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Whether the manager has finished initializing.
  private val ready: AtomicBoolean = AtomicBoolean(false)

  // Wait latch for asset consumers.
  private val latch: CountDownLatch = CountDownLatch(1)

  // Cache for rendered asset results.
  private val assetCache: Cache<ServerAsset, RenderedAsset> = Caffeine.newBuilder()
    .build()

  // Dependency graph loaded from the embedded manifest.
  private val dependencyGraph: AtomicReference<Network<AssetModuleId, AssetDependency>?> =
    AtomicReference(null)

  // Interpreted manifest structure loaded from the embedded proto document.
  private val assetManifest: AtomicReference<InterpretedAssetManifest?> = AtomicReference(null)

  /** @inheritDoc */
  override val logging: Logger = LoggerFactory.getLogger(AssetManager::class.java)

  @Synchronized override fun initialize() {
    if (initialized.compareAndSet(false, true)) {
      exec.service().run {
        // read the embedded asset bundle
        val assetBundle = manifestLoader.findLoadManifest() ?: return@run

        // build index of assets to modules and tags
        val (graph, manifest) = buildAssetIndexes(assetBundle)
        assetManifest.set(manifest)
        dependencyGraph.set(graph)

        // allow asset serving now
        latch.countDown()
      }
    }
  }

  private fun addDirectDeps(
    moduleId: String,
    depGraph: ImmutableNetwork.Builder<AssetModuleId, AssetDependency>,
    deps: AssetBundle.AssetDependencies,
  ) {
    // add only direct dependencies
    deps.directList.forEach {
      depGraph.addEdge(
        moduleId,
        it,
        AssetDependency(optional = false),
      )
    }
  }

  private fun pointerForConcrete(
    type: AssetType,
    key: AssetModuleId,
    idx: Int?,
    bundle: AssetBundle,
    depGraph: ImmutableNetwork.Builder<AssetModuleId, AssetDependency>,
  ): AssetPointer = when (type) {
    // JavaScript assets
    AssetType.SCRIPT -> {
      val script = bundle.getScriptsOrThrow(key)
      if (script.hasDependencies()) addDirectDeps(
        key,
        depGraph,
        script.dependencies,
      )
      AssetPointer(
        moduleId = key,
        type = type,
        index = idx,
      )
    }

    // CSS assets
    AssetType.STYLESHEET -> {
      val sheet = bundle.getStylesOrThrow(key)
      if (sheet.hasDependencies()) addDirectDeps(
        key,
        depGraph,
        sheet.dependencies,
      )
      AssetPointer(
        moduleId = key,
        type = type,
        index = idx,
      )
    }

    // generic assets
    AssetType.TEXT -> {
      bundle.getGenericOrThrow(
        key
      )
      AssetPointer(
        moduleId = key,
        type = type,
        index = idx,
      )
    }

    else -> error("Unsupported asset type for pointer: '${type.name}'")
  }

  private fun buildAssetIndexes(
    bundle: AssetBundle
  ): Pair<Network<AssetModuleId, AssetDependency>, InterpretedAssetManifest> {
    // create a builder for the asset graph
    val builder: ImmutableNetwork.Builder<AssetModuleId, AssetDependency> = NetworkBuilder
      .directed()
      .allowsParallelEdges(false)
      .allowsSelfLoops(false)
      .nodeOrder(ElementOrder.stable<AssetModuleId>())
      .edgeOrder(ElementOrder.stable<AssetDependency>())
      .immutable()

    // first, build the asset tag index by iterating over content, which references assets by module ID. we can then
    // use this index while building the asset module index, enabled with processed content records.
    val tagIndex = IntStream.rangeClosed(0, bundle.assetCount - 1).parallel().mapToObj {
      it to bundle.getAsset(it)
    }.map {
      val (idx, content) = it
      content.module to idx
    }.collect(
      Collectors.toMap(
        { it.first },
        { it.second },
        { left, _ ->
          error(
            "Found assets with duplicate tags: '$left'. This should not happen; please report this bug to the Elide " +
            "project authors: https://github.com/elide-dev"
          )
        },
        { TreeMap() }
      )
    )

    // build an index of each module ID => a module record. we can typically get to the module ID from everything else.
    val typeRanges = listOf(
      AssetType.SCRIPT to bundle.scriptsMap.keys,
      AssetType.STYLESHEET to bundle.stylesMap.keys,
      AssetType.TEXT to bundle.genericMap.keys,
    )
    val moduleIndex = typeRanges.parallelStream().flatMap {
      val (type, keys) = it
      keys.parallelStream().map { key ->
        type to key
      }
    }.map {
      // we're working with a type of asset and index in the array here. so we need to use a concrete extractor, but we
      // are just assembling an index, so we return back to generic use quickly.
      val (assetType, moduleId) = it
      builder.addNode(moduleId)
      it.second to pointerForConcrete(
        assetType,
        moduleId,
        tagIndex[moduleId],
        bundle,
        builder,
      )
    }.collect(
      Collectors.toMap(
        { it.first }, // module ID
        { it.second }, // pointer
        { value, _, -> error("Two assets cannot have the same module ID: '$value'") },
        { ConcurrentSkipListMap() }
      )
    )
    return builder.build() to InterpretedAssetManifest(
      bundle = bundle,
      moduleIndex = moduleIndex,
      tagIndex = tagIndex,
    )
  }

  // Build an HTTP asset response from the provided asset result.
  private fun buildAssetResponse(asset: RenderedAsset): StreamedAssetResponse {
    TODO("not yet implemented")
  }

  /** @inheritDoc */
  @OptIn(ExperimentalCoroutinesApi::class)
  override suspend fun renderAssetAsync(asset: ServerAsset): Deferred<StreamedAssetResponse> {
    // if asset serving is disabled, return a 404 for all asset calls.
    if (!config.assets.enabled) {
      return Futures.immediateFuture(
        HttpResponse.notFound<StreamedAsset>()
      ).asDeferred()
    }

    return withContext(Dispatchers.IO) {
      // if the asset system is still initializing, wait for it to complete
      if (!ready.get()) {
        initialize()
        latch.await(
          waitTimeout,
          java.util.concurrent.TimeUnit.SECONDS
        )
      }

      async {
        // pass off to the reader to read the asset
        val assetContent = reader.readAsync(asset)
        assetContent.invokeOnCompletion {
          if (it != null) {
            logging.error("Error reading asset: $asset", it)
          } else {
            assetCache.put(asset, assetContent.getCompleted())
          }
        }
        buildAssetResponse(
          assetContent.await()
        )
      }
    }
  }
}
