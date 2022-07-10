package dev.elide.buildtools.gradle.plugin.cfg

import org.gradle.api.file.CopySpec
import java.util.SortedSet

/** Configured and resolved information for an embedded server-side asset. */
data class AssetInfo(
    /** Name of the module implemented by this asset. */
    val module: AssetModuleId,

    /** Type of asset symbolized by this record. */
    val type: AssetType,

    /** Direct module dependencies. */
    val directDeps: SortedSet<AssetModuleId>,

    /** Registered paths for this asset module -- all must be distinct across all [AssetInfo] entries. */
    val paths: SortedSet<String>,

    /** Copy specification for this module's sources. */
    @Transient val copySpec: CopySpec,
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
