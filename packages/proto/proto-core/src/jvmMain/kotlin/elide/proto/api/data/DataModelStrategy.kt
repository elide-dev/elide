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
