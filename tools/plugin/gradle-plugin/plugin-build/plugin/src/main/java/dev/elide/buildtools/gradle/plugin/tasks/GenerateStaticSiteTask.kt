package dev.elide.buildtools.gradle.plugin.tasks

import dev.elide.buildtools.gradle.plugin.ElideExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

/**
 * # Task: Generate Static Site
 *
 * This task implements the Static Site Generator (SSG) tooling for the Elide plugin. It is responsible for executing
 * built app handlers to produce a static site at build time, based on the manifest created by the KSP-based route
 * processor.
 */
public abstract class GenerateStaticSiteTask : DefaultTask() {
    public companion object {
        private const val TASK_NAME: String = "generateStaticSite"
        private const val KSP_TASK: String = "kspKotlin"

        // After determining the SSG build is eligible to run, apply plugins, then build/install tasks.
        @JvmStatic public fun install(extension: ElideExtension, project: Project) {
            project.afterEvaluate {
                installIfEligible(extension, project)
            }
        }

        // Determine whether the SSG compiler can run, and if so, install it.
        @JvmStatic private fun installIfEligible(extension: ElideExtension, project: Project) {
            if (!extension.server.hasSsgConfig()) {
                return // no need to subscribe
            }
            project.pluginManager.withPlugin("com.google.devtools.ksp") {
                val producer = project.tasks.findByName(KSP_TASK)
                val assetBuilder = project.tasks.findByName(BundleAssetsBuildTask.TASK_NAME)

                when {
                    // make sure KSP is installed and working
                    producer == null && extension.server.hasSsgConfig() -> error(
                        "Cannot run SSG compiler: KSP task not found. Please make sure KSP is installed and applied."
                    )

                    // make sure this task has an asset bundle target
                    assetBuilder == null -> error(
                        "Cannot run SSG compiler: asset builder task not found"
                    )

                    // otherwise we're ready to rock
                    else -> project.tasks.create(TASK_NAME, GenerateStaticSiteTask::class.java) {
                        it.description = "Generate a static site from this app"
                        it.group = "build"
                        it.enabled = extension.server.hasSsgConfig()
                        it.dependsOn.addAll(listOf(
                            producer,
                            assetBuilder,
                        ))
                    }
                }
            }
        }
    }

    /**
     * Run the action to fulfill SSG build settings, by generating a static site from the target application.
     */
    @TaskAction public fun runAction() {
        project.logger.lifecycle("Generating site via SSG...")
    }
}
