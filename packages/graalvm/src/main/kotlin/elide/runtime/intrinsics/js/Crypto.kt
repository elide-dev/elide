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

package elide.runtime.intrinsics.js

import elide.vm.annotations.Polyglot

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
