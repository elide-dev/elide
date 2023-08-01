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
