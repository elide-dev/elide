/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
package elide.runtime.intrinsics.js.node

import org.graalvm.polyglot.Value
import elide.annotations.API
import elide.vm.annotations.Polyglot

/**
 * ## Node API: Crypto
 */
@API public interface CryptoAPI : NodeAPI {
  /**
   * ## Crypto: randomUUID
   * Generates a random RFC 4122 version 4 UUID. The UUID is generated using a cryptographic pseudorandom number generator.
   *
   * See also: [Node Crypto API: `randomUUID`](https://nodejs.org/api/crypto.html#cryptorandomuuidoptions)
   *
   * @param options Optional settings (supports `disableEntropyCache` property, but is currently ignored)
   * @return A randomly generated 36 character UUID c4 string in lowercase format (e.g. "5cb34cef-5fc2-47e4-a3ac-4bb055fa2025")
   */
  @Polyglot public fun randomUUID(options: Value? = null): String
}
