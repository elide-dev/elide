package dev.elide.buildtools.gradle.plugin.cfg

import tools.elide.assets.ManifestFormat
import tools.elide.crypto.HashAlgorithm

/** Hard-coded or constant values that relate to asset bundles. */
public object StaticValues {
    const val currentVersion: Int = 2
    const val defaultHashRounds: Int = 1
    const val defaultTailSize: Int = 8
    val defaultEncoding: ManifestFormat = ManifestFormat.BINARY
    val assetHashAlgo: HashAlgorithm = HashAlgorithm.SHA256
}
