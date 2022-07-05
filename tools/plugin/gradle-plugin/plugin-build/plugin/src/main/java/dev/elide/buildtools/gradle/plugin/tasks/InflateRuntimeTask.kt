package dev.elide.buildtools.gradle.plugin.tasks

import dev.elide.buildtools.gradle.plugin.ElideExtension
import dev.elide.buildtools.gradle.plugin.util.UntarUtil
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.StandardCopyOption

/** Task which inflates the packaged JS runtime into a build path where it can be referenced. */
abstract class InflateRuntimeTask : DefaultTask() {
    companion object {
        const val TASK_NAME = "inflateJsRuntime"
        private const val RUNTIME_PKG = "/dev/elide/buildtools/js/runtime/js-runtime.tar.gz"

        @JvmStatic fun install(extension: ElideExtension, project: Project): InflateRuntimeTask {
            return project.tasks.create(TASK_NAME, InflateRuntimeTask::class.java) {
                it.enableRuntime.set(extension.js.runtime.inject.get())
            }
        }
    }

    init {
        with(project) {
            destinationDirectory.set(
                file("${rootProject.buildDir}/js/elideRuntime")
            )
            modulesPath.set(
                file("${destinationDirectory.get()}/node_modules")
            )
            runtimePackage.set(
                InflateRuntimeTask::class.java.getResourceAsStream(
                    RUNTIME_PKG
                ) ?: throw FileNotFoundException(
                    "Failed to locate JS runtime package. This is an internal error; please report it to the Elide " +
                    "project at https://github.com/elide-dev/v3/issues."
                )
            )
        }
    }

    // Raw/unwrapped input stream for the runtime tarball package.
    @get:Internal
    abstract val runtimePackage: Property<InputStream>

    /** Output path prefix to use. */
    @get:OutputDirectory
    @get:Option(
        option = "destinationDirectory",
        description = "Where to write the inflated runtime package to",
    )
    abstract val destinationDirectory: DirectoryProperty

    /** Node modules path to inject for the runtime. */
    @get:OutputDirectory
    @get:Option(
        option = "modulesPath",
        description = "Path which should be used as a 'node_modules' entry for the JS runtime",
    )
    abstract val modulesPath: Property<File>

    /** Whether to enable runtime overrides. */
    @get:Input
    @get:Option(
        option = "enableRuntime",
        description = "Whether to enable Elide's JS runtime overrides",
    )
    abstract val enableRuntime: Property<Boolean>

    /**
     * Run the inflate-runtime action, by finding the runtime tarball and inflating it to the target [modulesPath].
     */
    @TaskAction fun inflateRuntime() {
        val nodeModsTarget = destinationDirectory.get().dir(
            "node_modules"
        ).asFile

        if (!nodeModsTarget.exists()) {
            // create the root
            nodeModsTarget.mkdirs()
        }

        // expand the tarball stream into it
        project.logger.info("Expanding packaged JS runtime...")
        UntarUtil.untar(
            runtimePackage.get(),
            nodeModsTarget,
            StandardCopyOption.REPLACE_EXISTING,
        )
    }
}
