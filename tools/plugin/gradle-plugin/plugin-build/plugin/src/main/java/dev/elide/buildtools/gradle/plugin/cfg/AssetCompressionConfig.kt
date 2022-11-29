package dev.elide.buildtools.gradle.plugin.cfg

import tools.elide.data.CompressionMode
import java.util.EnumSet

/** Describes asset compression settings. */
internal data class AssetCompressionConfig(
    /** Whether to enable asset compression. */
    internal val enabled: Boolean,

    /** Compression modes to enable. [CompressionMode.IDENTITY] is implied and does not need to be specified. */
    internal val modes: EnumSet<CompressionMode>,

    /** Minimum file size to be eligible for compression. */
    internal val minimumSize: Int,

    /** When `true`, only keep the smallest variant. [CompressionMode.IDENTITY] is always kept. */
    internal val keepBest: Boolean,

    /** When `true`, keep all variants unconditionally. */
    internal val force: Boolean,
) : java.io.Serializable
