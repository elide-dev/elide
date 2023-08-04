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
public interface DataContainer<Concrete, Builder, Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings> :
  Record<DataContainer<Concrete, Builder, Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings>, Builder>
  where Fingerprint : DataFingerprint<Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings>,
        FingerprintBuilder : DataFingerprint.IBuilder<Fingerprint, HashAlgorithms, Encodings,  FingerprintBuilder>,
        Builder : DataContainer.IBuilder<Concrete, Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings, Builder>,
        HashAlgorithms: Enum<HashAlgorithms>,
        Encodings : Enum<Encodings> {
  /**
   * TBD.
   */
  public interface IBuilder<
    Container,
    Fingerprint,
    FingerprintBuilder,
    HashAlgorithms,
    Encodings,
    B: IBuilder<Container, Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings, B>
  > : Record.IBuilder<Container>
    where Encodings: Enum<Encodings>,
          Fingerprint : DataFingerprint<Fingerprint, FingerprintBuilder, HashAlgorithms, Encodings>,
          FingerprintBuilder : DataFingerprint.IBuilder<Fingerprint, HashAlgorithms, Encodings, FingerprintBuilder>,
          HashAlgorithms: Enum<HashAlgorithms> {
    /**
     * TBD.
     */
    public var data: ByteArray

    /**
     * TBD.
     */
    public var encoding: Encodings

    /**
     * TBD.
     */
    public fun setData(value: ByteArray): B

    /**
     * TBD.
     */
    public fun setData(value: String): B

    /**
     * TBD.
     */
    public fun setBase64(value: Base64Data): B

    /**
     * TBD.
     */
    public fun setHex(value: HexData): B

    /**
     * TBD.
     */
    public override fun build(): Container
  }

  /**
   * TBD.
   */
  public interface Factory<
    Container,
    Builder,
    Fingerprint,
    FingerprintBuilder,
    HashAlgorithm,
    Encoding,
  > : Record.Factory<Container, Builder>
    where Encoding : Enum<Encoding>,
          HashAlgorithm : Enum<HashAlgorithm>,
          Fingerprint : DataFingerprint<Fingerprint, FingerprintBuilder, HashAlgorithm, Encoding>,
          FingerprintBuilder : DataFingerprint.IBuilder<Fingerprint, HashAlgorithm, Encoding, FingerprintBuilder>,
          Builder : IBuilder<Container, Fingerprint, FingerprintBuilder, HashAlgorithm, Encoding, Builder> {
    /**
     * TBD.
     */
    public fun create(encoding: Encoding, data: ByteArray): Container

    /**
     * TBD.
     */
    public fun create(data: ByteArray): Container

    /**
     * TBD.
     */
    public fun create(base64: Base64Data): Container

    /**
     * TBD.
     */
    public fun create(hex: HexData): Container

    /**
     * TBD.
     */
    public fun create(data: String): Container
  }

  /**
   * TBD.
   */
  public fun bytes(): ByteArray

  /**
   * TBD.
   */
  public fun encoding(): Encodings?

  /**
   * TBD.
   */
  public fun fingerprint(): DataFingerprint<*, *, *, *>?

  /**
   * TBD.
   */
  public fun mutate(op: context(Builder) () -> Unit): Concrete
}
