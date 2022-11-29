package dev.elide.buildtools.gradle.plugin.cfg

import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm

/** Hard-coded or constant values that relate to asset bundles. */
internal object StaticValues {
    internal const val currentVersion: Int = 2
    internal const val defaultHashRounds: Int = 1
    internal const val defaultTailSize: Int = 8
    internal val defaultEncoding: ManifestFormat = ManifestFormat.BINARY
    internal val assetHashAlgo: HashAlgorithm = HashAlgorithm.SHA256
}
