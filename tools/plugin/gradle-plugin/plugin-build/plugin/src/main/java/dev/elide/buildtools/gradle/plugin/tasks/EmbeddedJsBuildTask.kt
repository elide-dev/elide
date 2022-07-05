package dev.elide.buildtools.gradle.plugin.tasks

import com.github.gradle.node.task.NodeTask
import com.google.protobuf.Timestamp
import dev.elide.buildtools.gradle.plugin.BuildMode
import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.cfg.ElideJsHandler
import dev.elide.buildtools.gradle.plugin.js.BundleTarget
import dev.elide.buildtools.gradle.plugin.js.BundleTool
import dev.elide.buildtools.gradle.plugin.js.BundleType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.options.Option
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.RootPackageJsonTask
import tools.elide.assets.*
import tools.elide.assets.EmbeddedScriptMetadataKt.jsScriptMetadata
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.time.Instant

/** Task which builds JavaScript targets for embedded use with Elide. */
@Suppress("unused")
abstract class EmbeddedJsBuildTask : BundleSpecTask<EmbeddedScript, EmbeddedBundleSpec>() {
    companion object {
        private const val TASK_NAME = "bundleEmbeddedJs"
        private val defaultTargetMode: BuildMode = BuildMode.DEVELOPMENT
        private const val defaultTargetModeName: String = BuildMode.DEVELOPMENT_NAME

        private val defaultTargetType: BundleTarget = BundleTarget.EMBEDDED
        private const val defaultTargetTypeName: String = BundleTarget.EMBEDDED_NAME

        private const val defaultEcmaVersion: String = "2020"
        private const val defaultLibraryName: String = "embedded"
        private const val defaultEntrypointName: String = "main.js"
        private const val defaultOutputConfig: String = "embedded-js/compile.js"
        private const val defaultProcessShim: String = "embedded-js/shim.process.js"
        const val esbuildConfigTemplatePath = "/dev/elide/buildtools/js/esbuild-wrapper.js.hbs"
        const val processShimTemplatePath = "/dev/elide/buildtools/js/process-wrapper.js.hbs"

        // Determine whether the JS bundle task is eligible to run for the given project / extension pair.
        @JvmStatic fun isEligible(extension: ElideExtension, project: Project): Boolean {
            // we enable the JS build extension in two cases:
            //
            // 1) the user has installed the KotlinJS plugin and the Elide plugin. based on whether KotlinJS is
            //    configured for Node or Browser targets, and the presence of the NodeJS plugin, it either builds in
            //    `EMBEDDED` or `WEB` mode.
            // 2) the user has installed the Elide plugin, and configured a JS block. in this case, the JS block will
            //    tell us the intended target and tool. based on these values, the NodeJS or KotlinJS plugins may be
            //    applied on behalf of the user.
            return (
              project.plugins.hasPlugin("org.jetbrains.kotlin.js") ||
              extension.hasJsTarget()
            )
        }

        // Apply plugins which are required to run before the JS bundle task.
        @JvmStatic fun applyPlugins(node: Boolean, project: Project, cbk: () -> Unit) {
            applyPlugin(project, "org.jetbrains.kotlin.js") {
                if (node) {
                    applyPlugin(project, "com.github.node-gradle.node") {
                        cbk.invoke()
                    }
                } else {
                    cbk.invoke()
                }
            }
        }

        // After determining the task is eligible, install it in the given project with the provided extension settings.
        @JvmStatic fun install(extension: ElideExtension, project: Project) {
            // apply NPM deps for tooling
            injectDeps(project)

            // determine whether we're running an embedded build, in which case the Node plugin is applied as well.
            val isEmbedded = (
                extension.js.bundleTarget.get() == BundleTarget.EMBEDDED
            )

            // apply requisite plugins
            applyPlugins(isEmbedded, project) {
                installTasks(
                    extension,
                    project,
                )
            }
        }

        // Build and install tasks within the scope of applied required plugins.
        @JvmStatic fun installTasks(extension: ElideExtension, project: Project) {
            // resolve the inflate-runtime task installed on the root project, or if there is not one, create it.
            val inflateRuntime = resolveInflateRuntimeTask(project, extension)

            // load JS plugin, configure with output, connect outputs to embedded build
            project.extensions.configure(KotlinJsProjectExtension::class.java) { jsExt ->
                val elideJsExt = extension.js
                val hasNode = project.plugins.hasPlugin(
                    "com.github.node-gradle.node"
                )

                jsExt.js(KotlinJsCompilerType.IR) {
                    if (hasNode || elideJsExt.bundleTarget.get() == BundleTarget.EMBEDDED) {
                        nodejs {
                            binaries.executable()
                        }
                    } else {
                        browser {
                            binaries.executable()
                        }
                    }
                }

                // resolve JS IR link task that we just set up
                val compileProdKotlinJs = resolveJsIrLinkTask(
                    project
                )

                // resolve embedded sources at `ssr/ssr.js`
                val fetchBuildSources = project.tasks.create("prepareEmbeddedJsBuild", Copy::class.java) {
                    it.dependsOn(compileProdKotlinJs)
                    it.from(compileProdKotlinJs.outputs.files.files) { copySpec ->
                        copySpec.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    }
                    it.from(compileProdKotlinJs.outputFileProperty) { copySpec ->
                        copySpec.rename { "ssr.js" }
                    }
                    it.into(
                        "${project.buildDir}/ssr"
                    )
                }

                val target = elideJsExt.bundleTarget.get()
                val tool = elideJsExt.bundleTool.get()

                setup(
                    project,
                    fetchBuildSources,
                    compileProdKotlinJs,
                    tool,
                    target,
                    extension,
                    elideJsExt,
                    inflateRuntime,
                )
            }
        }

        // Load an embedded resource from the plugin JAR.
        @JvmStatic private fun loadEmbedded(filename: String): String {
            return (EmbeddedJsBuildTask::class.java.getResourceAsStream(
                filename
            ) ?: throw FileNotFoundException(
                "Failed to locate embedded plugin resource: '$filename'"
            )).bufferedReader(
                StandardCharsets.UTF_8
            ).readText()
        }

        @JvmStatic fun injectDeps(project: Project) {
            // make sure node plugin is applied
            project.plugins.apply("com.github.node-gradle.node")
            project.dependencies.apply {
                (this as ExtensionAware).extensions.configure(NpmDependencyExtension::class.java) { npm ->
                    add("implementation", npm("esbuild", Versions.esbuild))
                    add("implementation", npm("prepack", Versions.prepack))
                }
            }
        }

        // Setup build tasks for the provided project.
        @JvmStatic fun setup(
            project: Project,
            fetchSources: Copy,
            kotlinJsLink: Task,
            tool: BundleTool,
            target: BundleTarget,
            extension: ElideExtension,
            jsExtension: ElideJsHandler,
            inflateRuntime: InflateRuntimeTask,
        ) {
            if (tool == BundleTool.ESBUILD && target == BundleTarget.EMBEDDED) {
                BuildMode.values().forEach { mode ->
                    setupEmbeddedEsbuildTask(
                        mode,
                        project,
                        fetchSources,
                        kotlinJsLink,
                        jsExtension,
                        inflateRuntime,
                    )
                }
                setupEsbuildEntrypointTask(
                    project,
                    extension,
                )
            } else if (tool == BundleTool.WEBPACK && target != BundleTarget.EMBEDDED) {
                BuildMode.values().forEach { mode ->
                    setupBrowserWebpackBuildTask(
                        mode,
                        project,
                        fetchSources,
                        kotlinJsLink,
                        extension,
                        jsExtension,
                    )
                }
            } else throw IllegalArgumentException(
                "Unsupported bundle tool/target combination: $tool/$target"
            )
        }

        @JvmStatic fun setupEsbuildEntrypointTask(
            project: Project,
            extension: ElideExtension,
        ) {
            project.afterEvaluate {
                val activeMode = extension.mode
                val targetBundleTask = "generate${activeMode.name.lowercase().capitalized()}EsBuildConfig"
                val genSpecTaskName = "generate${activeMode.name.lowercase().capitalized()}EmbeddedJsSpec"
                val targetEmbeddedTask = "${activeMode.name.lowercase()}EmbeddedExecutable"

                // create a synthesized distribution as an output
                val mainDist = project.configurations.create("nodeSsrDist") {
                    it.isCanBeConsumed = true
                    it.isCanBeResolved = false
                }
                project.artifacts.apply {
                    add(
                        mainDist.name,
                        project.tasks.named(targetEmbeddedTask).map {
                            it.outputs.files.files.single()
                        },
                    )
                    add(
                        mainDist.name,
                        project.tasks.named(genSpecTaskName).map {
                            it.outputs.files.files.single()
                        },
                    )
                }

                project.tasks.create(TASK_NAME) {
                    it.dependsOn(genSpecTaskName)
                    it.dependsOn(targetBundleTask)
                    it.dependsOn(targetEmbeddedTask)
                }
                project.tasks.named("build") {
                    it.dependsOn(TASK_NAME)
                }
            }
        }

        @JvmStatic fun setupEmbeddedEsbuildTask(
            mode: BuildMode,
            project: Project,
            fetchSources: Copy,
            kotlinJsLink: Task,
            jsExtension: ElideJsHandler,
            inflateRuntime: InflateRuntimeTask,
        ) {
            // resolve root-package-json task
            val rootPackageJson = project.rootProject.tasks.withType(
                RootPackageJsonTask::class.java
            ).first()

            val modeName = mode.name.lowercase().capitalized()
            val targetBundleTask = "generate${modeName}EsBuildConfig"
            val buildTask = project.tasks.create(targetBundleTask, EmbeddedJsBuildTask::class.java) {
                // setup deps
                it.dependsOn(project.tasks.named("productionExecutableCompileSync"))
                it.dependsOn(kotlinJsLink)
                it.dependsOn(fetchSources)
                it.dependsOn(inflateRuntime)

                // setup attrs
                it.group = "other"
                it.mode = mode
                it.tool = jsExtension.bundleTool.get()
                it.target = jsExtension.bundleTarget.get()
                it.entryFile.set(fetchSources.destinationDir / "ssr.js")
                it.libraryName = jsExtension.libraryName.get()

                // setup properties
                it.outputBundleName.set(buildString {
                    append(project.name)
                    when (mode) {
                        BuildMode.PRODUCTION -> append("-prod")
                        BuildMode.DEVELOPMENT -> append("-dev")
                    }
                    append(".js")
                })
                it.outputBundleName.set(buildString {
                    append(project.name)
                    when (mode) {
                        BuildMode.PRODUCTION -> append("-prod")
                        BuildMode.DEVELOPMENT -> append("-dev")
                    }
                    append(".opt")
                    append(".js")
                })
                it.outputBundleFolder.set(
                    File("${project.buildDir}/distributions").absolutePath
                )
                it.processShim.set(
                    File("${project.buildDir}/esbuild/process-shim.${modeName.lowercase()}.js")
                )
                it.outputConfig.set(
                    File("${project.buildDir}/esbuild/esbuild.${modeName.lowercase()}.js")
                )
                it.modulesFolders.set(listOf(
                    File(inflateRuntime.modulesPath.get().absolutePath),
                    File("${project.buildDir}/js/node_modules"),
                    File("${project.projectDir}/node_modules"),
                    File("${project.rootProject.buildDir}/js/node_modules"),
                    File("${project.rootProject.projectDir}/node_modules"),
                ))
                val defaultOptimize = mode == BuildMode.PRODUCTION
                it.minify = jsExtension.minify.get() ?: defaultOptimize
                it.prepack = jsExtension.prepack.get() ?: defaultOptimize
            }

            val targetEmbeddedTask = "${modeName.lowercase()}EmbeddedExecutable"
            val nodeBuildTask = project.tasks.create(
                targetEmbeddedTask,
                NodeTask::class.java
            ) {
                it.group = "distribution"
                it.dependsOn(buildTask)
                it.dependsOn(buildTask.processShim)
                it.dependsOn(buildTask.outputConfig)
                it.dependsOn(inflateRuntime)
                it.script.set(buildTask.outputConfig.get())

                it.setNodeModulesPath(
                    listOf(
                        inflateRuntime.modulesPath.get().absolutePath,
                        "${project.rootDir}/node_modules",
                        "${rootPackageJson.rootPackageJson.parentFile / "node_modules"}"
                    ).joinToString(":")
                )
                it.inputs.files(
                    buildTask.processShim,
                    buildTask.outputConfig,
                    fetchSources.destinationDir / "ssr.js",
                )
                if (mode == BuildMode.PRODUCTION) {
                    it.outputs.file(buildTask.outputOptimizedFile.absolutePath)
                } else {
                    it.outputs.file(buildTask.outputBundleFile.absolutePath)
                }
            }
            val catalogGenTask = installCatalogTask(
                targetBundleTask,
                mode,
                project,
                listOf(
                    targetBundleTask,
                    targetEmbeddedTask,
                )
            )

            // create a distribution for the bundle
            val nodeDist = project.configurations.create("nodeSsrDist${mode.name.lowercase().capitalized()}") {
                it.isCanBeConsumed = true
                it.isCanBeResolved = false
            }

            // add to project artifacts
            project.artifacts.add(
                nodeDist.name,
                nodeBuildTask.outputs.files.files.single()
            )
            project.artifacts.add(
                nodeDist.name,
                catalogGenTask.outputs.files.files.single()
            )
        }

        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun setupBrowserWebpackBuildTask(
            mode: BuildMode,
            project: Project,
            fetchSources: Copy,
            kotlinJsLink: Task,
            extension: ElideExtension,
            jsExtension: ElideJsHandler,
        ) {
            project.logger.lifecycle("Configuring embedded 'webpack' task for CSR...")
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

            // setup asset spec
            outputSpecName.set(
                StaticValues.defaultEncoding.fileNamed("embedded")
            )
            bundleEncoding.set(
                StaticValues.defaultEncoding
            )

            // set the default output bundle name and optimized bundle name
            outputBundleName.set(
                defaultOutputBundleName
            )
            outputOptimizedName.set(
                defaultOutputOptimizedName
            )

            // set the default output config file/wrapper
            outputConfig.set(file(
                "$buildDir/$defaultOutputConfig"
            ))

            // set the default process shim file
            processShim.set(file(
                "$buildDir/$defaultProcessShim"
            ))
        }
    }

