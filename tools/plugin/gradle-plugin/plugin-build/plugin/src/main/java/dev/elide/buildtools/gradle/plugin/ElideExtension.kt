package dev.elide.buildtools.gradle.plugin

import dev.elide.buildtools.gradle.plugin.cfg.ElideJsHandler
import dev.elide.buildtools.gradle.plugin.cfg.ElideKotlinPluginsHandler
import dev.elide.buildtools.gradle.plugin.cfg.ElideServerHandler
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Optional
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("UnnecessaryAbstractClass", "unused", "RedundantVisibilityModifier", "MemberVisibilityCanBePrivate")
public open class ElideExtension @Inject constructor(project: Project) {
    private val objects = project.objects
    internal val multiplatform: AtomicBoolean = AtomicBoolean(false)

    /** Version pin for Elide, plugins, and tooling. */
    public val version: Property<String> = objects.property(String::class.java)

    /** Configuration for JS runtime settings. */
    public val js: ElideJsHandler = objects.newInstance(ElideJsHandler::class.java)

    /** Configuration for server targets. */
    public val server: ElideServerHandler = objects.newInstance(ElideServerHandler::class.java)

    /** Configuration for Kotlin plugins. */
    public val kotlinPluginOptions: ElideKotlinPluginsHandler = objects.newInstance(
        ElideKotlinPluginsHandler::class.java
    )

    /** Static methods provided by the Elide extension. */
    public companion object {
        /** Configure the Elide plugin for the receiver [Project]. */
        public fun Project.elide(): ElideExtension {
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
    public fun js(action: Action<ElideJsHandler>) {
        js.active.set(true)
        action.execute(js)
    }

    /** Closure to configure [ElideServerHandler] settings. */
    public fun server(action: Action<ElideServerHandler>) {
        server.active.set(true)
        action.execute(server)
    }

    /** Closure to configure [ElideKotlinPluginsHandler] settings. */
    public fun kotlinPlugins(action: Action<ElideKotlinPluginsHandler>) {
        action.execute(kotlinPluginOptions)
    }

    /** Operating build mode for a given plugin run. */
    @get:Optional public var mode: BuildMode = BuildMode.PRODUCTION
}
