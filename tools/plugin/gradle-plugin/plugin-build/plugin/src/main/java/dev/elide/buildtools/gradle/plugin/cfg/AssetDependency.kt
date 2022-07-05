package dev.elide.buildtools.gradle.plugin.cfg

/** Models an asset dependency. */
data class AssetDependency(
    val dependent: AssetModuleId,
    val dependee: AssetModuleId,
    val direct: Boolean = true,
    val devOnly: Boolean = false,
    val testOnly: Boolean = false,
)
