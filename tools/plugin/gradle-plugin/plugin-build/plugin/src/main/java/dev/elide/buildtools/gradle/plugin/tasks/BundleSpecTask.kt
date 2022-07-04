package dev.elide.buildtools.gradle.plugin.tasks

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import dev.elide.buildtools.gradle.plugin.BuildMode
import dev.elide.buildtools.gradle.plugin.ElideExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import tools.elide.bundler.AssetBundler
import tools.elide.bundler.AssetBundler.ManifestFormat
import tools.elide.crypto.HashAlgorithm
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines the abstract base task type for all tasks which generate an asset catalog spec in addition to some asset
 * output used in a build; after running the regular task, the asset catalog is built, and later handed off to an
 * instance of [BundleWriteTask] to write the resulting catalog spec.
 *
 * @see BundleWriteTask which is responsible for ultimately writing the bundle created by an implementation of this task
 */
abstract class BundleSpecTask<M : Message, Spec> : DefaultTask() {
    companion object {
        /** Asset bundler tool. */
        @Suppress("unused")
        internal val bundler: AssetBundler = AssetBundler.create()

        /** Proto-JSON printer. */
        internal val jsonPrinter = JsonFormat
            .printer()
            .omittingInsignificantWhitespace()
            .includingDefaultValueFields()

        /** @return Digester for the provided algorithm. */
        @JvmStatic internal fun HashAlgorithm.digester(): MessageDigest? = when (this) {
            HashAlgorithm.MD5 -> MessageDigest.getInstance("MD5")
            HashAlgorithm.SHA1 -> MessageDigest.getInstance("SHA-1")
            HashAlgorithm.SHA256 -> MessageDigest.getInstance("SHA-256")
            HashAlgorithm.SHA512 -> MessageDigest.getInstance("SHA-512")
            HashAlgorithm.IDENTITY -> null
            else -> throw IllegalArgumentException("Unrecognized hash algorithm: $name")
        }

        @JvmStatic internal fun ManifestFormat.fileNamed(name: String): String = when (this) {
            ManifestFormat.BINARY -> "$name.assets.pb"
            ManifestFormat.TEXT -> "$name.assets.pb.txt"
            ManifestFormat.JSON -> "$name.assets.pb.json"
        }

        @JvmStatic
        internal fun resolveJsIrLinkTask(project: Project): KotlinJsIrLink {
            // resolve the Kotlin JS compile task (IR only)
            return try {
                project.tasks.named(
                    "compileProductionExecutableKotlinJs",
                    KotlinJsIrLink::class.java
                ).get()
            } catch (noSuchTask: UnknownTaskException) {
                throw IllegalStateException(
                    "Failed to resolve Kotlin JS IR link task: please ensure the Kotlin JS plugin is applied " +
                    "to the project and configured to use the IR compiler.",
                    noSuchTask
                )
            }
        }

        @JvmStatic
        @Suppress("SwallowedException")
        protected fun resolveInflateRuntimeTask(project: Project, extension: ElideExtension): InflateRuntimeTask {
            // resolve the inflate-runtime task installed on the root project, or if there is not one, create it.
            return try {
                if (
                    project.rootProject.tasks.findByPath(":${InflateRuntimeTask.TASK_NAME}") != null
                ) {
                    project.rootProject.tasks.named(InflateRuntimeTask.TASK_NAME, InflateRuntimeTask::class.java)
                        .get()
                } else {
                    InflateRuntimeTask.install(
                        extension,
                        project.rootProject,
                    )
                }
            } catch (noSuchTask: UnknownTaskException) {
                // install it
                InflateRuntimeTask.install(
                    extension,
                    project.rootProject,
                )
            }
        }

        /**
         * Install a task in the [project] task set which writes the compiled asset catalog provided by [sourceTaskName]
         * from the provided set of [deps] to the [sourceTaskName]'s spec name and output directory properties.
         *
         * It is up to the caller to place the returned task on the build graph by adding a dependency from the main
         * build entrypoint into the catalog install task. The returned task output needs to be exported as part of the
         * main injected SSR artifact for a given module in order to make it into downstream project resources.
         *
         * @param sourceTaskName Task name which is generating the asset catalog.
         * @param mode Active build mode for this task, should correspond to `sourceTaskName`.
         * @param project Target project where this task should be installed.
         * @param deps Dependencies to declare for the asset catalog.
         * @return Prepared bundle write task.
         */
        @JvmStatic protected fun installCatalogTask(
            sourceTaskName: String,
            mode: BuildMode,
            project: Project,
            deps: List<Any>,
        ): BundleWriteTask {
            val bundleTaskName = "generate${mode.name.lowercase().capitalized()}EmbeddedJsSpec"
            val task = project.tasks.create(bundleTaskName, BundleWriteTask::class.java) {
                val sourceTask = project.tasks.named(sourceTaskName, BundleSpecTask::class.java).get()
                it.bundleEncoding.set(sourceTask.bundleEncoding)
                it.outputBundleFolder.set(sourceTask.outputBundleFolder.get())
                it.outputSpecName.set(sourceTask.outputSpecName.get())
                it.sourceTaskName.set(sourceTask.name)
                it.outputs.file(it.outputAssetSpecFile)
            }
            val allDeps = deps.plus(project.tasks.named(sourceTaskName)).toTypedArray()
            task.dependsOn(allDeps)
            task.shouldRunAfter(allDeps)
            return task
        }
    }

