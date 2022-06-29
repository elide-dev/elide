package dev.elide.buildtools.gradle.plugin

/** Describes production and development build modes. */
@Suppress("unused")
enum class BuildMode(internal val minify: Boolean) {
    /** Built for production, with minification turned on. */
    PRODUCTION(minify = true),

    /** Built for development, with minification turned off. */
    DEVELOPMENT(minify = false);

    companion object {
        const val PRODUCTION_NAME = "PRODUCTION"
        const val DEVELOPMENT_NAME = "DEVELOPMENT"
    }
}
