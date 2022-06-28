package dev.elide.buildtools.gradle.plugin.js

/** Enumerates tools which may be used to perform JS bundling. */
@Suppress("unused")
enum class BundleTool {
    /** `esbuild`: https://esbuild.github.io/ */
    ESBUILD,

    /** `webpack`: https://webpack.js.org/ */
    WEBPACK;

    companion object {
        const val ESBUILD_NAME = "ESBUILD"
        const val WEBPACK_NAME = "WEBPACK"
    }
}
