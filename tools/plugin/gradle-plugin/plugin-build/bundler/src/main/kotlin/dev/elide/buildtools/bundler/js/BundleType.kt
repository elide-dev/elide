@file:Suppress("MemberVisibilityCanBePrivate")

package dev.elide.buildtools.bundler.js

/**
 * Enumerates types of bundles supported by the plugin, via the superset of supported formats between Webpack and
 * ESBuild.
 *
 * @param symbol Symbol to reference this format, by default.
 * @param supportedByEsbuild Whether ESBuild supports this bundle format.
 * @param supportedByWebpack Whether Webpack supports this bundle format.
 */
@Suppress("unused")
public enum class BundleType constructor(
    public val symbol: String,
    public val supportedByEsbuild: Boolean = true,
    public val supportedByWebpack: Boolean = true
) {
    /** Inline function definition and execution. */
    IIFE("iife"),

    /** CJS-based module loader. */
    COMMON_JS("cjs"),

    /** ESModules-based loading. */
    ESM("esm");

    companion object {
        public const val IIFE_NAME = "IIFE"
        public const val COMMON_JS_NAME = "COMMON_JS"
        public const val ESM_NAME = "ESM"
    }
}
