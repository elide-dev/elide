package dev.elide.buildtools.gradle.plugin.cfg

/** Models an asset dependency. */
internal data class AssetDependency(
    internal val dependent: AssetModuleId,
    internal val dependee: AssetModuleId,
    internal val direct: Boolean = true,
    internal val devOnly: Boolean = false,
    internal val testOnly: Boolean = false,
)
