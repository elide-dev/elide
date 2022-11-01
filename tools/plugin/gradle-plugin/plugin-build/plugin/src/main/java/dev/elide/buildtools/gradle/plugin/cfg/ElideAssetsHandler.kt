package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.model.ObjectFactory
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm
import tools.elide.data.CompressionMode
import java.io.File
import java.util.EnumSet
import java.util.SortedSet
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Configuration for server-embedded assets. */
@Suppress("RedundantVisibilityModifier", "MemberVisibilityCanBePrivate", "unused")
open class ElideAssetsHandler @Inject constructor(
    private val objects: ObjectFactory
) {
    companion object {
        val defaultManifestFormat = StaticValues.defaultEncoding
        val defaultHashAlgorithm = StaticValues.assetHashAlgo
        const val defaultMinimumUncompressedSize: Int = 400
        const val BROWSER_DIST_TARGET = "assetDist"
    }

    /** Whether the user has assets configured for embedding. */
    internal val active: AtomicBoolean = AtomicBoolean(false)

    /** Map of assets configured by the user for server embedding. */
    internal val assets: MutableMap<String, ServerAsset> = ConcurrentSkipListMap<String, ServerAsset>()

    /** Bundler configuration. */
    internal val bundlerConfig: AssetBundlerConfig = objects.newInstance(AssetBundlerConfig::class.java)

    /** Add a script asset with the specified [module] ID using the provided [block]. */
    fun script(module: String, block: Action<ServerAsset>) {
        asset(
            AssetType.SCRIPT,
            module,
            block,
        )
    }

    /** Add a stylesheet asset with the specified [module] ID using the provided [block]. */
    fun stylesheet(module: String, block: Action<ServerAsset>) {
        asset(
            AssetType.STYLESHEET,
            module,
            block,
        )
    }

    /** Add a text asset with the specified [module] ID using the provided [block]. */
    fun text(module: String, block: Action<ServerAsset>) {
        asset(
            AssetType.TEXT,
            module,
            block,
        )
    }

    /** Add a generic asset of the specified [type] using the provided [block]. */
    fun asset(type: AssetType, module: String, block: Action<ServerAsset>) {
        val asset = ServerAsset(objects, module, type)
        block.execute(asset)
        require(!assets.containsKey(module)) {
            "Asset module '$module' already configured"
        }
        assets[module] = asset
        active.set(true)
    }

    /** Configuration for the asset bundler task. */
    fun bundler(block: Action<AssetBundlerConfig>) {
        block.execute(bundlerConfig)
    }

    /** @return `true` if inter-project dependencies have been declared; `false` otherwise. */
    fun hasAnyProjectDeps(): Boolean {
        return assets.values.any { asset ->
            asset.projectDeps.get().isNotEmpty()
        }
    }

    /** @return Rolled-up set of inter-project dependencies. */
    fun getAllProjectDeps(): List<Pair<String, String>> {
        return assets.values.flatMap { asset ->
            asset.projectDeps.get().mapNotNull { handler ->
                val projectTarget = handler.projectPath.get()
                val configTarget = handler.targetConfiguration.get()
                if (projectTarget != null && configTarget != null) {
                    Pair(projectTarget, configTarget)
                } else {
                    null
                }
            }
        }
    }

    /** Extends a standard Gradle copy-task-spec with asset type information, so that sensible defaults can be used. */
    open class AssetCopySpec @Inject constructor (
        private val project: Project,
        internal val type: AssetType,
        private val objects: ObjectFactory,
    ) : CopySpec by project.copySpec()

    /**
     * Server-side asset configuration.
     *
     * @param objects Factory to create fresh objects via Gradle.
     * @param module Name of the module representing this server asset.
     * @param type Type of server asset.
     */
    @Suppress("TooManyFunctions")
    open class ServerAsset(
        private val objects: ObjectFactory,
        internal val module: String,
        internal val type: AssetType
    ) {
        /** Specification to copy target files from a given location. */
        internal val copySpec: AtomicReference<AssetCopySpec> = AtomicReference(
            objects.newInstance(AssetCopySpec::class.java, type)
        )

        /** Direct dependencies to consider for this asset. */
        internal val directDeps: AtomicReference<SortedSet<String>> = AtomicReference(TreeSet())

        /** Project-provided dependency declarations. */
        internal val projectDeps: AtomicReference<MutableList<InterProjectAssetHandler>> = AtomicReference(ArrayList())

        /** Source file paths for this asset module. */
        internal val filePaths: AtomicReference<SortedSet<String>> = AtomicReference(TreeSet())

        /** Copy assets from a specific location on-disk. */
        private fun copy(block: Action<AssetCopySpec>) {
            block.execute(copySpec.get())
        }

        /**
         * Pull an asset from somewhere else in a Gradle project where the plugin is equipped.
         *
         * @param project Path to the project containing the asset targets.
         * @param configuration Optionally, the name of a configuration to pull from. Defaults to `browserDist`.
         */
        public fun fromProject(project: String, configuration: String = BROWSER_DIST_TARGET) {
            projectDeps.get().add(
                InterProjectAssetHandler.fromProject(
                    objects,
                    project,
                    configuration,
                )
            )
        }

        /**
         * Pull an asset from somewhere else in a Gradle project where the plugin is equipped.
         *
         * @param project Path to the project containing the asset targets.
         * @param configuration Optionally, the name of a configuration to pull from. Defaults to `browserDist`.
         */
        public fun fromProject(project: Project, configuration: String = BROWSER_DIST_TARGET) {
            projectDeps.get().add(
                InterProjectAssetHandler.fromProject(
                    objects,
                    project.path,
                    configuration,
                )
            )
        }

        /**
         * Pull an asset from somewhere else in a Gradle project where the plugin is equipped; in this case, using a
         * declared configuration.
         *
         * @param configuration Configuration to pull the assets from.
         * @param project Project to pull from.
         */
        public fun from(configuration: Configuration, project: Project) {
            projectDeps.get().add(
                InterProjectAssetHandler.fromProject(
                    objects,
                    project.path,
                    configuration.name,
                )
            )
        }

        /**
         * Pull an asset from somewhere else in a Gradle project where the plugin is equipped; in this case, using a
         * managed configuration.
         *
         * @param project Project to pull from.
         */
        public fun from(project: Project) {
            projectDeps.get().add(
                InterProjectAssetHandler.fromProject(
                    objects,
                    project.path,
                    BROWSER_DIST_TARGET,
                )
            )
        }

        /**
         * Pull an asset from somewhere else in a Gradle project where the plugin is equipped; in this case, using a
         * [block] to configure the dependency mapping.
         *
         * @param block Block to execute to configure this mapping.
         */
        public fun from(block: Action<InterProjectAssetHandler>) {
            val handler = objects.newInstance(InterProjectAssetHandler::class.java)
            block.execute(handler)
            projectDeps.get().add(handler)
        }

        /** Shorthand to declare a single source file copy spec. */
        public fun sourceFile(path: String) {
            copy {
                val fileTarget = File(path)
                it.from(fileTarget.parentFile.path) { copySpec ->
                    copySpec.include(fileTarget.name)
                }
            }
            filePaths.set(sortedSetOf(path))
        }

        /** Shorthand to declare a multi-file source copy spec. */
        public fun sourceFiles(vararg path: String) {
            copy {
                it.from(*path)
            }
            filePaths.set(path.toSortedSet())
        }

        /** Indicate dependencies for this asset. */
        public fun dependsOn(vararg deps: String) {
            deps.forEach {
                if (it.startsWith(":")) {
                    projectDeps.get().add(
                        InterProjectAssetHandler.fromProject(
                            objects,
                            it,
                            BROWSER_DIST_TARGET,
                        )
                    )
                } else {
                    dependsOnAsset(it)
                }
            }
        }

        /** Indicate embedded asset dependencies for this asset. */
        public fun dependsOnAsset(block: Action<MutableSet<String>>) {
            val deps = TreeSet<String>()
            block.execute(deps)
            directDeps.set(deps)
        }

        /** Indicate dependencies for this asset. */
        public fun dependsOnAsset(vararg deps: String) {
            dependsOnAsset {
                it.addAll(deps)
            }
        }
    }

    /** Handler for configuring an inter-project asset dependency. */
    open class InterProjectAssetHandler : java.io.Serializable {
        internal val projectPath: AtomicReference<String?> = AtomicReference(null)
        internal val internalConfiguration: AtomicReference<String?> = AtomicReference(null)
        internal val targetConfiguration: AtomicReference<String?> = AtomicReference(null)
        internal val sourceConfiguration: AtomicReference<String?> = AtomicReference(null)
        internal val managedConfiguration: AtomicBoolean = AtomicBoolean(true)
        internal val includePaths: AtomicReference<SortedSet<String>> = AtomicReference(TreeSet())

        companion object {
            @JvmStatic internal fun fromProject(
                objects: ObjectFactory,
                project: String,
                configuration: String? = null
            ): InterProjectAssetHandler {
                val handler = objects.newInstance(InterProjectAssetHandler::class.java)
                handler.project(project, configuration)
                return handler
            }
        }

        /** Set the project which should be used for this target dependency. */
        public fun project(path: String, configuration: String? = null) {
            projectPath.set(path)
            if (configuration != null) {
                this.configuration(configuration)
            }
        }

        /** Set the project which should be used for this target dependency. */
        public fun project(project: Project, configuration: String? = null) {
            projectPath.set(project.path)
            if (configuration != null) {
                this.configuration(configuration)
            }
        }

        /** Set the project which should be used for this target dependency. */
        public fun project(configuration: Configuration, project: String) {
            projectPath.set(project)
            this.configuration(configuration.name)
        }

        /** Set the name of the target-side configuration to use. */
        public fun configuration(configuration: String) {
            targetConfiguration.set(configuration)
        }

        /** Set an explicit consumer configuration. This will disable managed configurations for this target. */
        public fun consumer(configuration: Configuration) {
            managedConfiguration.set(false)
            sourceConfiguration.set(configuration.name)
        }

        /** Set included paths for the copy operation between modules. Not usually necessary. */
        public fun include(vararg paths: String) {
            includePaths.get().addAll(paths)
        }
    }

    /** Asset bundler configuration. */
    open class AssetBundlerConfig @Inject constructor (objects: ObjectFactory) {
        internal val digestAlgorithm: AtomicReference<HashAlgorithm> = AtomicReference(defaultHashAlgorithm)
        internal val format: AtomicReference<ManifestFormat> = AtomicReference(defaultManifestFormat)
        internal val compressionConfig: CompressionHandler = objects.newInstance(CompressionHandler::class.java)
        internal val tagGenerator: AssetTagHandler = objects.newInstance(AssetTagHandler::class.java)

        /** @return Packaged compression config, based on the contents of this handler. */
        fun compressionConfig(): AssetCompressionConfig = compressionConfig.generateConfig()

        /** @return Packaged configuration for the asset tag generator. */
        fun tagGenConfig(): AssetTagConfig = tagGenerator.generateConfig()

        /** Set the hash algorithm to use for asset digests. */
        public fun digestAlgorithm(algorithm: HashAlgorithm) {
            digestAlgorithm.set(algorithm)
        }

        /** Set the encoded format of the asset bundle manifest. */
        public fun format(format: ManifestFormat) {
            this.format.set(format)
        }

        /** Set whether embedded assets should be compressed. */
        public fun compression(block: Action<CompressionHandler>) {
            block.execute(compressionConfig)
        }

        /** Apply settings to the asset filename tag generator. */
        public fun tagGenerator(block: Action<AssetTagHandler>) {
            block.execute(tagGenerator)
        }
    }

    /** Configuration specific to the asset tag generator. */
    open class AssetTagHandler {
        internal val digestAlgorithm: AtomicReference<HashAlgorithm> = AtomicReference(defaultHashAlgorithm)
        internal val tailSize: AtomicInteger = AtomicInteger(defaultMinimumUncompressedSize)
        internal val rounds: AtomicInteger = AtomicInteger(defaultMinimumUncompressedSize)

        /** @return Packaged tag generator config. */
        internal fun generateConfig(): AssetTagConfig = AssetTagConfig(
            hashAlgorithm = digestAlgorithm.get(),
            tailSize = tailSize.get(),
            rounds = rounds.get(),
        )

        /** Set the hash algorithm to use for asset digests. */
        public fun digestAlgorithm(algorithm: HashAlgorithm) {
            digestAlgorithm.set(algorithm)
        }

        /** Set the size, in count of pre-image bytes, to extract from the end of the file digest to form the tag. */
        public fun tailSize(size: Int) {
            tailSize.set(size)
        }

        /** Number of hash rounds to perform against the filename to generate the tag. Should generally be left at 1. */
        public fun rounds(count: Int) {
            rounds.set(count)
        }
    }

    /** Asset pre-compression configuration. */
    open class CompressionHandler {
        internal val enableCompression: AtomicBoolean = AtomicBoolean(true)
        internal val minimumUncompressedSize: AtomicInteger = AtomicInteger(defaultMinimumUncompressedSize)
        internal val keepOnlyBest: AtomicBoolean = AtomicBoolean(false)
        internal val forceKeepAll: AtomicBoolean = AtomicBoolean(false)
        internal val compressionAlgorithms: AtomicReference<EnumSet<CompressionMode>> = AtomicReference(EnumSet.of(
            // `IDENTITY` is implied
            CompressionMode.GZIP,
            CompressionMode.DEFLATE,
            CompressionMode.BROTLI,
        ))

        /** @return Packaged compression config, based on the contents of this handler. */
        internal fun generateConfig(): AssetCompressionConfig = AssetCompressionConfig(
            enabled = enableCompression.get(),
            modes = compressionAlgorithms.get(),
            minimumSize = minimumUncompressedSize.get(),
            keepBest = keepOnlyBest.get(),
            force = forceKeepAll.get(),
        )

        /** Set the minimum size for sources to be eligible for compression. Expressed in bytes. */
        public fun minimumSizeBytes(size: Int) {
            minimumUncompressedSize.set(size)
        }

        /** Sets a flag to keep only the best assets (by size), and discard all others. */
        public fun keepOnlyBest() {
            keepOnlyBest.set(true)
        }

        /** Indicates that all variants should be kept, regardless of size comparisons. */
        public fun keepAllVariants() {
            keepOnlyBest.set(false)
        }

        /** Sets a flag to force-keep all assets and ignore minimum sizes. Wins over all other relevant flags. */
        public fun forceVariants() {
            forceKeepAll.set(true)
        }

        /** Disable all compression. */
        public fun disable() {
            enableCompression.set(false)
        }

        /** Set whether embedded assets should be compressed. */
        public fun modes(vararg modes: CompressionMode) {
            enableCompression.set(true)
            compressionAlgorithms.set(EnumSet.copyOf(modes.toList()))
        }
    }
}
