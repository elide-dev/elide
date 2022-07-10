package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.common.graph.ElementOrder
import com.google.common.graph.ImmutableNetwork
import com.google.common.graph.Network
import com.google.common.graph.NetworkBuilder
import elide.server.AssetModuleId
import elide.server.runtime.AppExecutor
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Inject
import tools.elide.assets.AssetBundle
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Server-side utility which, at server startup, consumes the embedded asset bundle (if any), and generates a set of
 * runtime indexes therefrom.
 *
 * Indexes produced by this object include `(assetModuleId => assetPointer)`, and `(assetTag => idx)`. The first of
 * these can be used to efficiently resolve assets when you know the module ID before-hand. The second can be used to
 * directly resolve an asset tag from a URL to a content payload.
 *
 * Asset content payloads enclose their module ID, so if you need to resolve an `assetTag` to a pointer, you can simply
 * resolve the `assetTag` to the content payload index, resolve the content payload, and then use the other index to
 * get to an `assetPointer`, which should have everything you need.
 *
 * ### Dependency graph
 *
 * In addition to the indexes described above, an immutable directed graph is computed from asset dependencies described
 * in the bundle. The dependency graph is used to determine load order and dependency resolution when including assets
 * on a page.
 *
 * ### Startup sequence
 *
 * At server startup, [AssetStartupListener] is dispatched, which acquires a bean of [ServerAssetIndex]. The bean then
 * initializes by calling into [ServerAssetManifestProvider] (having been initialized by the DI container already), and
 * waits until a materialized asset bundle is ready. From that bundle, indexes are computed and then made live.
 *
 * @param exec Background executor which should be used to load and index assets.
 * @param manifestProvider Provider for the de-serialized asset manifest. Responsible for locating the bundle within the
 *   current application and de-serializing it into an interpreted manifest.
 */
@Context
@Suppress("UnstableApiUsage")
internal class ServerAssetIndex @Inject constructor(
  private val exec: AppExecutor,
  private val manifestProvider: AssetManifestLoader,
) {
  // Wait latch for asset consumers.
  private val latch: CountDownLatch = CountDownLatch(1)

  /** Listens for server startup and runs hooks. */
  @Context internal class AssetStartupListener : ApplicationEventListener<ServerStartupEvent> {
    @Inject private lateinit var beanContext: BeanContext

    /** @inheritDoc */
    override fun onApplicationEvent(event: ServerStartupEvent) {
      // initialize the asset manager
      beanContext.getBean(ServerAssetIndex::class.java).initialize()
    }
  }

  // Whether the manager has started initializing yet.
  internal val initialized: AtomicBoolean = AtomicBoolean(false)

  // Dependency graph loaded from the embedded manifest.
  internal val dependencyGraph: AtomicReference<Network<AssetModuleId, AssetDependency>> =
    AtomicReference(null)

  // Interpreted manifest structure loaded from the embedded proto document.
  internal val assetManifest: AtomicReference<ServerAssetManifest?> = AtomicReference(null)

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
        AssetDependency(
          depender = moduleId,
          dependee = it,
          optional = false
        ),
      )
    }
  }

  @VisibleForTesting
  internal fun buildAssetIndexes(
    bundle: AssetBundle
  ): Pair<Network<AssetModuleId, AssetDependency>, ServerAssetManifest> {
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
    return builder.build() to ServerAssetManifest(
      bundle = bundle,
      moduleIndex = moduleIndex,
      tagIndex = tagIndex,
    )
  }

  @VisibleForTesting
  internal fun pointerForConcrete(
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

  @VisibleForTesting
  @Synchronized
  internal fun initialize() {
    if (initialized.compareAndSet(false, true)) {
      exec.service().run {
        // read the embedded asset bundle
        val assetBundle = manifestProvider.findLoadManifest() ?: return@run

        // build index of assets to modules and tags
        val (graph, manifest) = buildAssetIndexes(assetBundle)
        assetManifest.set(manifest)
        dependencyGraph.set(graph)

        // allow asset serving now
        latch.countDown()
      }
    }
  }
}
