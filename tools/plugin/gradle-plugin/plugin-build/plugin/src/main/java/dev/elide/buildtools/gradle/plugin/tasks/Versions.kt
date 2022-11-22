package dev.elide.buildtools.gradle.plugin.tasks

/** Versions of build tools depended on by the embedded JS build task. */
public object Versions {
    /** Embedded `esbuild` default version. */
    public const val esbuild: String = "0.15.14"

    /** Embedded `prepack` default version. */
    public const val prepack: String = "0.2.54"
}
