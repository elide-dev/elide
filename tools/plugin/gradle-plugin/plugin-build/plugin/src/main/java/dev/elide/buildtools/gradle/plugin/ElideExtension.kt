package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.cfg.JsRuntimeConfig
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass", "unused", "RedundantVisibilityModifier")
public abstract class ElideExtension @Inject constructor(project: Project) {
    private val objects = project.objects

    /** Operating build mode for a given plugin run. */
    @get:Optional public val mode: Property<BuildMode> = objects.property(BuildMode::class.java).value(
        BuildMode.PRODUCTION
    )

    /** JavaScript runtime configuration. */
    @get:Optional public val jsRuntime: Property<JsRuntimeConfig> = objects.property(JsRuntimeConfig::class.java).value(
        JsRuntimeConfig()
    )
}
