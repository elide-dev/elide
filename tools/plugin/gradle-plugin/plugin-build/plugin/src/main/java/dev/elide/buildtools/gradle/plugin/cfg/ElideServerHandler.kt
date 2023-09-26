package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.model.ObjectFactory
import tools.elide.assets.EmbeddedScriptLanguage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Elide JVM server target settings. */
@Suppress("MemberVisibilityCanBePrivate", "unused")
public open class ElideServerHandler @Inject constructor(objects: ObjectFactory) {
    private companion object {
        /** Default scripting language to apply. */
        private val defaultScriptLanguage = EmbeddedScriptLanguage.JS
    }

    /** Whether the user configured a server target in their build script. */
    public val active: AtomicBoolean = AtomicBoolean(false)

    /** Server-embedded asset configuration. */
    public val assets: ElideAssetsHandler = objects.newInstance(ElideAssetsHandler::class.java)

    /** Server embedded SSR configuration. */
    public val ssr: ServerSSRHandler = objects.newInstance(ServerSSRHandler::class.java)

    /** Server SSR runtime configuration. */
    public val ssrRuntime: AtomicReference<EmbeddedScriptLanguage> = AtomicReference(defaultScriptLanguage)

    /** @return True if the user has configured an SSR bundle from their build script. */
    public fun hasSsrBundle(): Boolean {
        return ssr.hasBundle()
    }

    /** @return Whether the user has configured assets */
    public fun hasAssets(): Boolean {
        return assets.active.get()
    }

    /** Configure server-embedded assets. */
    public fun assets(action: Action<ElideAssetsHandler>) {
        action.execute(assets)
    }

    /** Configure a JVM server target for SSR. */
    public fun ssr(language: EmbeddedScriptLanguage = defaultScriptLanguage, action: Action<ServerSSRHandler>) {
        ssrRuntime.set(language)
        action.execute(ssr)
    }

    /** Configures SSR features for Elide server targets. */
    public open class ServerSSRHandler {
        internal companion object {
            internal const val defaultSsrConfiguration: String = "elideSsrDist"
        }

        /** Name of the target project to pull assets from. */
        internal val targetProject: AtomicReference<String?> = AtomicReference(null)

        /** Name of the configuration, within [targetProject], to pull assets from. */
        internal val targetConfiguration: AtomicReference<String?> = AtomicReference(defaultSsrConfiguration)

        // Indicate whether a bundle has been configured.
        internal fun hasBundle(): Boolean {
            return (
                targetProject.get()?.isNotBlank() == true &&
                targetConfiguration.get()?.isNotBlank() == true
            )
        }

        /** Inject the specified JS bundle as an SSR application script. */
        public fun bundle(project: Project, configuration: String = defaultSsrConfiguration) {
            targetProject.set(project.path)
            targetConfiguration.set(configuration)
        }

        /** Inject the specified JS bundle as an SSR application script, using a symbolic project dependency. */
        public fun bundle(
            accessor: DelegatingProjectDependency,
            configuration: String = defaultSsrConfiguration,
        ): Unit = accessor.dependencyProject.let { project ->
            targetProject.set(project.path)
            targetConfiguration.set(configuration)
        }
    }
}
