package dev.elide.buildtools.gradle.plugin.js

/**
 * Enumerates the types of bundle targets supported by the plugin; besides granting symbols to these names, this enum
 * resolves to a default [BundleType] for each target type.
 *
 * @param platform Platform name to use when invoking ESBuild.
 * @param bundleType Default bundle type to use for this target type.
 * @param bundleTool Default tool to use for this bundle target type.
 */
@Suppress("unused")
enum class BundleTarget(
    internal val platform: String,
    internal val bundleType: BundleType,
    internal val bundleTool: BundleTool,
) {
    /** The bundle is being assembled for use in an embedded (VM) context. */
    EMBEDDED("neutral", BundleType.IIFE, BundleTool.ESBUILD),

    /** The bundle is being assembled for use with NodeJS. */
    NODE("node", BundleType.COMMON_JS, BundleTool.ESBUILD),

    /** The bundle is being assembled for use with web browsers. */
    WEB("browser", BundleType.IIFE, BundleTool.WEBPACK);

    companion object {
        internal const val EMBEDDED_NAME = "EMBEDDED"
        internal const val NODE_NAME = "NODE"
        internal const val WEB_NAME = "WEB"
    }
}
