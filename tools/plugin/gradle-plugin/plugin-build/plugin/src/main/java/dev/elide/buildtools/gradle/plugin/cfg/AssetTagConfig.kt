package dev.elide.buildtools.gradle.plugin.cfg

import tools.elide.crypto.HashAlgorithm

/** Configuration specific to the asset tag generator. */
data class AssetTagConfig(
    /** Hash algorithm to use for generating tags. */
    val hashAlgorithm: HashAlgorithm,

    /** Size, in pre-image bytes, to take from the end of the hash. */
    val tailSize: Int,

    /** Rounds of digest activity to apply. */
    val rounds: Int,
) : java.io.Serializable
