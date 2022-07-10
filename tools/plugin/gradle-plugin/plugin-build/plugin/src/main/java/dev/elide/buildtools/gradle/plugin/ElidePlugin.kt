package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.ElideExtension.Companion.elide
import dev.elide.buildtools.gradle.plugin.tasks.BundleAssetsBuildTask
import dev.elide.buildtools.gradle.plugin.tasks.EmbeddedJsBuildTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Definition for the main Elide build-tools plugin. */
@Suppress("unused")
abstract class ElidePlugin : Plugin<Project> {
    companion object {
        const val EXTENSION_NAME = "elide"
    }

    override fun apply(project: Project) = project.run {
        var kotlinPluginFound = false
        val elide = elide()

        // if the embedded JS plugin can be applied (node context, kotlin JS, etc), then do that
        if (EmbeddedJsBuildTask.isEligible(elide, project)) {
            kotlinPluginFound = true
            EmbeddedJsBuildTask.install(
                elide,
                project
            )
        }

        // if the server-side JVM plugin can be applied (asset context) then do that
        if (BundleAssetsBuildTask.isEligible(elide, project)) {
            kotlinPluginFound = true
            BundleAssetsBuildTask.install(
                elide,
                project
            )
        }

        // kotlin MPP isn't supported yet, but it counts as a plugin
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            kotlinPluginFound = true
            project.logger.warn(
                "Elide doesn't support JS targets in Kotlin MPP modules. Build plugin will have no effect."
            )
        }

        if (!kotlinPluginFound) project.logger.warn(
            "Please apply a Kotlin plugin to use the Elide plugin (`js` or `jvm`)."
        )
    }
}
