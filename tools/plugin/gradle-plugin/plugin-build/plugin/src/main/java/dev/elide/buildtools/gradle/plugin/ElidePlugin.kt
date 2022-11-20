package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.ElideExtension.Companion.elide
import dev.elide.buildtools.gradle.plugin.tasks.BundleAssetsBuildTask
import dev.elide.buildtools.gradle.plugin.tasks.EmbeddedJsBuildTask
import dev.elide.buildtools.gradle.plugin.tasks.GenerateStaticSiteTask
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
            elide.multiplatform.set(false)
            EmbeddedJsBuildTask.install(
                elide,
                project,
            )
        }

        // if the server-side JVM plugin can be applied (asset context) then do that
        if (BundleAssetsBuildTask.isEligible(elide, project)) {
            kotlinPluginFound = true
            elide.multiplatform.set(false)
            BundleAssetsBuildTask.install(
                elide,
                project,
            )
        }

        // kotlin MPP isn't supported yet, but it counts as a plugin
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            kotlinPluginFound = true
            elide.multiplatform.set(true)
            project.logger.warn(
                "Elide support for multiplatform modules is experimental. Proceed with caution."
            )
        }

        // we un-conditionally call `install` on this task because it will detect on its own if the project is eligible
        // to be built with the SSG compiler.
        if (kotlinPluginFound) GenerateStaticSiteTask.install(
            elide,
            project,
        )
        if (!kotlinPluginFound) project.logger.warn(
            "Please apply a Kotlin plugin to use the Elide plugin (`js` or `jvm`)."
        )
    }
}
