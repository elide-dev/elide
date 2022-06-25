package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.tasks.EmbeddedJsBuildTask
import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
abstract class ElidePlugin : Plugin<Project> {
    companion object {
        const val EXTENSION_NAME = "elide"
        const val TASK_NAME = "bundleEmbeddedJs"
    }

    override fun apply(project: Project) {
        // Add the 'template' extension object
        project.extensions.create(EXTENSION_NAME, ElideExtension::class.java, project)

        // Add a task that uses configuration from the extension object
        project.tasks.register(TASK_NAME, EmbeddedJsBuildTask::class.java) {
//            it.tag.set(extension.tag)
//            it.message.set(extension.message)
//            it.outputFile.set(extension.outputFile)
        }
    }
}
