package dev.elide.buildtools.gradle.plugin

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertNotNull
import org.junit.Test

class ElidePluginTest {
    @Test fun `extension is created correctly`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("dev.elide.buildtools.plugin")
        assertNotNull(project.extensions.getByName("elide"))
    }

    @Test fun `plugin is applied to the project without error`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("dev.elide.buildtools.plugin")
    }
}
