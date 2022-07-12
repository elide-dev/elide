package dev.elide.buildtools.bundler

/** Describes production and development build modes. */
@Suppress("unused")
public enum class BuildMode(public val minify: Boolean, public val prepack: Boolean) {
    /** Built for production, with minification turned on. */
    PRODUCTION(minify = true, prepack = true),

    /** Built for development, with minification turned off. */
    DEVELOPMENT(minify = false, prepack = false);

    companion object {
        public const val PRODUCTION_NAME = "PRODUCTION"
        public const val DEVELOPMENT_NAME = "DEVELOPMENT"
    }
}
