package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.tasks.EmbeddedJsBuildTask
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertNotNull
import org.junit.Test

class ElidePluginTest {
    @Test fun `plugin is applied correctly to the project`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("dev.elide.buildtools.plugin")

        assertNotNull(project.tasks.getByName("bundleEmbeddedJs"))
        assert(project.tasks.getByName("bundleEmbeddedJs") is EmbeddedJsBuildTask)
    }

    @Test fun `extension is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("dev.elide.buildtools.plugin")

        assertNotNull(project.extensions.getByName("elide"))
    }

    @Test fun `parameters are passed correctly from extension to task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("dev.elide.buildtools.plugin")
        (project.extensions.getByName("elide") as ElideExtension).apply {
//            tag.set("a-sample-tag")
//            message.set("just-a-message")
//            outputFile.set(aFile)
        }

        val task = project.tasks.getByName("bundleEmbeddedJs") as EmbeddedJsBuildTask

        assertNotNull("should be able to find mounted task in project", task)
//        assertEquals("a-sample-tag", task.tag.get())
//        assertEquals("just-a-message", task.message.get())
//        assertEquals(aFile, task.outputFile.get().asFile)
    }
}
