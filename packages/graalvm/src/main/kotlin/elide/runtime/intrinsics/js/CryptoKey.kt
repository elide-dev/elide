package elide.runtime.intrinsics.js

import elide.annotations.core.Polyglot

/**
 * # JavaScript: `CryptoKey`
 *
 * The `CryptoKey` interface of the Web Crypto API represents a cryptographic key obtained, wrapped, or generated, using
 * the [SubtleCrypto] facilities.
 *
 * From [MDN](https://developer.mozilla.org/en-US/docs/Web/API/CryptoKey):
 * "The `CryptoKey` interface of the Web Crypto API represents a cryptographic key obtained from one of the
 * [SubtleCrypto] methods `generateKey`, `deriveKey`, `importKey`, or `unwrapKey`."
 *
 * See instance properties for more information. This interface is part of the Web Crypto API ([Crypto]).
 *
 * @see Crypto for the main Web Crypto API interface.
 * @see SubtleCrypto for the "Subtle Crypto" module, which provides cryptographic primitives which run in constant-time.
 *   Methods on this interface are used to generate, wrap, or otherwise obtain [CryptoKey] instances.
 */
public interface CryptoKey {
  /**
   * ## Crypto Key: `type`
   *
   */
  @get:Polyglot public val type: String

  /**
   * ## Crypto Key: `extractable`
   *
   */
  @get:Polyglot public val extractable: Boolean

  /**
   * ## Crypto Key: `algorithm`
   *
   */
  @get:Polyglot public val algorithm: Any

  /**
   * ## Crypto Key: `usages`
   *
   */
  @get:Polyglot public val usages: Array<String>

  /**
   * ## Crypto Key: Constructors
   *
   * Non-standard interface which gathers the available key creation, generation, and wrapping methods, so they can be
   * defined on a single factory. See MDN documentation for [CryptoKey] and [SubtleCrypto] for more information.
   */
  public interface Constructors
}
