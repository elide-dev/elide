package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * # JavaScript: `crypto`
 *
 * Main interface providing the Web Crypto API to guest JavaScript VMs. The Web Crypto API is primarily substantiated by
 * this interface, in addition to [SubtleCrypto], which focuses on cryptographic primitives which operate in constants
 * time.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * "The Crypto interface represents basic cryptography features available in the current context. It allows access to a
 * cryptographically strong random number generator and to cryptographic primitives."
 *
 * &nbsp;
 *
 * ## Specification compliance
 *
 * Elide's implementation of the Web Crypto API is backed by primitives provided by the JVM host environment; random
 * data uses [java.security.SecureRandom], and UUIDs are generated using [java.util.UUID.randomUUID]. [SubtleCrypto] is
 * provided via the [subtle] property.
 */
public interface Crypto: RandomSource {
  public companion object {
    /** Maximum number of bytes that can be requested from [getRandomValues]; above this size, an error is thrown. */
    public const val MAX_RANDOM_BYTES_SIZE: Int = 65_535
  }

  /**
   * ## Subtle Crypto
   *
   * Access to so-called "Subtle Crypto," which is a module constituent to the Web Crypto API that focuses on
   * cryptographic primitives which run in constant-time.
   */
  @get:Polyglot public val subtle: SubtleCrypto
}
