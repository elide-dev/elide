@file:Suppress("WildcardImport")

package dev.elide.buildtools.gradle.plugin.tasks

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.Encoder
import com.google.common.annotations.VisibleForTesting
import com.google.common.graph.ElementOrder
import com.google.common.graph.ImmutableNetwork
import com.google.common.graph.NetworkBuilder
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.*
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import tools.elide.assets.AssetBundle
import tools.elide.assets.AssetBundleKt.ScriptBundleKt.scriptAsset
import tools.elide.assets.AssetBundleKt.StyleBundleKt.styleAsset
import tools.elide.assets.AssetBundleKt.assetDependencies
import tools.elide.assets.AssetBundleKt.bundlerSettings
import tools.elide.assets.AssetBundleKt.digestSettings
import tools.elide.assets.AssetBundleKt.genericBundle
import tools.elide.assets.AssetBundleKt.scriptBundle
import tools.elide.assets.AssetBundleKt.styleBundle
import tools.elide.assets.ManifestFormat
import tools.elide.assets.assetBundle
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.*
import tools.elide.page.ContextKt.ScriptsKt.javaScript
import tools.elide.page.ContextKt.StylesKt.stylesheet
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.Stream
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import javax.inject.Inject
import kotlin.streams.toList

/** Task to interpret server-side asset configuration and content into a compiled asset bundle. */
@Suppress("UnstableApiUsage", "SameParameterValue", "LargeClass")
abstract class GenerateAssetGraphTask @Inject constructor(
    objects: ObjectFactory,
) : BundleBaseTask() {
    companion object {
        private const val BROWSER_DIST_DEFAULT = "assetDist"
        private const val GZIP_BUFFER_SIZE = 1024
        private const val BROTLI_LEVEL = 11
        private const val BROTLI_WINDOW = 24

        // Indicate whether Brotli can be loaded and used in this environment.
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        private fun brotliSupported(): Boolean {
            return try {
                Brotli4jLoader.isAvailable()
            } catch (thr: Throwable) {
                false
            }
        }

        /** Generate a trimmed digest which should be used as an asset's "tag". */
        @JvmStatic
        @VisibleForTesting
        internal fun generateAssetToken(
            info: AssetInfo,
            tagConfig: AssetTagConfig,
            digest: ByteArray,
        ): String {
            val (algorithm, _, rounds) = tagConfig
            val algo = if (algorithm == HashAlgorithm.IDENTITY) {
                HashAlgorithm.SHA256
            } else {
                algorithm
            }
            val preimage = buildString {
                append(info.module)
                append(algorithm.ordinal)
            }
            val preimageBytes = ByteArrayOutputStream()
            preimageBytes.use {
                it.write(preimage.toByteArray(StandardCharsets.UTF_8))
                it.write(digest)
            }
            var digester = algo.digester()!!
            digester.update(preimage.toByteArray())
            var digestTarget = digester.digest()

            if (rounds - 1 > 0) {
                (1 until rounds).forEach { _ ->
                    digester = algo.digester()!!
                    digestTarget = digester.digest(digestTarget)
                }
            }
            return digest.joinToString("") {
                java.lang.Byte.toUnsignedInt(it).toString(radix = 16).padStart(2, '0')
            }
        }
    }

    /** Module info payloads associated with each module ID. */
    @get:Input
    @get:Option(
        option = "assetModules",
        description = "Map of module IDs to their asset info configurations",
    )
    val assetModules: MapProperty<AssetModuleId, AssetInfo> = objects.mapProperty(
        AssetModuleId::class.java,
        AssetInfo::class.java,
    )

    /** Map of module source files to their owning asset info module IDs. */
    @get:Input
    @get:Option(
        option = "assetModuleMap",
        description = "Map of source file paths to the module IDs that own them",
    )
    val assetModuleMap: MapProperty<String, AssetModuleId> = objects.mapProperty(
        String::class.java,
        AssetModuleId::class.java,
    )

    /** Full set of all input source files to be considered as assets. */
    @get:InputFiles
    @get:Option(
        option = "inputFiles",
        description = "Transitive set of all asset source files which will be expressed in this graph",
    )
    abstract val inputFiles: Property<FileCollection>

    /** Algorithm to use when digesting assets. */
    @get:Input
    @get:Option(
        option = "digestAlgorithm",
        description = "Digest algorithm to use when calculating asset digests",
    )
    abstract val digestAlgorithm: Property<HashAlgorithm>

    /** Compression configuration to apply to bundled assets. */
    @get:Input
    @get:Option(
        option = "compressionConfig",
        description = "Compression configuration to apply to bundled assets",
    )
    abstract val compressionConfig: Property<AssetCompressionConfig>

    /** Name of the manifest that this task should write. */
    @get:Input
    @get:Option(
        option = "manifestName",
        description = "Name of the asset manifest to write.",
    )
    abstract val manifestName: Property<String>

    /** Manifest file that this task should write. */
    @get:OutputFile
    @get:Option(
        option = "manifestFile",
        description = "File where the manifest should be written.",
    )
    abstract val manifestFile: Property<File>

    /** Asset graph which will be built by this task. */
    @get:Internal
    internal val assetGraphBuilder: ImmutableNetwork.Builder<AssetModuleId, AssetDependency> = NetworkBuilder
        .directed()
        .allowsParallelEdges(false)
        .allowsSelfLoops(false)
        .nodeOrder(ElementOrder.stable<AssetModuleId>())
        .edgeOrder(ElementOrder.stable<AssetDependency>())
        .immutable()

    /** @inheritDoc */
    override fun runAction() {
        val inputs = inputFiles.get()
        val assetModules = assetModules.get()
        val elideExtension: ElideExtension = project.extensions.getByType(
            ElideExtension::class.java
        )

        project.logger.lifecycle(
            "Generating compiled graph for ${assetModules.size} asset modules " +
            "(comprising ${inputs.files.size} sources)"
        )

        // add all dependencies as graph nodes
        assetModules.keys.forEach {
            assetGraphBuilder.addNode(it)
        }

        // for each module, begin calculating a bundle component
        val hashAlgorithm = digestAlgorithm.get()
        val assetSpecs = buildAssetSpecMap(assetModules, hashAlgorithm)

        // finish building our dep graph, so we can retrieve the transitive closure of dependencies for each asset spec.
        val dependencyGraph = buildDependencyGraph(
            assetSpecs
        )
        project.logger.debug(
            "Generated asset graph. Building asset bundle partials by bundle"
        )

        // resolve static algorithm inputs
        val now = Instant.now()
        val hashRounds = StaticValues.defaultHashRounds
        val tailSize = StaticValues.defaultTailSize
        val baseBundle = assetBundle {
            version = StaticValues.currentVersion
            generated = Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
            settings = bundlerSettings {
                digestSettings = digestSettings {
                    algorithm = hashAlgorithm
                    rounds = hashRounds
                    tail = tailSize
                }
            }
        }.toBuilder()

        // process each builder into a partial asset bundle
        val tagGenConfig = elideExtension.server.assets.bundlerConfig.tagGenConfig()
        val collectedBuilders = buildDependencySortedAssetBundle(
            dependencyGraph,
            assetSpecs,
            tagGenConfig,
        )

        // with each finalized builder, update the global digester, and seal the bundle partial.
        val assetBundle = buildAssetDescriptor(
            hashAlgorithm,
            collectedBuilders,
            baseBundle,
            elideExtension,
        )

        // grab a base64-fingerprint of the bundle
        val algoName = hashAlgorithm.name
        val b64 = Base64.getEncoder().withoutPadding().encodeToString(
            assetBundle.digest.toByteArray()
        )
        project.logger.lifecycle(
            "Asset bundle assembled from all inputs: $algoName($b64)"
        )
        writeAssetBundle(
            assetBundle,
        )
    }

    @Suppress("USELESS_ELVIS")
    private fun writeAssetBundle(bundle: AssetBundle) {
        // ensure file is writable
        val targetFile = this.manifestFile.get() ?: throw IllegalStateException(
            "Failed to resolve target file for manifest output. This is an internal error; please " +
            "report it to the Elide build-tools team: https://github.com/elide-dev/buildtools/issues"
        )
        targetFile.outputStream().use { out ->
            when (val encoding = this.bundleEncoding.get()) {
                ManifestFormat.BINARY -> {
                    project.logger.debug("Writing asset bundle in proto-binary")
                    out.buffered().use {
                        bundle.writeTo(out)
                    }
                }

                // text bundles share a code path
                ManifestFormat.JSON, ManifestFormat.TEXT -> writeTextBundle(
                    out,
                    bundle,
                    encoding,
                )
                else -> error(
                    "Unrecognized bundle encoding: $${encoding.name}"
                )
            }
        }
    }

    private fun writeTextBundle(out: OutputStream, bundle: AssetBundle, encoding: ManifestFormat) {
        out.bufferedWriter(StandardCharsets.UTF_8).use { textBuf ->
            if (encoding == ManifestFormat.JSON) {
                project.logger.debug("Writing asset bundle in proto-JSON")
                jsonPrinter.appendTo(
                    bundle,
                    textBuf,
                )
            } else {
                project.logger.debug("Writing asset bundle in proto-text")
                textBuf.write(
                    bundle.toString()
                )
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun buildDependencyGraph(
        assetSpecs: Map<AssetModuleId, AssetContent>
    ): ImmutableNetwork<AssetModuleId, AssetDependency> {
        // add each asset spec to the module graph, considering direct dependencies for each module as we go. the stream
        // is already sorted by module ID; we preserve that by remaining in serial mode.
        //
        // after ensuring each node is added to the graph, pivot to adding edges.
        assetSpecs.entries.stream().flatMap { asset ->
            asset.value.assetInfo.directDeps.stream().map {
                asset.value.assetInfo.module to it
            }
        }.forEach {
            val (depSource, depTarget) = it
            assetGraphBuilder.addEdge(
                depSource,
                depTarget,
                AssetDependency(
                    dependent = depSource,
                    dependee = depTarget,
                    devOnly = false,
                    direct = true,
                )
            )
        }
        return this.assetGraphBuilder.build()
    }

    @Suppress("LongMethod")
    private fun buildAssetSpecMap(
        assetModules: Map<AssetModuleId, AssetInfo>,
        hashAlgorithm: HashAlgorithm,
    ): Map<AssetModuleId, AssetContent> {
        return assetModules.entries.stream().parallel().map { entry ->
            val moduleId = entry.key
            val assetInfo = entry.value
            val paths = assetInfo.paths
            val depPaths = assetInfo.projectDeps

            // resolve files for each project dependency
            val resolvedProjectDeps = depPaths.map {
                it.projectPath.get() to (it.sourceConfiguration.get() ?: BROWSER_DIST_DEFAULT)
            }.mapNotNull {
                val (projectTarget, configSource) = it
                if (projectTarget == null) {
                    null
                } else {
                    val project = project.findProject(projectTarget)
                    if (project == null) {
                        null
                    } else {
                        project to configSource
                    }
                }
            }.mapNotNull {
                val (_, sourceConfig) = it
                project.configurations.findByName(sourceConfig)
            }.flatMap {
                it.resolve()
            }.flatMap {
                require(it.exists()) {
                    "Project dependency mapping '${it.path}' (for module '$moduleId') does not exist"
                }
                if (it.isDirectory) {
                    it.listFiles()?.toList() ?: emptyList()
                } else {
                    listOf(it)
                }
            }

            val staticFiles = paths.stream().parallel().map {
                val file = project.file(it)
                require(file.exists()) {
                    "Failed to resolve bundled asset file: '$it'"
                }
                project.logger.debug(
                    "Processing asset for module ID '$moduleId': ${file.path}"
                )
                file
            }
            val allFiles = Stream.concat(
                staticFiles,
                resolvedProjectDeps.stream(),
            )
            val allResolvedFiles = allFiles.parallel().map { file ->
                val fileBytes = file.inputStream().buffered().use { buf ->
                    buf.readAllBytes()
                }
                val digester = digestAlgorithm.get().digester()
                val digestData = if (digester != null) {
                    digester.digest(
                        fileBytes
                    )
                } else {
                    ByteArray(0)
                }
                AssetContent.AssetFile(
                    filename = file.name,
                    base = file.parentFile.absolutePath,
                    size = fileBytes.size.toLong(),
                    digestAlgorithm = hashAlgorithm,
                    digest = digestData,
                    content = fileBytes,
                    file = file,
                )
            }.collect(
                Collectors.toCollection {
                    ConcurrentLinkedQueue()
                }
            )
            AssetContent(
                assetInfo = assetInfo,
                assets = allResolvedFiles,
            )
        }.sorted { left, right ->
            left.assetInfo.module.compareTo(
                right.assetInfo.module
            )
        }.collect(
            Collectors.toMap({ it.assetInfo.module }, { it }, { _, _ ->
                error("Duplicate module ID")
            }, { ConcurrentSkipListMap() })
        )
    }

    @Suppress("LongMethod")
    private fun buildDependencySortedAssetBundle(
        dependencyGraph: ImmutableNetwork<AssetModuleId, AssetDependency>,
        assetSpecs: Map<AssetModuleId, AssetContent>,
        tagConfig: AssetTagConfig,
    ): List<Pair<AssetBundle.Builder, AssetContent>> {
        return TopologicalGraphIterator.map(dependencyGraph.asGraph()) { moduleId ->
            // resolve module content
            val moduleContent = assetSpecs[moduleId] ?: error(
                "Failed to resolve known-good module at ID '$moduleId'"
            )

            // resolve module direct deps
            val sources = moduleContent.assets
            val directDeps = moduleContent.assetInfo.directDeps
            val transitiveDeps = emptySet<AssetModuleId>()

            project.logger.info(
                "Processing asset module: $moduleId" +
                "(deps: '${moduleContent.assetInfo.directDeps.joinToString(", ")}')"
            )

            // generate asset proto
            val partial = AssetBundle.newBuilder()
            when (moduleContent.assetInfo.type) {
                // add the module as a script asset
                AssetType.SCRIPT -> {
                    val bundle = scriptBundle {
                        module = moduleId
                        sources.forEach {
                            val token = generateAssetToken(
                                moduleContent.assetInfo,
                                tagConfig,
                                it.digest,
                            )
                            val content = it.toProto(
                                moduleId,
                                token,
                            )
                            partial.addAsset(content)
                            asset.add(
                                scriptAsset {
                                    this.filename = content.filename
                                    this.token = token
                                    this.script = javaScript {
                                        // @TODO(sgammon): script injection customization from build script or server?
                                    }
                                }
                            )
                        }
                        dependencies = assetDependencies {
                            direct.addAll(directDeps)
                            transitive.addAll(transitiveDeps)
                        }
                    }
                    partial.putScripts(
                        moduleId,
                        bundle,
                    )
                }

                // add the module as a stylesheet asset
                AssetType.STYLESHEET -> {
                    val bundle = styleBundle {
                        module = moduleId
                        sources.forEach {
                            val token = generateAssetToken(
                                moduleContent.assetInfo,
                                tagConfig,
                                it.digest,
                            )
                            val content = it.toProto(
                                moduleId,
                                token,
                            )
                            partial.addAsset(content)
                            asset.add(
                                styleAsset {
                                    this.filename = content.filename
                                    this.token = token
                                    this.stylesheet = stylesheet {
                                        // @TODO(sgammon): sheet injection customization from build script or server?
                                    }
                                }
                            )
                        }
                        dependencies = assetDependencies {
                            direct.addAll(directDeps)
                            transitive.addAll(transitiveDeps)
                        }
                    }
                    partial.putStyles(
                        moduleId,
                        bundle,
                    )
                }

                // add the module as a generic text asset
                AssetType.TEXT -> {
                    val bundle = genericBundle {
                        module = moduleId
                        sources.forEach {
                            val token = generateAssetToken(
                                moduleContent.assetInfo,
                                tagConfig,
                                it.digest,
                            )
                            val content = it.toProto(
                                moduleId,
                                token,
                            )
                            partial.addAsset(content)
                            this.filename = content.filename
                            this.token = token
                        }
                    }
                    partial.putGeneric(
                        moduleId,
                        bundle,
                    )
                }
            }
            partial to moduleContent
        }.toList()
    }

    @Suppress("USELESS_ELVIS")
    private fun buildAssetDescriptor(
        hashAlgorithm: HashAlgorithm,
        collectedBuilders: List<Pair<AssetBundle.Builder, AssetContent>>,
        baseBundle: AssetBundle.Builder,
        extension: ElideExtension,
    ): AssetBundle {
        // process each builder into a finalized asset
        val globalDigester = (digestAlgorithm.get() ?: HashAlgorithm.SHA256).digester()!!

        // enriched variants with compression, etc
        val finalizedBuilder = collectedBuilders.stream().parallel().map {
            it.second to buildDescriptorVariants(
                hashAlgorithm,
                it.first,
                it.second,
                extension,
            )
        }.collect(Collectors.toList())

        // with each finalized builder, update the global digester, and seal the bundle partial.
        return finalizedBuilder.stream().map {
            val (content, builder) = it
            content.assets.forEach { sourceFile ->
                globalDigester.update(sourceFile.digest)
            }
            builder
        }.reduce(baseBundle) { acc, partial ->
            acc.mergeFrom(partial.buildPartial())
            acc
        }.setDigest(
            ByteString.copyFrom(globalDigester.digest())
        ).build()
    }

    @Suppress("UNUSED_PARAMETER", "LongMethod", "ComplexMethod")
    private fun buildDescriptorVariants(
        hashAlgorithm: HashAlgorithm,
        builder: AssetBundle.Builder,
        content: AssetContent,
        extension: ElideExtension,
    ): AssetBundle.Builder {
        val resolvedConfig = compressionConfig.get()
        val modes = resolvedConfig.modes

        return if (modes.isEmpty() || (modes.size == 1 && modes.first() == CompressionMode.IDENTITY)) {
            // compression is disabled
            builder
        } else {
            require(builder.assetCount > 0) {
                "Cannot build descriptor with no asset payloads"
            }

            // for each indexed asset in the set, split off a job which then splits off for each configured compression
            // mode. each job generates its own variant from the read-only `IDENTITY` data already present.
            IntStream.of(builder.assetCount - 1).parallel().mapToObj { idx ->
                // map the index entry to the extracted asset payload.
                val entry = builder.getAsset(idx)
                idx to entry
            }.flatMap { assetSpec ->
                // flat-map to split across the set of configured compression modes for each asset. we are careful here
                // to maintain the index because we will need it when we are ready to write.
                modes.stream().map { it to assetSpec }
            }.map {
                // unpack the double tuples, and run the compression job. once we're done, built a variant out of it,
                // and assign it directly into the builder at the held index.
                val (mode, target) = it
                val (assetIdx, assetData) = target
                val rawContent = pluckRawContent(assetData)

                project.logger.debug(
                    "Compressing asset '${assetData.module}' with mode '${mode.name}'"
                )

                // check the raw content length. if it's under the minimum threshold, we can skip it (unless we are
                // instructed to keep all variants). if the minimum length is `0` or below, we don't need to do any
                // checks, because there is no minimum value set.
                if (resolvedConfig.minimumSize > 0 && (
                        resolvedConfig.minimumSize > rawContent.size && !resolvedConfig.force
                    )
                ) {
                    // the asset is too small, and we have not been instructed to keep it. so we can safely skip it here
                    // by returning `null` for the data position.
                    project.logger.debug(
                        "Asset '${assetData.module}' was too small (${rawContent.size} bytes, minimum is " +
                        "${resolvedConfig.minimumSize} bytes) to compress. Skipping."
                    )
                    return@map assetIdx to (mode to null)
                } else if (resolvedConfig.force) {
                    // the asset is too small, but we have been instructed to keep it anyway.
                    project.logger.debug(
                        "Asset '${assetData.module}' failed to meet minimum size threshold " +
                        "(${rawContent.size} bytes, minimum is ${resolvedConfig.minimumSize} bytes) to compress. " +
                        "Force mode is `true`, so the asset was included anyway."
                    )
                }

                // compress the data using the specified compression mode.
                assetIdx to (
                    mode to compressAssetData(
                        mode,
                        rawContent,
                    )
                )
            }.filter {
                // if the compressed output data is null, it means the compression mode in question is not supported or
                // could not be loaded; that condition logs its own warning.
                it.second.second?.second != null
            }.map {
                val (assetIdx, compressedPayload) = it
                val (mode, compressed) = compressedPayload
                val (compressedLength, compressedData) = compressed!!

                // build a digest of the compressed data, which we also include with the variant payload. the digest and
                // size can be checked at runtime to enforce data integrity guarantees.
                val digester = hashAlgorithm.digester()
                val digest = if (digester != null) {
                    digester.digest(compressedData)
                } else {
                    ByteArray(0)
                }

                // build the variant. we attach the compressed data, compressed data size, and the fingerprint, along
                // with the has algorithm that produced it.
                assetIdx to compressedData {
                    compression = mode
                    size = compressedLength.toLong()
                    data = dataContainer {
                        raw = ByteString.copyFrom(compressedData)
                    }
                    integrity.add(
                        dataFingerprint {
                            hash = hashAlgorithm
                            fingerprint = ByteString.copyFrom(digest)
                        }
                    )
                }
            }.collect(Collectors.toMap({ it.first }, {
                // sort inner set by compression mode to make sure the output of this method remains deterministic.
                // outer map will already be sorted by index.
                val wrapset = ConcurrentSkipListSet<CompressedData> { left, right ->
                    left.compression.compareTo(right.compression)
                }
                wrapset.add(it.second)
                wrapset
            }, { leftSet, rightSet ->
                // this happens naturally for two variants for the same asset. we merge them into a fresh set, ordered
                // by compression mode -- unless we expect to make decisions based on the size of each compressed data
                // variant, in which case it is ordered by size.
                val wrapset = ConcurrentSkipListSet<CompressedData> { left, right ->
                    left.size.compareTo(right.size)
                }
                wrapset.addAll(leftSet)
                wrapset.addAll(rightSet)

                if (resolvedConfig.keepBest && !resolvedConfig.force) {
                    // we are supposed to only keep the *best* of this merged set of assets, and we are not being asked
                    // to force-hold all of them. so, at this time, we'll filter out all but the best variant, returning
                    // this set to a single-item collection -- but, with ordering re-set to operate on the mode.
                    val resorted = ConcurrentSkipListSet<CompressedData> { left, right ->
                        left.compression.compareTo(right.compression)
                    }
                    val bestVariant = wrapset.first()
                    project.logger.debug(
                        "Best compressed variant is size=(${bestVariant.size}), using " +
                        "mode=(${bestVariant.compression.name}). Keeping and ejecting " +
                        "${wrapset.size - 1} other variants."
                    )
                    resorted.add(bestVariant)
                    resorted
                } else if (resolvedConfig.force) {
                    // we are supposed to only keep the *best* of this merged set of assets, but we have *also* been
                    // instructed to force-keep all variants. so we do the measurement anyway, log about it, and keep
                    // it without taking any other action.
                    val bestVariant = wrapset.first()
                    project.logger.debug(
                    "Best compressed variant is size=(${bestVariant.size}), using " +
                        "mode=(${bestVariant.compression.name}). However, we are configured to keep all variants, " +
                        "so this one will be kept."
                    )
                    wrapset
                } else {
                    // `keepBest` is false, rendering `force` irrelevant: we keep all variants by simply merging the
                    // sorted set and returning it.
                    wrapset
                }
            }, { ConcurrentSkipListMap() })).entries.stream().flatMap { variantEntry ->
                // here we have converted the stream into an ordered and serial set of variants, so that we can safely
                // mount them into the subject builder. the stream has to be ordered, so it operates deterministically,
                // because we will eventually serialize and write this output.
                variantEntry.value.stream().map {
                    variantEntry.key to it
                }
            }.forEachOrdered { variantEntry ->
                // unpack the original asset index for this variant, and the variant data.
                val (assetIdx, variant) = variantEntry

                // add the variant to the set of variants for the asset.
                val asset = builder.getAssetBuilder(
                    assetIdx
                )

                // at this time, when `keepBest` is active, we need to do one more check against the size of the
                // compressed variant. if it doesn't  beat the `IDENTITY` variant, and `keepBest` is `true`, we skip
                // adding it altogether because it failed  to save space over the wire compared to the asset itself.
                if (resolvedConfig.keepBest) {
                    val rawContent = pluckRawContent(asset)
                    if (variant.size > rawContent.size && !resolvedConfig.force) {
                        // the variant didn't save space, and `force` is off, while `keepBest` is active. drop it.
                        project.logger.debug(
                            "Asset '${asset.module}' compressed variant size=(${variant.size}) " +
                            "is less than uncompressed size=(${rawContent.size}). Skipping variant."
                        )
                        return@forEachOrdered
                    } else if (resolvedConfig.force) {
                        // the variant didn't save space, and `force` is on, while `keepBest` is active. keep it.
                        project.logger.debug(
                            "Asset '${asset.module}' compressed variant size=(${variant.size}) " +
                            "is less than uncompressed size=(${rawContent.size}), but `force` mode is active. " +
                            "Keeping variant, but it would have been dropped."
                        )
                    }
                    asset.addVariant(
                        variant
                    )
                } else {
                    // `keepBest` is off, rendering `force` inert. add variant unconditionally.
                    asset.addVariant(variant)
                }
            }
            builder
        }
    }

    private fun pluckRawContent(assetData: AssetBundle.AssetContentOrBuilder): ByteArray {
        // resolve content data to compress. because `IDENTITY` is the default and always included, we can force
        // resolve it here safely.
        return (assetData.variantList.find { candidate ->
            candidate.compression == CompressionMode.IDENTITY
        } ?: error(
            "Failed to resolve payload of type `IDENTITY`."
        )).data.raw.toByteArray()
    }

    private fun compressAssetData(mode: CompressionMode, data: ByteArray): Pair<Int, ByteArray>? = when {
        // `IDENTITY` mode uses no compression.
        mode == CompressionMode.IDENTITY -> data.size to data

        mode == CompressionMode.GZIP -> {
            val baos = ByteArrayOutputStream()
            val gzipOut = OptimizedGzipOutputStream(baos)
            gzipOut.use { compressor ->
                compressor.write(data)
            }
            val compressed = baos.toByteArray()
            compressed.size to compressed
        }

        mode == CompressionMode.DEFLATE -> {
            val baos = ByteArrayOutputStream()
            val deflater = Deflater(Deflater.BEST_COMPRESSION)
            val gzipOut = DeflaterOutputStream(baos, deflater, GZIP_BUFFER_SIZE)
            gzipOut.use { compressor ->
                compressor.write(data)
            }
            val compressed = baos.toByteArray()
            compressed.size to compressed
        }

        brotliSupported() && mode == CompressionMode.BROTLI -> {
            val encoderParams = Encoder.Parameters()
            encoderParams.setQuality(BROTLI_LEVEL)
            encoderParams.setWindow(BROTLI_WINDOW)
            val compressed = Encoder.compress(data, encoderParams)
            compressed.size to compressed
        }

        else -> {
            project.logger.debug(
                "Compression mode is not supported in this environment: '${mode.name}'. Skipping variant."
            )
            null
        }
    }

    @Suppress("MagicNumber")
    class OptimizedGzipOutputStream(delegate: OutputStream) : DeflaterOutputStream(
        delegate,
        Deflater(Deflater.BEST_COMPRESSION, true),
        GZIP_BUFFER_SIZE,
        true,
    ) {
        companion object {
            /*
             * GZIP header magic number.
             */
            private const val GZIP_MAGIC = 0x8b1f

            /*
             * Trailer size in bytes.
             *
             */
            private const val TRAILER_SIZE = 8
        }

        /**
         * CRC-32 of uncompressed data.
         */
        private var crc = CRC32()

        init {
            writeHeader()
            crc.reset()
        }

        /** Writes GZIP member header. */
        @Throws(IOException::class)
        private fun writeHeader() {
            out.write(
                byteArrayOf(
                    GZIP_MAGIC.toByte(),
                    (GZIP_MAGIC shr 8).toByte(), // Magic number (short)
                    Deflater.DEFLATED.toByte(), // Compression method (CM)
                    0, // Flags (FLG)
                    0, // Modification time MTIME (int)
                    0, // Modification time MTIME (int)
                    0, // Modification time MTIME (int)
                    0, // Modification time MTIME (int)
                    0, // Extra flags
                    0 // Operating system (OS)
                )
            )
        }

        /*
         * Writes GZIP member trailer to a byte array, starting at a given
         * offset.
         */
        @Throws(IOException::class)
        private fun writeTrailer(buf: ByteArray, offset: Int) {
            writeInt(crc.value.toInt(), buf, offset) // CRC-32 of uncompressed data
            writeInt(def.totalIn, buf, offset + 4) // Number of uncompressed bytes
        }

        /*
         * Writes integer in Intel byte order to a byte array, starting at a
         * given offset.
         */
        @Throws(IOException::class)
        private fun writeInt(i: Int, buf: ByteArray, offset: Int) {
            writeShort(i and 0xffff, buf, offset)
            writeShort(i shr 16 and 0xffff, buf, offset + 2)
        }

        /*
         * Writes short integer in Intel byte order to a byte array, starting
         * at a given offset
         */
        @Throws(IOException::class)
        private fun writeShort(s: Int, buf: ByteArray, offset: Int) {
            buf[offset] = (s and 0xff).toByte()
            buf[offset + 1] = (s shr 8 and 0xff).toByte()
        }

        /**
         * Writes array of bytes to the compressed output stream. This method
         * will block until all the bytes are written.
         * @param buf the data to be written
         * @param off the start offset of the data
         * @param len the length of the data
         * @exception IOException If an I/O error has occurred.
         */
        @Synchronized
        @Throws(IOException::class)
        override fun write(buf: ByteArray, off: Int, len: Int) {
            super.write(buf, off, len)
            crc.update(buf, off, len)
        }

        /**
         * Finishes writing compressed data to the output stream without closing
         * the underlying stream. Use this method when applying multiple filters
         * in succession to the same output stream.
         * @exception IOException if an I/O error has occurred
         */
        @Throws(IOException::class)
        override fun finish() {
            if (!def.finished()) {
                def.finish()
                while (!def.finished()) {
                    var len = def.deflate(buf, 0, buf.size)
                    if (def.finished() && len <= buf.size - TRAILER_SIZE) {
                        // last deflater buffer. Fit trailer at the end
                        writeTrailer(buf, len)
                        len += TRAILER_SIZE
                        out.write(buf, 0, len)
                        return
                    }
                    if (len > 0) out.write(buf, 0, len)
                }
                // if we can't fit the trailer at the end of the last
                // deflater buffer, we write it separately
                val trailer = ByteArray(TRAILER_SIZE)
                writeTrailer(trailer, 0)
                out.write(trailer)
            }
        }
    }
}
