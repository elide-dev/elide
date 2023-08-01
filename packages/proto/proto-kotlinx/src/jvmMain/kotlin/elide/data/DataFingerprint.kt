@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("RedundantVisibilityModifier")

package elide.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Pure-Kotlin implementation of a data fingerprint container. */
@Serializable internal data class DataFingerprint constructor(
  /**
   * TBD.
   */
  @ProtoNumber(1) public val algorithm: HashAlgorithm,

  /**
   * TBD.
   */
  @ProtoNumber(2) public val salt: ByteArray?,

  /**
   * TBD.
   */
  @ProtoNumber(3) public val fingerprint: ByteArray,

  /**
   * TBD.
   */
  @ProtoNumber(4) public val encoding: Encoding,
) {
  /** @inheritDoc */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataFingerprint

    // salt is deliberately not considered here
    if (!fingerprint.contentEquals(other.fingerprint)) return false
    if (algorithm != other.algorithm) return false
    if (encoding != other.encoding) return false
    return true
  }

  /** @inheritDoc */
  override fun hashCode(): Int {
    var result = fingerprint.contentHashCode()
    result = 31 * result + algorithm.hashCode()
    result = 31 * result + encoding.hashCode()
    return result
  }
}
