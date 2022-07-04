package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.js.BundleTarget
import dev.elide.buildtools.gradle.plugin.js.BundleTool
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/** */
@Suppress("UnnecessaryAbstractClass")
abstract class ElideEmbeddedJsExtension @Inject constructor(project: Project) {
    companion object {
        const val EXTENSION_NAME = "embedded"
    }

    private val objects = project.objects

    /** Operating build mode for a given plugin run. */
    @get:Optional val tool: Property<BundleTool> = objects.property(BundleTool::class.java)

    /** Target runtime for a given run of the embedded JS plugin. */
    @get:Optional val target: Property<BundleTarget> = objects.property(BundleTarget::class.java)

    /** Name of the target library. */
    @get:Optional val libraryName: Property<String> = objects.property(String::class.java)
}
