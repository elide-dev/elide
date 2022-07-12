package dev.elide.buildtools.gradle.plugin.tasks

import com.google.protobuf.ByteString
import com.google.protobuf.Message
import com.google.protobuf.util.JsonFormat
import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.bundler.cfg.StaticValues
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm
import java.security.MessageDigest

/** Base task which provides shared logic and declarations across all Elide plugin tasks. */
@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class BundleBaseTask : DefaultTask() {
    companion object {
        const val defaultOutputBundleFolder = "bundle"
        const val defaultOutputBundleName = "bundle.js"
        const val defaultOutputOptimizedName = "bundle.opt.js"

        /** Proto-JSON printer. */
        internal val jsonPrinter = JsonFormat
            .printer()
            .omittingInsignificantWhitespace()
            .includingDefaultValueFields()

        /** @return Digester for the provided algorithm. */
        @JvmStatic
        internal fun HashAlgorithm.digester(): MessageDigest? = when (this) {
            HashAlgorithm.MD5 -> MessageDigest.getInstance("MD5")
            HashAlgorithm.SHA1 -> MessageDigest.getInstance("SHA-1")
            HashAlgorithm.SHA256 -> MessageDigest.getInstance("SHA-256")
            HashAlgorithm.SHA512 -> MessageDigest.getInstance("SHA-512")
            HashAlgorithm.IDENTITY -> null
            else -> throw IllegalArgumentException("Unrecognized hash algorithm: $name")
        }

        @JvmStatic
        internal fun ManifestFormat.fileNamed(name: String): String = when (this) {
            ManifestFormat.BINARY -> "$name.assets.pb"
            ManifestFormat.TEXT -> "$name.assets.pb.txt"
            ManifestFormat.JSON -> "$name.assets.pb.json"
            else -> error(
                "Unrecognized bundle format: '${this.name}'"
            )
        }

        /** Apply the plugin at the provided [id] to the provided [project] within the scope of the [cbk]. */
        @JvmStatic
        fun applyPlugin(project: Project, id: String, cbk: () -> Unit) {
            project.plugins.withId(id) {
                cbk.invoke()
            }
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

        @JvmStatic
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
    }

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

    /**
     * Run any needed pre-action steps, which should occur before invoking the main bundle step; task implementations
     * are encouraged to override this method if needed.
     */
    protected open fun preAction() {
        // default: nothing
    }

    /**
     * Run any needed post-action steps, which should occur after invoking the main bundle step; task implementations
     * are encouraged to override this method if needed.
     */
    protected open fun postAction() {
        // default: nothing
    }

    /**
     * Run the action defined by this [BundleBaseTask], which should generate source files, or compile source files, as
     * applicable; after running this step, post-action build steps can be run with [postAction], and pre-action steps
     * can be run with [preAction].
     */
    abstract fun runAction()

    /**
     * Entrypoint for a standard [BundleBaseTask] implementation; first, pre-action steps are run, then, the action
     * itself is run, and then any post action steps are run.
     */
    @TaskAction fun execTask() {
        preAction()
        runAction()
        postAction()
    }
}
