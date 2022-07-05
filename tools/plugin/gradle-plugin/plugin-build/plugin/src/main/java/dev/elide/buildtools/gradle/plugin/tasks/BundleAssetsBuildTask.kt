package dev.elide.buildtools.gradle.plugin.tasks

import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.AssetInfo
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.options.Option
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.TreeSet
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Collectors

/** Task which creates Elide asset specifications for embedding in app JARs. */
@Suppress("UnusedPrivateMember", "UnstableApiUsage")
abstract class BundleAssetsBuildTask : BundleBaseTask() {
    companion object {
        private const val TASK_NAME = "bundleAssets"
        private const val ASSETS_INTERMEDIATE_FOLDER = "serverAssets"

        @JvmStatic fun isEligible(extension: ElideExtension, project: Project): Boolean {
            return (
                project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") ||
                extension.hasServerTarget()
            )
        }

        // Apply plugins which are required to run before the server-side build task.
        @JvmStatic fun applyPlugins(project: Project, cbk: () -> Unit) {
            applyPlugin(project, "org.jetbrains.kotlin.jvm") {
                cbk()
            }
        }

        // After determining the server-side plugin is eligible to run, apply plugins, then build/install tasks.
        @JvmStatic fun install(extension: ElideExtension, project: Project) {
            applyPlugins(project) {
                project.afterEvaluate {
                    installTasks(
                        extension,
                        project,
                    )
                }
            }
        }

        // Under the scope of applied required plugins, install tasks and wire together server-side targets.
        @JvmStatic fun installTasks(extension: ElideExtension, project: Project) {
            val allTasks = ArrayList<TaskProvider<out Task>>()

            // add SSR injection task if the user has configured a bundle
            if (extension.server.hasSsrBundle()) {
                allTasks.addAll(
                    buildSsrConsumeTask(
                        extension,
                        project,
                    )
                )
            }

            // if the user has configured server-side assets, setup tasks to process those.
            if (extension.server.hasAssets()) {
                allTasks.addAll(
                    buildAssetTasks(
                        extension,
                        project,
                    )
                )
            }

            // register task to build embedded asset spec
            project.tasks.register(TASK_NAME, BundleAssetsBuildTask::class.java) {
                it.dependsOn(allTasks)
            }
        }

        // If the user has configured an SSR bundle, this method is called to configure a task which consumes it. The
        // bundle is injected into the server's resources set, along with the proto-spec for the bundle.
        @JvmStatic fun buildSsrConsumeTask(extension: ElideExtension, project: Project): List<TaskProvider<out Task>> {
            val allTasks = ArrayList<TaskProvider<out Task>>()

            // add a project configuration which is capable of consuming SSR artifacts.
            val ssrDist: Configuration = project.configurations.create("ssrDist") {
                it.isCanBeResolved = true
                it.isCanBeConsumed = false
            }

            // make sure we have a valid, non-empty project and configuration
            val targetProject = extension.server.ssrConfig.targetProject.get()
            val targetConfig = extension.server.ssrConfig.targetConfiguration.get()
            if (targetProject?.isNotBlank() != true || targetConfig?.isNotBlank() != true) {
                throw IllegalStateException(
                    "Failed to resolve target project or configuration for SSR bundle. " +
                        "This is an internal error; please report it to the Elide build-tools authors."
                )
            }

            // add a dependency for the artifact, based on the declared bundle to be injected
            project.dependencies.apply {
                add(ssrDist.name, project(
                    mapOf(
                        "path" to extension.server.ssrConfig.targetProject.get(),
                        "configuration" to extension.server.ssrConfig.targetConfiguration.get(),
                    )
                ))
            }

            // prep a task which copies the SSR distribution into resources
            val ssrDistCopyTask = project.tasks.register("copySsrDist", Copy::class.java) {
                it.from(ssrDist) { spec ->
                    spec.include("**/*.js")
                    spec.include("**/*.pb*")
                }
                it.into(
                    "${project.buildDir}/resources/main/embedded"
                )
            }

            // resolve the `processResources` task, make sure it invokes the SSR dist copy task when packaging
            // assets for the application.
            val processResources = project.tasks.named("processResources", ProcessResources::class.java) {
                it.dependsOn(ssrDistCopyTask)
            }
            allTasks.add(ssrDistCopyTask)
            allTasks.add(processResources)
            return allTasks
        }

        // If the user has configured served assets, this method is called to configure a set of tasks which process and
        // package those assets, in addition to generating metadata files which describe them to the server at runtime.
        @JvmStatic
        @Suppress("SpreadOperator")
        fun buildAssetTasks(extension: ElideExtension, project: Project): List<TaskProvider<out Task>> {
            val allTasks = ArrayList<TaskProvider<out Task>>()
            val assetConfigs = extension.server.assets.assets

            // compute and build all `AssetInfo` copy specs and configurations
            val allSpecs = assetConfigs.entries.stream().parallel().map { assetEntry ->
                val moduleId = assetEntry.key
                val assetConfig = assetEntry.value

                // translate the asset configuration into an asset info record
                val assetType = assetConfig.type
                val assetDeps = assetConfig.directDeps.get() ?: TreeSet()

                // prepare the asset's copy spec, falling back to a sensible default glob if needed.
                val targetCopySpec = assetConfig.copySpec.get() ?: project.copySpec {
                    it.from("${project.projectDir}/src/main") { spec ->
                        spec.include("**/*.${assetType.extension}")
                    }
                }
                val targetPaths = assetConfig.filePaths.get()
                if (targetPaths == null || targetPaths.isEmpty()) throw IllegalStateException(
                    "Empty source set for module '$moduleId': please remove it or add sources"
                )
                AssetInfo(
                    module = moduleId,
                    type = assetType,
                    directDeps = assetDeps,
                    copySpec = targetCopySpec,
                    paths = targetPaths,
                )
            }.sorted { left, right ->
                left.module.compareTo(right.module)
            }.collect(Collectors.toMap({ it.module }, { it }, { left, _ ->
                throw IllegalStateException(
                    "Duplicate asset module ID '${left.module}': " +
                    "please remove one of the duplicate entries from the configuration"
                )
            }, {
                ConcurrentSkipListMap()
            }))

            // build a map of source files => owning modules, so we can enforce the rule that only one module owns each
            // source file. this is later passed on, so it can be expressed to the server, and also used to compute the
            // transitive closure of each module's dependencies.
            val moduleSourceMap = allSpecs.values.stream().parallel().flatMap { asset ->
                asset.paths.stream().map { path ->
                    path to asset.module
                }
            }.collect(Collectors.toMap({ it.first }, { it.second }, { left, _ ->
                throw IllegalStateException(
                    "Duplicate source path '$left': mapped for more than one asset module"
                )
            }, {
                ConcurrentSkipListMap()
            }))

            // sorted set of all files in the copy job
            val allFiles = moduleSourceMap.keys

            // set up a copy job to copy all assets into the asset root.
            val assetsCopy = project.tasks.register("copyServerAssets", Copy::class.java) { copy ->
                copy.from(*(allFiles.toTypedArray()))
                copy.into(
                    "${project.buildDir}/$ASSETS_INTERMEDIATE_FOLDER/main/assets"
                )
            }

            // set up a task which follows up on the copy job by taking fingerprints and reading file content into the
            // asset spec. at the same time, we take the opportunity to generate the graph of asset dependencies.
            val assetGraph = project.tasks.register("generateAssetGraph", GenerateAssetGraphTask::class.java) {
                it.dependsOn(assetsCopy)
                it.assetModules.set(allSpecs)
                it.assetModuleMap.set(moduleSourceMap)
                it.digestAlgorithm.set(extension.server.assets.bundlerConfig.digestAlgorithm.get())
                it.compressionConfig.set(extension.server.assets.bundlerConfig.compressionConfig())
                it.bundleEncoding.set(extension.server.assets.bundlerConfig.format.get())
                it.outputBundleFolder.set(project.file(
                    "${project.buildDir}/$ASSETS_INTERMEDIATE_FOLDER/main/bundle"
                ).absolutePath)
                it.manifestName.set("assets")
                it.manifestFile.set(project.file(
                    // `{outputBundleFolder}/{outputManifestName}.{ext = .pb.*}`
                    "${it.outputBundleFolder.get()}/${it.bundleEncoding.get().fileNamed(it.manifestName.get())}"
                ))
                it.inputFiles.set(project.files(project.file(
                    "${project.buildDir}/$ASSETS_INTERMEDIATE_FOLDER/main/assets"
                ).listFiles()))
            }

            // as a final step, copy all outputs from the intermediates output bases into the finalized set of resources
            // embedded with the server binary.
            val finalizedAssetCopy = project.tasks.register("copyFinalAssets", Copy::class.java) { copy ->
                copy.dependsOn(assetsCopy)
                copy.dependsOn(assetGraph)
                copy.from("${project.buildDir}/$ASSETS_INTERMEDIATE_FOLDER/main/assets") {
                    it.include("**/*.*")
                }
                copy.from("${project.buildDir}/$ASSETS_INTERMEDIATE_FOLDER/main/bundle") {
                    it.include("**/*.*")
                }
                copy.into(
                    "${project.buildDir}/resources/main/assets"
                )
            }

            // resolve the process-resources step for the server target, and make it depend on our embedded assets.
            val processResources = project.tasks.named("processResources", ProcessResources::class.java) {
                it.dependsOn(assetGraph)
                it.dependsOn(finalizedAssetCopy)
            }
            allTasks.add(assetsCopy)
            allTasks.add(processResources)
            return allTasks
        }
    }

    init {
        description = "Configures an application target for use with ESBuild or Webpack"
        group = BasePlugin.BUILD_GROUP

        // set defaults
        with(project) {
            // set the default output bundle folder
            outputBundleFolder.set(
                file("$buildDir\\$defaultOutputBundleFolder").absolutePath
            )
            bundleEncoding.set(
                StaticValues.defaultEncoding
            )
        }
    }

    /** All source files to consider as bundled asset inputs. */
    @get:Input
    @get:Option(
        option = "assets",
        description = "Source asset input files to bundle for server-side use.",
    )
    abstract val assets: ListProperty<AssetInfo>

    /** @inheritDoc */
    override fun runAction() {
        // nothing at this time
    }
}
