package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot
import elide.runtime.intrinsics.js.err.QuotaExceededError
import elide.runtime.intrinsics.js.err.ValueError
import elide.runtime.intrinsics.js.typed.UUID
import org.graalvm.polyglot.Value as GuestValue

/**
 * # JavaScript: `RandomSource`
 *
 * Base interface type which provides access to basic random data generation utilities; random bytes can be filled into
 * a guest JS array (usually typed), and random UUIDs can be generated.
 *
 * See method documentation for details.
 *
 * @see getRandomValues for details on random byte generation.
 * @see randomUUID for details on random UUID generation.
 */
public interface RandomSource {
  /**
   * ## Crypto: `getRandomValues`
   *
   * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Crypto/getRandomValues):
   * "The Crypto.getRandomValues() method lets you get cryptographically strong random values. The array given as the
   * parameter is filled with random numbers (random in its cryptographic meaning).
   *
   * To guarantee enough performance, implementations are not using a truly random number generator, but they are using
   * a pseudo-random number generator seeded with a value with enough entropy. The pseudo-random number generator
   * algorithm (PRNG) may vary across user agents, but is suitable for cryptographic purposes."
   *
   * Further notes from MDN:
   * "Don't use [getRandomValues] to generate encryption keys. Instead, use the `generateKey` method from [SubtleCrypto]
   * interface. There are a few reasons for this; for example, getRandomValues() is not guaranteed to be running in a
   * secure context.
   *
   * There is no minimum degree of entropy mandated by the Web Cryptography specification. User agents are instead urged
   * to provide the best entropy they can when generating random numbers, using a well-defined, efficient pseudorandom
   * number generator built into the user agent itself, but seeded with values taken from an external source of
   * pseudorandom numbers, such as a platform-specific random number function, the Unix `/dev/urandom` device, or other
   * source of random or pseudorandom data."
   *
   * @throws QuotaExceededError Thrown if the byteLength of typedArray exceeds 65,536.
   * @param typedArray An integer-based `TypedArray`, that is one of: `Int8Array`, `Uint8Array`, `Uint8ClampedArray`,
   *  `Int16Array`, `Uint16Array`, `Int32Array`, `Uint32Array`, `BigInt64Array`, `BigUint64Array` (but not
   *  `Float32Array` nor `Float64Array`). All elements in the array will be overwritten with random numbers.
   * @return The same array passed as typedArray but with its contents replaced with the newly generated random numbers.
   *  Note that typedArray is modified in-place, and no copy is made.
   */
  @Throws(ValueError::class, QuotaExceededError::class)
  @Polyglot public fun getRandomValues(typedArray: GuestValue): ByteArray

  /**
   * ## Crypto: `randomUUID`
   *
   * From [MDN]():
   * "The `randomUUID` method of the Crypto interface is used to generate a v4 UUID using a cryptographically secure
   * random number generator."
   *
   * @return String-like value containing a randomly generated, 36-character long v4 UUID.
   */
  @Throws(ValueError::class)
  @Polyglot public fun randomUUID(): UUID
}
