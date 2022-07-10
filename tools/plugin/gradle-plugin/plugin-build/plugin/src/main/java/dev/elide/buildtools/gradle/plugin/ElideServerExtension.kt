package dev.elide.buildtools.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/** */
@Suppress("UnnecessaryAbstractClass")
abstract class ElideServerExtension @Inject constructor(project: Project) {
    companion object {
        const val EXTENSION_NAME = "server"
    }

    private val objects = project.objects

    /** Operating build mode for a given plugin run. */
    @get:Optional val inspect: Property<Boolean> = objects.property(Boolean::class.java).value(
        false
    )

    /** Project to inject as an SSR application. */
    @get:Optional val ssrProject: Property<String> = objects.property(String::class.java)
}
