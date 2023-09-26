package dev.elide.buildtools.gradle.plugin.tasks

/** Versions of build tools depended on by the embedded JS build task. */
public object Versions {
    /** Embedded `esbuild` default version. */
    public const val esbuild: String = "0.18.17"

    /** Embedded `prepack` default version. */
    public const val prepack: String = "0.2.54"

    /** Embedded `buffer` default version. */
    public const val buffer: String = "6.0.3"

    /** Embedded `web-streams-polyfill` default version. */
    public const val webstreams: String = "3.2.1"
}
