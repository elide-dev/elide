package dev.elide.buildtools.gradle.plugin.cfg

import dev.elide.buildtools.bundler.js.BundleTarget
import dev.elide.buildtools.bundler.js.BundleTool
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import tools.elide.assets.EmbeddedScriptMetadata.JsScriptMetadata.JsLanguageLevel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/** Elide JavaScript settings. */
@Suppress("RedundantVisibilityModifier", "MemberVisibilityCanBePrivate", "unused")
open class ElideJsHandler @Inject constructor(
    objects: ObjectFactory
) {
    /** Whether the user added a JS target in their build script. */
    internal val active: AtomicBoolean = AtomicBoolean(false)

    /** Tool that should be used for bundling. */
    internal val bundleTool = AtomicReference(BundleTool.ESBUILD)

    /** Target platform for the embedded bundle. */
    internal val bundleTarget = AtomicReference(BundleTarget.EMBEDDED)

    /** Target library name. */
    internal val libraryName: AtomicReference<String> = AtomicReference("embedded")

    /** Settings which relate to the JS runtime. */
    internal val runtime: ElideJsRuntimeHandler = objects.newInstance(ElideJsRuntimeHandler::class.java)

    /** Whether to minify built bundles. */
    internal val minify: AtomicReference<Boolean> = AtomicReference(null)

    /** Whether to pre-pack built bundles. */
    internal val prepack: AtomicReference<Boolean> = AtomicReference(null)

    /** Set the [BundleTool] used to build embedded JS. */
    public fun tool(bundleTool: BundleTool) {
        this.bundleTool.set(bundleTool)
    }

    /** Set the [platform] where this bundle target will run. */
    public fun target(platform: BundleTarget) {
        this.bundleTarget.set(platform)
    }

    /** Configure the JS runtime provided by Elide. */
    public fun runtime(action: Action<ElideJsRuntimeHandler>) {
        action.execute(runtime)
    }

    /** Set the name of the embedded library. */
    public fun libraryName(name: String) {
        libraryName.set(name)
    }

    /** Set whether bundles should be minified. If unspecified, defaults to `true` in `PRODUCTION` mode. */
    public fun minify(enable: Boolean) {
        this.minify.set(enable)
    }

    /** Set whether bundles should be pre-packed. If unspecified, defaults to `true` in `PRODUCTION` mode. */
    public fun prepack(enable: Boolean) {
        this.prepack.set(enable)
    }

    /** Elide JS runtime settings. */
    open class ElideJsRuntimeHandler {
        /** Language level to expect/apply for the embedded script. */
        internal val inject = AtomicBoolean(true)

        /** Language level to expect/apply for the embedded script. */
        internal val languageLevel = AtomicReference(JsLanguageLevel.ES2020)

        /** Whether to enable the injected JS runtime. */
        public fun inject(enable: Boolean) {
            this.inject.set(enable)
        }

        /** Set the JavaScript language [level] expected for this script. Defaults to `ES2020`. */
        public fun languageLevel(level: JsLanguageLevel) {
            this.languageLevel.set(level)
        }
    }
}