    /** Build mode to apply for targets. */
    @get:Input
    @get:Option(
        option = "mode",
        description = (
            "Build mode: `${BuildMode.DEVELOPMENT_NAME}` or `${BuildMode.PRODUCTION_NAME}`. Passed to Node. " +
                "Defaults to `$defaultTargetModeName`."
            ),
    )
    var mode: BuildMode = defaultTargetMode

    /** Target build type to apply for JS targets. */
    @get:Input
    @get:Option(
        option = "target",
        description = (
            "Type of target to build: `${BundleTarget.EMBEDDED_NAME}`, `${BundleTarget.NODE_NAME}`, or " +
                "`${BundleTarget.WEB_NAME}`. Defaults to `$defaultTargetTypeName`."
            ),
    )
    var target: BundleTarget = defaultTargetType

    /** Target format type for the bundle. */
    @get:Input
    @get:Option(
        option = "format",
        description = (
            "Format of the bundle to build: `${BundleType.IIFE_NAME}`, `${BundleType.COMMON_JS_NAME}`, or " +
                "`${BundleType.ESM_NAME}`. Defaults to the value stipulated by `target`."
            ),
    )
    var format: BundleType = target.bundleType

    /** Tool to use for building this target.. */
    @get:Input
    @get:Option(
        option = "tool",
        description = (
            "Tool to use for JS bundling. Supported values are `${BundleTool.ESBUILD_NAME}` or " +
                "`${BundleTool.WEBPACK_NAME}`. Defaults to the value stipulated by `target`."
            ),
    )
    var tool: BundleTool = target.bundleTool

