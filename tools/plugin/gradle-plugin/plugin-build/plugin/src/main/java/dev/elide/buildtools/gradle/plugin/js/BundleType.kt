@file:Suppress("MemberVisibilityCanBePrivate")

package dev.elide.buildtools.gradle.plugin.js

/**
 * Enumerates types of bundles supported by the plugin, via the superset of supported formats between Webpack and
 * ESBuild.
 *
 * @param symbol Symbol to reference this format, by default.
 * @param supportedByEsbuild Whether ESBuild supports this bundle format.
 * @param supportedByWebpack Whether Webpack supports this bundle format.
 */
@Suppress("unused")
enum class BundleType constructor(
    internal val symbol: String,
    internal val supportedByEsbuild: Boolean = true,
    internal val supportedByWebpack: Boolean = true
) {
    /** Inline function definition and execution. */
    IIFE("iife"),

    /** CJS-based module loader. */
    COMMON_JS("cjs"),

    /** ESModules-based loading. */
    ESM("esm");

    companion object {
        internal const val IIFE_NAME = "IIFE"
        internal const val COMMON_JS_NAME = "COMMON_JS"
        internal const val ESM_NAME = "ESM"
    }
}
