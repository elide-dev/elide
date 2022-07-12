package dev.elide.buildtools.bundler.cfg

import tools.elide.data.CompressionMode
import java.util.EnumSet

/** Describes asset compression settings. */
data class AssetCompressionConfig(
    /** Whether to enable asset compression. */
    val enabled: Boolean,

    /** Compression modes to enable. [CompressionMode.IDENTITY] is implied and does not need to be specified. */
    val modes: EnumSet<CompressionMode>,

    /** Minimum file size to be eligible for compression. */
    val minimumSize: Int,

    /** When `true`, only keep the smallest variant. [CompressionMode.IDENTITY] is always kept. */
    val keepBest: Boolean,

    /** When `true`, keep all variants unconditionally. */
    val force: Boolean,
) : java.io.Serializable