    /** Target format type for the bundle. */
    @get:Input
    @get:Option(
        option = "ecma",
        description = "ECMA standard level to target. Defaults to `$defaultEcmaVersion`.",
    )
    var ecma: String = defaultEcmaVersion

    /** Name to use within the JAR for this JS bundle. */
    @get:Input
    @get:Option(
        option = "libraryName",
        description = "Name to use for the output JavaScript bundle. Defaults to `$defaultLibraryName`.",
    )
    var libraryName: String = defaultLibraryName

    /** Output file for the ESBuild/Webpack configuration, as applicable. */
    @get:OutputFile
    @get:Option(
        option = "outputConfig",
        description = "Where to put the generated build configuration or entrypoint. Typically managed by the plugin."
    )
    abstract val outputConfig: Property<File>

    /** Output file for the `process` shim, as applicable. */
    @get:OutputFile
    @get:Option(
        option = "processShim",
        description = "Where to put the generated Node process shim. Typically managed by the plugin.",
    )
    abstract val processShim: Property<File>

    /** Name to give the bundle being compiled by this task. */
    @get:Input
    @get:Option(
        option = "outputBundleName",
        description = "Name to give the bundle built by this task. Typically managed by the plugin.",
    )
    abstract val outputBundleName: Property<String>

    /** Name to give the pre-compiled and optimized version of the output bundle. */
    @get:Input
    @get:Option(
        option = "outputOptimizedName",
        description = "Name to give the optimized bundle built by this task. Typically managed by the plugin.",
    )
    abstract val outputOptimizedName: Property<String>

