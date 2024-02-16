package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.file.CopySpec
import java.util.*

/** Configured and resolved information for an embedded server-side asset. */
internal data class AssetInfo(
    /** Name of the module implemented by this asset. */
    internal val module: AssetModuleId,

    /** Type of asset symbolized by this record. */
    internal val type: AssetType,

    /** Direct module dependencies. */
    internal val directDeps: SortedSet<AssetModuleId>,

    /** Registered paths for this asset module -- all must be distinct across all [AssetInfo] entries. */
    internal val paths: SortedSet<String>,

    /** Gradle multi-module project dependencies related to this asset. Each pair is a `project`, `configuration`. */
    internal val projectDeps: List<ElideAssetsHandler.InterProjectAssetHandler>,

    /** Copy specification for this module's sources. */
    @Transient internal val copySpec: CopySpec,
) : java.io.Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AssetInfo
        if (module != other.module) return false
        return true
    }

    override fun hashCode(): Int {
        return module.hashCode()
    }
}
