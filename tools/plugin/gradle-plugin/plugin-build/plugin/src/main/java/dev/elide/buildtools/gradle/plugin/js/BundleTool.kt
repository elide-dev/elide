package dev.elide.buildtools.gradle.plugin.js

/** Enumerates tools which may be used to perform JS bundling. */
@Suppress("unused")
public enum class BundleTool {
    /** `esbuild`: https://esbuild.github.io/ */
    ESBUILD,

    /** `webpack`: https://webpack.js.org/ */
    WEBPACK;

    public companion object {
        /** Name of the `esbuild` tool. */
        public const val ESBUILD_NAME: String = "ESBUILD"

        /** Name of the `webpack` tool. */
        public const val WEBPACK_NAME: String = "WEBPACK"
    }
}
