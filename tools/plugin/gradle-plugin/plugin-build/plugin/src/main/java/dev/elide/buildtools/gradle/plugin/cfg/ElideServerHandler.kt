package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import tools.elide.assets.EmbeddedScriptLanguage
import java.util.SortedSet
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import elide.tool.ssg.SiteCompilerParams.Options as CompilerOptions

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

    /** Static site generator (SSG) configuration. */
    public val ssg: StaticSiteHandler = objects.newInstance(StaticSiteHandler::class.java)

    /** Server SSR runtime configuration. */
    public val ssrRuntime: AtomicReference<EmbeddedScriptLanguage> = AtomicReference(defaultScriptLanguage)

    /** @return True if the user has configured an SSR bundle from their build script. */
    public fun hasSsrBundle(): Boolean {
        return ssr.hasBundle()
    }

    /** @return True if the user has configured an SSG target from their build script. */
    public fun hasSsgConfig(): Boolean {
        return ssg.enabled.get()
    }

    /** @return Whether the user has configured assets */
    public fun hasAssets(): Boolean {
        return assets.active.get()
    }

    /** Configure SSG compilation pass. */
    public fun ssg(action: Action<StaticSiteHandler>) {
        ssg.enabled.set(true)
        action.execute(ssg)
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
            internal const val defaultSsrConfiguration: String = "nodeSsrDist"
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
    }

    /** Configures SSG (static site generator) features for Elide server targets. */
    public open class StaticSiteHandler {
        /** Whether the user configured a static site target in their build script. */
        internal val enabled: AtomicBoolean = AtomicBoolean(false)

        /** Explicit manifest path; if not provided, one will be calculated/resolved. */
        internal val manifest: AtomicReference<String?> = AtomicReference(null)

        /** Explicit output path; if not provided, one will be calculated. */
        internal val output: AtomicReference<String?> = AtomicReference(null)

        /** Explicit target app path; if not provided, one will be calculated. */
        internal val target: AtomicReference<String?> = AtomicReference(null)

        /** Whether to run the SSG site task in verbose mode. */
        internal val verbose: AtomicBoolean = AtomicBoolean(CompilerOptions.DEFAULTS.verbose)

        /** Whether to run the SSG site task in debug mode. */
        internal val debug: AtomicBoolean = AtomicBoolean(CompilerOptions.DEFAULTS.debug)

        /** Whether to allow colorized/dynamic output. */
        internal val pretty: AtomicBoolean = AtomicBoolean(CompilerOptions.DEFAULTS.pretty)

        /** Whether to crawl for additional assets. */
        internal val crawl: AtomicBoolean = AtomicBoolean(CompilerOptions.DEFAULTS.crawl)

        /** Response timeout to apply when operating in HTTP mode. */
        internal val timeout: AtomicInteger = AtomicInteger(CompilerOptions.DEFAULT_REQUEST_TIMEOUT)

        /** Response timeout to apply when operating in HTTP mode. */
        internal val extraOrigins: SortedSet<String> = ConcurrentSkipListSet()

        /** Whether to ignore cert errors when operating in HTTP mode. */
        internal val ignoreCertErrors: AtomicBoolean = AtomicBoolean(CompilerOptions.DEFAULTS.ignoreCertErrors)

        /** Enable a static site build run for a given server target. */
        public fun enable() {
            enabled.set(true)
        }

        /** Disable a static site build run for a given server target. */
        public fun disable() {
            enabled.set(false)
        }
    }
}