    /** Input Node Modules directory for installed dependencies. */
    @get:InputFiles
    @get:Option(
        option = "modulesFolders",
        description = "Locations of `node_modules` to load from when bundling. Typically managed by the plugin.",
    )
    abstract val modulesFolders: ListProperty<File>

    /** Platform value to specify when invoking ESBuild. */
    @get:Input
    @get:Option(
        option = "platform",
        description = "Platform to specify when invoking ESBuild. Defaults to the value stipulated by `target`.",
    )
    var platform: String = target.platform

    /** Whether to enable React shims for the VM runtime. */
    @get:Input
    @get:Option(
        option = "enableReact",
        description = "Provide low-overhead runtime support for React SSR. Defaults to `true`.",
    )
    var enableReact: Boolean = true

    /** Whether to perform minification on the target bundle. */
    @get:Input
    @get:Option(
        option = "minify",
        description = "Whether to minify the target bundle. Defaults to the value stipulate by `mode`.",
    )
    var minify: Boolean = mode.minify

    /** Whether to perform pre-packing on the target bundle. */
    @get:Input
    @get:Option(
        option = "prepack",
        description = "Whether to run the bundle through `prepack`. Only applies to server-side SSR bundles.",
    )
    var prepack: Boolean = mode.prepack

    /** Whether to generate a single output bundle. */
    @get:Input
    @get:Option(
        option = "bundle",
        description = "Whether to generate a bundle. Defaults to `true` and typically needs to stay `true`.",
    )
    var bundle: Boolean = true

