package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.ElideExtension.Companion.elide
import dev.elide.buildtools.gradle.plugin.tasks.BundleAssetsBuildTask
import dev.elide.buildtools.gradle.plugin.tasks.ElideDependencies.installApplyKSP
import dev.elide.buildtools.gradle.plugin.tasks.ElideDependencies.installCommonLibs
import dev.elide.buildtools.gradle.plugin.tasks.ElideDependencies.installElideProcessor
import dev.elide.buildtools.gradle.plugin.tasks.ElideDependencies.installJavaPlatform
import dev.elide.buildtools.gradle.plugin.tasks.ElideDependencies.installServerLibs
import dev.elide.buildtools.gradle.plugin.tasks.EmbeddedJsBuildTask
import dev.elide.buildtools.gradle.plugin.tasks.GenerateStaticSiteTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Definition for the main Elide build-tools plugin. */
@Suppress("unused")
public abstract class ElidePlugin : Plugin<Project> {
    public companion object {
        public const val EXTENSION_NAME: String = "elide"
    }

    /** @inheritDoc */
    override fun apply(project: Project): Unit = project.run {
        var kotlinPluginFound = false
        var isKotlinJVM = false
        var isKotlinMPP = false
        var isKotlinJS = false
        val elide = elide()

        // kotlin MPP isn't supported yet, but it counts as a plugin
        if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
            kotlinPluginFound = true
            isKotlinMPP = true
            elide.multiplatform.set(true)
        } else if (project.plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
            kotlinPluginFound = true
            isKotlinJVM = true
            elide.multiplatform.set(false)
        } else if (project.plugins.hasPlugin("org.jetbrains.kotlin.js")) {
            kotlinPluginFound = true
            isKotlinJS = true
            elide.multiplatform.set(false)
        }

        // always make sure KSP is installed.
        if (isKotlinJVM) project.installApplyKSP()

        project.afterEvaluate {
            // if we're instructed to configure the build, it's time to install common Kotlin libraries, and we'll
            // also need the Elide processor for KSP.
            if (isKotlinJVM) project.installElideProcessor()

            // install baseline project dependencies, as demanded by configuration
            val shouldConfig = elide.shouldConfigureBuild()
            if (kotlinPluginFound && shouldConfig) {
                // if there is a server target configuration, install base server libs.
                if (elide.hasServerTarget()) project.installServerLibs()

                // if we are configuring a JVM project, install the Java Platform library, which will make sure lib
                // versions remain consistent throughout.
                if (isKotlinJVM) project.installJavaPlatform()
            }

            // install the SSG compiler task, which will avoid running if the project is not eligible.
            if (kotlinPluginFound && !isKotlinJS) {
                GenerateStaticSiteTask.install(
                    elide,
                    project,
                )
            }
        }

        // if the embedded JS plugin can be applied (node context, kotlin JS, etc), then do that.
        if (EmbeddedJsBuildTask.isEligible(elide, project)) {
            EmbeddedJsBuildTask.install(
                elide,
                project,
            )
        } else if (BundleAssetsBuildTask.isEligible(elide, project)) {
            // if the server-side JVM plugin can be applied (asset context) then do that.
            BundleAssetsBuildTask.install(
                elide,
                project,
            )
        }
        if (!kotlinPluginFound) project.logger.warn(
            "Please apply a Kotlin plugin to use the Elide plugin (`js` or `jvm`)."
        )
        if (isKotlinMPP) project.logger.warn(
            "Elide support for multiplatform modules is experimental. Proceed with caution."
        )
    }
}
