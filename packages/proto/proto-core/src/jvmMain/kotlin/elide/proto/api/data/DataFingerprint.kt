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

@file:Suppress("RedundantVisibilityModifier")

package elide.proto.api.data

import elide.core.encoding.base64.Base64Data
import elide.core.encoding.hex.HexData
import elide.proto.api.Record

/**
 * TBD.
 */
public interface DataFingerprint<Concrete, Builder, Algorithm, Encoding> :
  Record<DataFingerprint<Concrete, Builder, Algorithm, Encoding>, Builder>
  where Builder : DataFingerprint.IBuilder<Concrete, Algorithm, Encoding, Builder>,
        Algorithm : Enum<Algorithm>,
        Encoding : Enum<Encoding> {
  /**
   * TBD.
   */
  public interface IBuilder<Concrete, Algorithm, Encoding, B: IBuilder<Concrete, Algorithm, Encoding, B>> :
    Record.IBuilder<Concrete>
    where Encoding: Enum<Encoding>,
          Algorithm : Enum<Algorithm> {
    /**
     * TBD.
     */
    public var fingerprint: ByteArray

    /**
     * TBD.
     */
    public var algorithm: Algorithm

    /**
     * TBD.
     */
    public var encoding: Encoding

    /**
     * TBD.
     */
    public fun setFingerprint(data: ByteArray): B

    /**
     * TBD.
     */
    public fun setFingerprint(data: ByteArray, withAlgorith: Algorithm): B

    /**
     * TBD.
     */
    public fun setFingerprint(value: ByteArray, withAlgorith: Algorithm, withEncoding: Encoding): B

    /**
     * TBD.
     */
    public fun setAlgorithm(value: Algorithm): B

    /**
     * TBD.
     */
    public fun setEncoding(value: Encoding): B

    /**
     * TBD.
     */
    public override fun build(): Concrete
  }

  /**
   * TBD.
   */
  public interface Factory<Concrete, Builder, Algorithm, Encoding> : Record.Factory<Concrete, Builder>
    where Algorithm : Enum<Algorithm>,
          Encoding : Enum<Encoding> {
    /**
     * TBD.
     */
    fun create(algorithm: Algorithm, data: ByteArray): Concrete

    /**
     * TBD.
     */
    fun create(algorithm: Algorithm, base64: Base64Data): Concrete

    /**
     * TBD.
     */
    fun create(algorithm: Algorithm, hex: HexData): Concrete

    /**
     * TBD.
     */
    fun create(algorithm: Algorithm, data: String): Concrete

    /**
     * TBD.
     */
    fun create(algorithm: Algorithm, data: String, encoding: Encoding): Concrete
  }

  /**
   * TBD.
   */
  fun bytes(): ByteArray

  /**
   * TBD.
   */
  fun encoding(): Encoding

  /**
   * TBD.
   */
  fun algorithm(): Algorithm
}