    /** Entrypoint file to begin the compilation from. */
    @Optional
    @get:InputFile
    @get:Option(
        option = "entryFileName",
        description = "Name of the source file which should serve as the entrypoint for this build.",
    )
    var entryFileName: String? = null

    /** Entrypoint file to begin the compilation from. */
    @get:InputFile
    @get:Option(
        option = "entryFile",
        description = "Source file which should serve as the entrypoint for this build.",
    )
    abstract val entryFile: RegularFileProperty

    /** Template content to use for the ESBuild wrapper. Please use with caution, this is not documented yet. */
    @get:Input
    val configTemplate = loadEmbedded(
        esbuildConfigTemplatePath
    )

    /** Template content to use for the `process` shim. Please use with caution, this is not documented yet. */
    @get:Input
    val processShimTemplate = loadEmbedded(
        processShimTemplatePath
    )

    private fun templateVar(varname: String): String = "{{${varname.uppercase()}}}"
    private fun replaceVar(subj: String, varname: String, value: String): String =
        subj.replace(templateVar(varname), value)

    private fun renderTemplateVals(tpl: String): String {
        var subj = tpl
        listOf(
            "entry" to entryFile.asFile.get().absolutePath.fixSlashes(),
            "mode" to mode.name.lowercase().trim(),
            "format" to format.symbol,
            "bundle" to bundle.toString(),
            "minify" to minify.toString(),
            "prepack" to prepack.toString(),
            "libname" to libraryName,
            "platform" to platform.trim().lowercase(),
            "process" to processShim.get().absolutePath.fixSlashes(),
            "outfile" to outputBundleFile.absolutePath.fixSlashes(),
            "prepack_outfile" to outputOptimizedFile.absolutePath.fixSlashes(),
            "nodepath" to modulesFolders.get().joinToString(",") {
                "'${it.absolutePath.fixSlashes()}'"
            },
        ).forEach {
            subj = replaceVar(subj, it.first, it.second)
        }
        return subj
    }

    /** @inheritDoc */
    override fun buildAssetCatalog(builderOp: EmbeddedBundleSpec.() -> Unit): EmbeddedScript {
        return embeddedScript(builderOp)
    }

    /** @inheritDoc */
    override fun assetCatalog(): EmbeddedBundleSpec.() -> Unit = {
        module = libraryName
        filename = outputBundleName.get()
        language = EmbeddedScriptLanguage.JS
        lastModified = Timestamp.newBuilder().setSeconds(Instant.now().epochSecond).build()
        metadata = embeddedScriptMetadata {
            javascript = jsScriptMetadata {
                level = EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel.ES2020
            }
        }
    }

    /** @inheritDoc */
    override fun runAction() {
        processShim.get().writeText(
            renderTemplateVals(processShimTemplate)
        )
        outputConfig.get().writeText(
            renderTemplateVals(configTemplate)
        )
        logger.lifecycle(
            "Config generated for `${tool.name.lowercase()}` (mode: ${mode.name}): " + outputConfig.get().path
        )
    }
}
