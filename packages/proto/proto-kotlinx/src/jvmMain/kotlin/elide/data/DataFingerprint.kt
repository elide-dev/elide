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

  override fun hashCode(): Int {
    var result = fingerprint.contentHashCode()
    result = 31 * result + algorithm.hashCode()
    result = 31 * result + encoding.hashCode()
    return result
  }
}
