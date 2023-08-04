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

package elide.proto.impl.data


/** Use a native Kotlin enumeration for hash algorithms. */
internal typealias HashAlgorithm = FlatHashAlgorithm

/** Symbol for different hash algorithms (integer used). */
internal typealias HashAlgorithmSymbol = Int

/** Use a native Kotlin enumeration for encodings. */
internal typealias Encoding = FlatEncoding

/** Symbol for different encodings (integer used). */
internal typealias EncodingSymbol = Int

/** Unwrap the provided [FlatHashAlgorithm] enum to its expected constant value. */
internal fun FlatHashAlgorithm.unwrap(): HashAlgorithmSymbol {
  return this.symbol
}

/** Unwrap the provided [FlatEncoding] enum to its expected constant value. */
internal fun FlatEncoding.unwrap(): HashAlgorithmSymbol {
  return this.symbol
}
