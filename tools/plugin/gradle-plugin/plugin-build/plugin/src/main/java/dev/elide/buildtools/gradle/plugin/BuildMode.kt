package dev.elide.buildtools.gradle.plugin

/** Describes production and development build modes. */
@Suppress("unused")
public enum class BuildMode(internal val minify: Boolean, internal val prepack: Boolean) {
    /** Built for production, with minification turned on. */
    PRODUCTION(minify = true, prepack = true),

    /** Built for development, with minification turned off. */
    DEVELOPMENT(minify = false, prepack = false);

    public companion object {
        /** String name of the `PRODUCTION` build mode. */
        public const val PRODUCTION_NAME: String = "PRODUCTION"

        /** String name of the `DEVELOPMENT` build mode. */
        public const val DEVELOPMENT_NAME: String = "DEVELOPMENT"
    }
}
