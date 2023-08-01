package elide.server.assets

import com.google.common.annotations.VisibleForTesting
import com.google.common.graph.ElementOrder
import com.google.common.graph.ImmutableNetwork
import com.google.common.graph.Network
import com.google.common.graph.NetworkBuilder
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import tools.elide.assets.AssetBundle
import tools.elide.assets.AssetBundle.AssetContent
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors
import java.util.stream.IntStream
import jakarta.inject.Inject
import kotlin.math.max
import elide.server.AssetModuleId
import elide.server.AssetTag
import elide.server.assets.ServerAssetIndex.AssetStartupListener
import elide.server.cfg.AssetConfig
import elide.util.Base64

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
 * @param assetConfig Configuration for the asset system, which is live for this server run.
 * @param manifestProvider Provider for the de-serialized asset manifest. Responsible for locating the bundle within the
 *   current application and de-serializing it into an interpreted manifest.
 */
@Context
@Suppress("UnstableApiUsage", "TooManyFunctions")
internal class ServerAssetIndex @Inject constructor(
  private val assetConfig: AssetConfig,
  private val manifestProvider: AssetManifestLoader,
) {
  companion object {
    private const val WAIT_TIMEOUT = 5L
    private const val MIN_TAIL_SIZE = 4
  }

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

  // Build an ETag value for the provided `entry`.
  @VisibleForTesting internal fun buildETagForAsset(entry: AssetContent, bundle: AssetBundle): String {
    return if (entry.variantCount > 0 && entry.getVariant(0).data.integrityCount > 0) {
      val identityVariant = entry.getVariant(0)
      val integrityValue = identityVariant.data.getIntegrity(0)
      if (!(assetConfig.preferWeakEtags ?: AssetConfig.DEFAULT_PREFER_WEAK_ETAGS)) {
        // we have an integrity tag and we prefer strong etags
        val tailCount = bundle.settings.digestSettings.tail
        val encoded = String(
          Base64.encodeWebSafe(integrityValue.fingerprint.toByteArray().takeLast(tailCount).toByteArray()),
          StandardCharsets.UTF_8
        )
        "\"$encoded\""
      } else {
        // we have an integrity tag and we prefer weak etags
        "W/\"${bundle.generated.seconds}\""
      }
    } else {
      // since we don't have an integrity fingerprint for this asset, we can substitute and use a "weak" ETag via the
      // generated-timestamp in the asset bundle.
      "W/\"${bundle.generated.seconds}\""
    }
  }

  // Build an ETag value for the provided `entry`, resolving the active manifest.
  @VisibleForTesting internal fun buildETagForAsset(entry: AssetContent): String {
    return buildETagForAsset(entry, activeManifest().bundle)
  }

  // Resolve the active manifest or fail loudly.
  @VisibleForTesting internal fun activeManifest(): ServerAssetManifest {
    require(initialized.get()) {
      "Asset manager is not initialized; `activeManifest` cannot be called yet"
    }
    return assetManifest.get()!!
  }

  @VisibleForTesting internal fun addDirectDeps(
    moduleId: String,
    depGraph: ImmutableNetwork.Builder<AssetModuleId, AssetDependency>,
    deps: AssetBundle.AssetDependencies,
  ) {
    depGraph.addNode(moduleId)

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

    // check for module and tag duplicates
    val distinctAssets = bundle.assetList.stream().map {
      it.module
    }.distinct().collect(Collectors.toSet())

    // no duplicate module IDs allowed
    if (distinctAssets.size != bundle.assetCount) {
      val dupes = bundle.assetList.stream().map {
        it.module
      }.map { moduleId ->
        moduleId to bundle.assetList.count {
          it.module == moduleId
        }
      }.collect(Collectors.toList()).joinToString(", ") {
        "${it.first} (${it.second} entries)"
      }

      error("Duplicate asset modules detected in bundle: $dupes")
    }

    val tagIndex = ConcurrentSkipListMap<AssetTag, Int>()

    // first, build a content payload index which maps each module to the content payload which implements it.
    val modulesToIndexes = IntStream.rangeClosed(0, bundle.assetCount - 1).parallel().mapToObj {
      it to bundle.getAsset(it)
    }.map {
      val (idx, content) = it

      // map the tag to both the full asset fingerprint, and the "trimmed" asset fingerprint, which is the "asset tag."
      // the asset tag length is specified on the settings payload in the manifest.
      tagIndex[content.token] = idx
      tagIndex[content.token.takeLast(max(bundle.settings.digestSettings.tail, MIN_TAIL_SIZE))] = idx
      content.module to idx
    }.collect(
      Collectors.toMap(
        { it.first },
        { sortedSetOf(it.second) },
        { _, _ -> error("Assets must hold a maximum of one source file.") },
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

      // we also need to fetch the content record so we can index the asset tag along with the other details.
      val targetIndexes = modulesToIndexes[moduleId]
      val assetContent = bundle.getAsset(targetIndexes!!.first())

      it.second to pointerForConcrete(
        assetType,
        moduleId,
        assetContent,
        modulesToIndexes[moduleId],
        bundle,
        builder,
      )
    }.collect(
      Collectors.toMap(
        { it.first }, // module ID
        { it.second }, // pointer
        { value, _ -> error("Two assets cannot have the same module ID: '$value'") },
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
    content: AssetContent,
    idx: TreeSet<Int>?,
    bundle: AssetBundle,
    depGraph: ImmutableNetwork.Builder<AssetModuleId, AssetDependency>,
  ): AssetPointer {
    // pre-emptively build an etag (if enabled)
    val token = content.token
    val etag = buildETagForAsset(content, bundle)
    val tailCount = bundle.settings.digestSettings.tail
    val tag = token.takeLast(tailCount)

    when (type) {
      // JavaScript assets
      AssetType.SCRIPT -> {
        val script = bundle.getScriptsOrThrow(key)
        if (script.hasDependencies()) addDirectDeps(
          key,
          depGraph,
          script.dependencies,
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
      }

      // generic assets
      AssetType.TEXT -> {
        bundle.getGenericOrThrow(
          key
        )
      }

      else -> error("Unsupported asset type for pointer: '${type.name}'")
    }
    return AssetPointer(
      moduleId = key,
      type = type,
      token = token,
      tag = tag,
      etag = etag,
      modified = bundle.generated.seconds,
      index = idx,
    )
  }

  @VisibleForTesting
  @Synchronized
  internal fun initialize() {
    if (initialized.compareAndSet(false, true)) {
      // read the embedded asset bundle
      val assetBundle = manifestProvider.findLoadManifest() ?: return

      // build index of assets to modules and tags
      val (graph, manifest) = buildAssetIndexes(assetBundle)
      assetManifest.set(manifest)
      dependencyGraph.set(graph)

      // allow asset serving now
      latch.countDown()
    }
  }

  @VisibleForTesting
  internal fun buildConcreteAsset(
    type: AssetType,
    moduleId: String,
    bundle: AssetBundle,
    idx: SortedSet<Int>?
  ): ServerAsset {
    return when (type) {
      // if it's a script, wrap it as a script
      AssetType.SCRIPT -> ServerAsset.Script(
        bundle.getScriptsOrThrow(moduleId),
        idx,
      )

      // if it's a stylesheet, wrap it as a stylesheet
      AssetType.STYLESHEET -> ServerAsset.Stylesheet(
        bundle.getStylesOrThrow(moduleId),
        idx,
      )

      // same with text
      AssetType.TEXT -> ServerAsset.Text(
        bundle.getGenericOrThrow(moduleId),
        idx,
      )

      else -> error("Unsupported asset type for pointer: '${type.name}'")
    }
  }

  /**
   * Look up any embedded server asset by the provided asset [tag], or return `null` to indicate that there was no
   * matching asset.
   *
   * @param tag Tag for the asset to resolve.
   * @param timeoutSeconds Max time to wait for the asset engine.
   * @return Resolved and interpreted asset, or `null`.
   */
  internal fun resolveByTag(tag: String, timeoutSeconds: Long = WAIT_TIMEOUT): ServerAsset? {
    if (!initialized.get()) {
      latch.await(timeoutSeconds, TimeUnit.SECONDS)
      if (!initialized.get()) {
        return null
      }
    }
    val manifest = activeManifest()
    return manifest.tagIndex[tag]?.let { asset ->
      // we've found an asset pointer, so we just need to wrap it with extra metadata before returning.
      val assetPayload = manifest.bundle.getAsset(asset)
      val pointer = manifest.moduleIndex[assetPayload.module]!!
      buildConcreteAsset(
        pointer.type,
        pointer.moduleId,
        manifest.bundle,
        pointer.index,
      )
    }
  }

  /**
   * Given a known-good asset [idx] for a content payload, read the asset and perform any transformations or other
   * relevant pre-requisite work before returning it to the invoking client.
   *
   * @param idx Index of the content payload implementing this module.
   * @return Rendered asset module, ready for serving decisions.
   */
  internal suspend fun readByModuleIndex(idx: Int): AssetContent {
    return activeManifest().bundle.getAsset(idx)!!
  }

  /**
   * Retrieve the timestamp indicating when the active asset bundle was generated; this is used as the last-modified
   * value when serving assets from the bundle.
   *
   * @return Generated timestamp value, in seconds, from the active asset bundle.
   */
  internal fun getBundleTimestamp(): Long {
    return activeManifest().bundle.generated.seconds
  }
}
