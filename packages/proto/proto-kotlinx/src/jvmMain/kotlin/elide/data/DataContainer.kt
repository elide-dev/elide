/*
 * Copyright (c) 2023 Elide Ventures, LLC.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

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
