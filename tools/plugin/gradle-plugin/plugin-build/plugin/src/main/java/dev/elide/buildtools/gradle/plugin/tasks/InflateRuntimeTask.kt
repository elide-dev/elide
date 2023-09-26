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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.file.StandardCopyOption

/** Task which inflates the packaged JS runtime into a build path where it can be referenced. */
public abstract class InflateRuntimeTask : DefaultTask() {
    public companion object {
        /** Name of the inflate-runtime task. */
        public const val TASK_NAME: String = "inflateJsRuntime"

        /** Where we should be able to find the packaged JS runtime. */
        private const val RUNTIME_PKG: String = "/dev/elide/buildtools/js/runtime/js-runtime.tar.gz"

        /** Install the [InflateRuntimeTask] on the provided [extension] and [project]. */
        @JvmStatic public fun install(extension: ElideExtension, project: Project): InflateRuntimeTask {
            return project.tasks.create(TASK_NAME, InflateRuntimeTask::class.java) {
                it.enableRuntime.set(extension.js.runtime.inject.get())
            }
        }
    }

    init {
        with(project) {
            destinationDirectory.set(
                file(layout.buildDirectory.dir("js/elideRuntime"))
            )
            modulesPath.set(
                file("${destinationDirectory.get()}/node_modules")
            )
            runtimePackage.set(
                InflateRuntimeTask::class.java.getResourceAsStream(
                    RUNTIME_PKG
                ) ?: throw FileNotFoundException(
                    "Failed to locate JS runtime package. This is an internal error; please report it to the Elide " +
                    "team at https://github.com/elide-dev/elide/issues."
                )
            )
        }
    }

    // Raw/unwrapped input stream for the runtime tarball package.
    @get:Internal
    internal abstract val runtimePackage: Property<InputStream>

    /** Output path prefix to use. */
    @get:OutputDirectory
    @get:Option(
        option = "destinationDirectory",
        description = "Where to write the inflated runtime package to",
    )
    internal abstract val destinationDirectory: DirectoryProperty

    /** Node modules path to inject for the runtime. */
    @get:OutputDirectory
    @get:Option(
        option = "modulesPath",
        description = "Path which should be used as a 'node_modules' entry for the JS runtime",
    )
    internal abstract val modulesPath: Property<File>

    /** Whether to enable runtime overrides. */
    @get:Input
    @get:Option(
        option = "enableRuntime",
        description = "Whether to enable Elide's JS runtime overrides",
    )
    internal abstract val enableRuntime: Property<Boolean>

    /**
     * Run the inflate-runtime action, by finding the runtime tarball and inflating it to the target [modulesPath].
     */
    @TaskAction public fun inflateRuntime() {
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
