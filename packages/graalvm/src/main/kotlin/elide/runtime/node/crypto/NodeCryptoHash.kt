/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.node.crypto

import org.graalvm.polyglot.Value
import java.security.MessageDigest
import java.util.Base64
import elide.runtime.node.buffer.NodeHostBuffer
import elide.vm.annotations.Polyglot

// Map Node.js hash algorithm names to the JVM equivalent
private val NODE_TO_JVM_ALGORITHM = mapOf(
  "md5" to "MD5",
  "sha1" to "SHA-1",
  "sha256" to "SHA-256",
  "sha512" to "SHA-512",
  "sha3-256" to "SHA3-256",
)

// @TODO(elijahkotyluk) Add support for an optional options parameter to configure output format, etc.
/**
 * ## Node API: Hash
 * Implements the Node.js `Hash` class for feeding data into the Hash object and creating hash digests.
 * See also: [Node.js Crypto API: `Hash`](https://nodejs.org/api/crypto.html#class-hash)
 */
public class NodeHash(
  private val algorithm: String,
  md: MessageDigest? = null,
  private var digested: Boolean = false
) {
  private val md: MessageDigest = md ?: MessageDigest.getInstance(resolveAlgorithm(algorithm))

  // @TODO(elijahkotyluk) add support for transform options
  public fun copy(): NodeHash {
    if (digested) throw IllegalStateException("Digest already called, cannot copy a finalized Hash.")

    val mdClone = try {
      md.clone() as MessageDigest
    } catch (e: CloneNotSupportedException) {
      throw IllegalStateException(e.message ?: "Failed to clone MessageDigest instance.")
    }

    // Create new NodeHash with the cloned digest
    return NodeHash(
      algorithm = this.algorithm,
      md = mdClone,
      digested = false
    )
  }
  // Update the current hash with new data
  public fun update(data: Any): NodeHash {
    if (digested) throw IllegalStateException("Digest already called")

    val bytes = when (data) {
      is String -> data.toByteArray(Charsets.UTF_8)
      is ByteArray -> data
      is Value -> {
        when {
          data.hasArrayElements() -> {
            val arr = ByteArray(data.arraySize.toInt())
            for (i in arr.indices) {
              arr[i] = (data.getArrayElement(i.toLong()).asInt() and 0xFF).toByte()
            }
            arr
          }
          data.isString -> data.asString().toByteArray(Charsets.UTF_8)
          else -> throw IllegalArgumentException("Unsupported item type, must be of type string or an instance of Buffer, TypedArray, or Dataview. Received: ${data.javaClass.name}")
        }
      }
      is Iterable<*> -> { // @TODO(elijahkotyluk) Handle Polyglot lists as an iterable, may need to revisit
        val arr = data.map {
          when (it) {
            is Number -> it.toByte()
            else -> throw IllegalArgumentException("Unsupported item type, must be of type string or an instance of Buffer, TypedArray, or Dataview. Received: ${it?.javaClass?.name}")
          }
        }.toByteArray()
        arr
      }
      else -> throw IllegalArgumentException("The \"data\" argument must be of type string or an instance of Buffer, TypedArray, or Dataview. Received an instance of: ${data::class}")
    }

    md.update(bytes)

    return this
  }

  /**
   * Public overload of [digestInternal].
   * This is equivalent to calling [digestInternal] with a `null` encoding.
   * @return The computed digest as a [ByteArray].
   */
  @Polyglot public fun digest(): Any = digestInternal(null)

  /**
   * Public overload of [digestInternal].
   * This is equivalent to calling [digestInternal] with the specified [encoding].
   * @param encoding Encoding for the output digest.
   * @return The computed digest in the specified encoding.
   */
  @Polyglot public fun digest(encoding: String?): Any = digestInternal(encoding)

  /**
   * Compute the digest of the data passed to [update], returning it in the specified [encoding].
   * @param encoding Optional encoding for the output digest. Supported values are:
   * - `null` or `"buffer"`: returns a [ByteArray]
   * - `"hex"`: returns a hexadecimal [String]
   * - `"base64"`: returns a Base64-encoded [String]
   * - `"latin1"`: returns a ISO-8859-1 encoded [String]
   * @return The computed digest in the specified encoding.
   */
  private fun digestInternal(encoding: String? = null): Any {
    if (digested) throw IllegalStateException("Digest has already been called on this Hash instance.")
    digested = true
    val result = md.digest()

    return when (encoding?.lowercase()) {
      null, "buffer" -> NodeHostBuffer.wrap(result)
      "hex" -> result.joinToString("") { "%02x".format(it) }
      "base64" -> Base64.getEncoder().encodeToString(result)
      "latin1" -> result.toString(Charsets.ISO_8859_1)
      else -> throw IllegalArgumentException("Encoding: ${encoding} is not supported.")
    }
  }

  // @TODO(elijahkotyluk) better error messaging should be added here
  private fun resolveAlgorithm(nodeAlgo: String): String =
    NODE_TO_JVM_ALGORITHM[nodeAlgo.lowercase()] ?: throw IllegalArgumentException("Unsupported algorithm: $nodeAlgo.")
}
