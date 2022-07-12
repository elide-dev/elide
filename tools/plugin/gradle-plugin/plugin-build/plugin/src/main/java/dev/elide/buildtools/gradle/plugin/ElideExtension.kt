package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.bundler.BuildMode
import dev.elide.buildtools.gradle.plugin.cfg.ElideJsHandler
import dev.elide.buildtools.gradle.plugin.cfg.ElideServerHandler
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Optional
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass", "unused", "RedundantVisibilityModifier", "MemberVisibilityCanBePrivate")
public open class ElideExtension @Inject constructor(project: Project) {
    private val objects = project.objects

    /** Configuration for JS runtime settings. */
    public val js: ElideJsHandler = objects.newInstance(ElideJsHandler::class.java)

    /** Configuration for server targets. */
    public val server: ElideServerHandler = objects.newInstance(ElideServerHandler::class.java)

    companion object {
        fun Project.elide(): ElideExtension {
            return extensions.create("elide", ElideExtension::class.java)
        }
    }

    /** Indicate whether a JS target was configured. */
    public fun hasJsTarget(): Boolean {
        return js.active.get()
    }

    /** Indicate whether a server target was configured. */
    public fun hasServerTarget(): Boolean {
        return server.active.get()
    }

    /** Closure to configure [ElideJsHandler] settings. */
    fun js(action: Action<ElideJsHandler>) {
        js.active.set(true)
        action.execute(js)
    }

    /** Closure to configure [ElideServerHandler] settings. */
    fun server(action: Action<ElideServerHandler>) {
        server.active.set(true)
        action.execute(server)
    }

    /** Operating build mode for a given plugin run. */
    @get:Optional public var mode: BuildMode = BuildMode.PRODUCTION
}
