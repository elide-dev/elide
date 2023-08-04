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

package elide.proto.api.data

import elide.proto.api.wkt.Timestamp

/**
 * TBD.
 */
interface DataModelStrategy<
  HashAlgorithms,
  Encodings,
  Container,
  ContainerB,
  Fingerprint,
  FingerprintB,
  Stamp,
  StampB,
> where
  Encodings : Enum<Encodings>,
  HashAlgorithms : Enum<HashAlgorithms>,
  Container : DataContainer<Container, ContainerB, Fingerprint, FingerprintB, HashAlgorithms, Encodings>,
  ContainerB : DataContainer.IBuilder<Container, Fingerprint, FingerprintB, HashAlgorithms, Encodings, ContainerB>,
  Fingerprint : DataFingerprint<Fingerprint, FingerprintB, HashAlgorithms, Encodings>,
  FingerprintB : DataFingerprint.IBuilder<Fingerprint, HashAlgorithms, Encodings, FingerprintB>,
  StampB : Timestamp.IBuilder<Stamp> {
  /**
   * TBD.
   */
  fun fingerprints(): DataFingerprint.Factory<
    Fingerprint,
    FingerprintB,
    HashAlgorithms,
    Encodings,
  >

  /**
   * TBD.
   */
  fun containers(): DataContainer.Factory<
    Container,
    ContainerB,
    Fingerprint,
    FingerprintB,
    HashAlgorithms,
    Encodings,
  >

  /**
   * TBD.
   */
  fun timestamps(): Timestamp.Factory<
    Stamp,
    StampB,
  >
}
