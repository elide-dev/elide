package dev.elide.buildtools.gradle.plugin.tasks

import com.google.protobuf.Message
import dev.elide.buildtools.gradle.plugin.BuildMode
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.options.Option
import org.gradle.configurationcache.extensions.capitalized
import java.util.concurrent.atomic.AtomicReference

/**
 * Defines the abstract base task type for all tasks which generate an asset catalog spec in addition to some asset
 * output used in a build; after running the regular task, the asset catalog is built, and later handed off to an
 * instance of [BundleWriteTask] to write the resulting catalog spec.
 *
 * @see BundleWriteTask which is responsible for ultimately writing the bundle created by an implementation of this task
 */
abstract class BundleSpecTask<M : Message, Spec> : BundleBaseTask() {
    companion object {
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

    // Assembled spec which should be written.
    @get:Input
    internal val assetSpec: AtomicReference<M?> = AtomicReference(null)

    /** Name to give the asset catalog being affixed by this task. */
    @get:Input
    @get:Option(
        option = "outputSpecName",
        description = "Name to give the asset catalog built by this task. Typically managed by the plugin.",
    )
    abstract val outputSpecName: Property<String>

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
     * Post-action entrypoint for a standard [BundleSpecTask] implementation; first, the action itself is run, and then
     * an asset catalog payload is built by interrogating the task.
     *
     * The resulting asset catalog bundle is later written by the [BundleWriteTask], configuration and unexpected errors
     * permitting.
     */
    override fun postAction() {
        assetSpec.set(
            buildAssetCatalog(assetCatalog())
        )
    }
}
