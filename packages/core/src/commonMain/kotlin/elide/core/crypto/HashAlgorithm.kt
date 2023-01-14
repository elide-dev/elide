package elide.core.crypto

/** Enumerates supported hash algorithms for pure-Kotlin use. */
public enum class HashAlgorithm {
  /** No hash algorithm in use. */
  IDENTITY,

  /** Algorithm: MD5. */
  MD5,

  /** Algorithm: SHA-1. */
  SHA1,

  /** Algorithm: SHA-256. */
  SHA_256,

  /** Algorithm: SHA-512. */
  SHA_512,

  /** Algorithm: SHA3-224. */
  SHA3_224,

  /** Algorithm: SHA3-256. */
  SHA3_256,

  /** Algorithm: SHA3-512. */
  SHA3_512;
}
