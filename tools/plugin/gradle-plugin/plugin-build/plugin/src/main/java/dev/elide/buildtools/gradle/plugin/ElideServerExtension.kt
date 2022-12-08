package dev.elide.buildtools.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import javax.inject.Inject

/** Configures server-side features provided by the Elide plugin for Gradle. */
@Suppress("UnnecessaryAbstractClass")
public abstract class ElideServerExtension @Inject constructor(project: Project) {
    public companion object {
        /** Name of the Elide server extension. */
        public const val EXTENSION_NAME: String = "server"
    }

    /** Whether to enable VM inspection. */
    @get:Optional public val inspect: Property<Boolean> = project.objects.property(Boolean::class.java).value(false)

    /** Project to inject as an SSR application. */
    @get:Optional public abstract val ssrProject: Property<String>
}