    // Hard-coded or constant values that relate to asset bundles.
    internal object StaticValues {
        const val currentVersion: Int = 2
        val defaultEncoding: ManifestFormat = ManifestFormat.TEXT
        val assetHashAlgo: HashAlgorithm = HashAlgorithm.SHA256
    }

    // Assembled spec which should be written.
    @get:Input
    internal val assetSpec: AtomicReference<M?> = AtomicReference(null)

    /** Folder in which to put built bundle targets. */
    @get:Input
    @get:Option(
        option = "bundleEncoding",
        description = "Mode to use for encoding the asset bundle. Typically managed by the plugin.",
    )
    abstract val bundleEncoding: Property<ManifestFormat>

    /** Folder in which to put built bundle targets. */
    @get:Input
    @get:Option(
        option = "outputBundleFolder",
        description = "Where to put compiled asset catalogs on the filesystem. Typically managed by the plugin.",
    )
    abstract val outputBundleFolder: Property<String>

    /** Name to give the asset catalog being affixed by this task. */
    @get:Input
    @get:Option(
        option = "outputSpecName",
        description = "Name to give the asset catalog built by this task. Typically managed by the plugin.",
    )
    abstract val outputSpecName: Property<String>

    protected fun fingerprintMessage(catalog: Message): ByteString? {
        val bytes = catalog.toByteArray()
        val digest = StaticValues.assetHashAlgo.digester().let { digester ->
            digester?.digest(bytes)
        }
        return if (digest != null) {
            ByteString.copyFrom(
                digest
            )
        } else {
            null
        }
    }

    /**
     * Utility function which enters a DSL for building an asset bundle, which is assigned to the local task state and
     * then returned for further use.
     *
     * Local task state writes the resulting bundle when [BundleWriteTask] is dispatched.
     */
    protected abstract fun buildAssetCatalog(builderOp: Spec.() -> Unit): M

    /**
     * Build the asset catalog that should be enclosed with the asset resulting from [runAction]; the context of the
     * returned builder is of [Spec], which should produce a [Message] instance [M].
     *
     * @return Builder closure to produce an asset catalog.
     */
    abstract fun assetCatalog(): (Spec.() -> Unit)

    /**
     * Run the action defined by this [BundleSpecTask], which should generate source files, or compile source files, as
     * applicable; after running this step, the asset catalog is built via [assetCatalog].
     */
    abstract fun runAction()

    /**
     * Entrypoint for a standard [BundleSpecTask] implementation; first, the action itself is run, and then an asset
     * catalog payload is built by interrogating the task.
     *
     * The resulting asset catalog bundle is later written by the [BundleWriteTask], configuration and unexpected errors
     * permitting.
     */
    @TaskAction fun execBundledTask() {
        runAction()
        assetSpec.set(
            buildAssetCatalog(assetCatalog())
        )
    }
}
