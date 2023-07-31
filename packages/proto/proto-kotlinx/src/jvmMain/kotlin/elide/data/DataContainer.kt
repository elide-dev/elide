@file:OptIn(ExperimentalSerializationApi::class)
@file:Suppress("RedundantVisibilityModifier")

package elide.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

/** Pure-Kotlin implementation of a raw data container. */
@Serializable internal data class DataContainer constructor(
  /**
   * TBD.
   */
  @ProtoNumber(1) public val raw: ByteArray,

  /**
   * TBD.
   */
  @ProtoNumber(2) public val integrity: DataFingerprint?,

  /**
   * TBD.
   */
  @ProtoNumber(3) public val encoding: Encoding?,
) {
  /** @inheritDoc */
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DataContainer

    if (integrity != other.integrity) return false
    if (encoding != other.encoding) return false
    if (integrity == null) return false  // not comparable without fingerprint

    return true
  }

  /** @inheritDoc */
  override fun hashCode(): Int {
    var result = integrity?.hashCode() ?: 0
    result = 31 * result + encoding.hashCode()
    return result
  }

  /** @inheritDoc */
  override fun toString(): String {
    return "DataContainer(size=${raw.size})"
  }
}
