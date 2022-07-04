package dev.elide.buildtools.gradle.plugin

/** Describes production and development build modes. */
@Suppress("unused")
enum class BuildMode(internal val minify: Boolean, internal val prepack: Boolean) {
    /** Built for production, with minification turned on. */
    PRODUCTION(minify = true, prepack = true),

    /** Built for development, with minification turned off. */
    DEVELOPMENT(minify = false, prepack = false);

    companion object {
        const val PRODUCTION_NAME = "PRODUCTION"
        const val DEVELOPMENT_NAME = "DEVELOPMENT"
    }
}
