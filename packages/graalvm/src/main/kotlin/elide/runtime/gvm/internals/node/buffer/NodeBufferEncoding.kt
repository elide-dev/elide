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
package elide.runtime.gvm.internals.node.buffer

import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.encoding.Base64 as Base64Encoding

/**
 * A collection of helper functions handling encoding and decoding of strings with supported encodings in Node.js APIs.
 */
internal sealed interface NodeBufferEncoding {
  fun computeLength(string: String): Int

  fun encode(string: String): ByteArray

  fun decode(bytes: ByteArray): String

  /** Hex encoding implementation using the Kotlin standard library hex format. */
  @OptIn(ExperimentalStdlibApi::class)
  data object Hex : NodeBufferEncoding {
    override fun computeLength(string: String): Int = string.length / 2
    override fun encode(string: String): ByteArray = string.hexToByteArray()
    override fun decode(bytes: ByteArray): String = bytes.toHexString()
  }

  /** UTF-8 implementation using the Kotlin standard library encoder with the appropriate charset. */
  data object Utf8 : NodeBufferEncoding {
    override fun computeLength(string: String): Int {
      // TODO(@darvld): calculate the size without encoding first
      return encode(string).size
    }

    override fun encode(string: String): ByteArray = string.toByteArray(Charsets.UTF_8)
    override fun decode(bytes: ByteArray): String = bytes.toString(Charsets.UTF_8)
  }

  /**
   * UTF-16LE implementation using the Kotlin standard library encoder with the appropriate charset; Node.js only
   * supports the little-endian variant of UTF-16 encoding.
   */
  data object Utf16LE : NodeBufferEncoding {
    override fun computeLength(string: String): Int {
      // TODO(@darvld): calculate the size without encoding first
      return encode(string).size
    }

    override fun encode(string: String): ByteArray = string.toByteArray(Charsets.UTF_16LE)
    override fun decode(bytes: ByteArray): String = bytes.toString(Charsets.UTF_16LE)
  }

  /** Latin-1 (ISO-8859-1) implementation using the Kotlin standard library encoder with the appropriate charset. */
  data object Latin1 : NodeBufferEncoding {
    override fun computeLength(string: String): Int = string.length
    override fun encode(string: String): ByteArray = string.toByteArray(Charsets.ISO_8859_1)
    override fun decode(bytes: ByteArray): String = bytes.toString(Charsets.ISO_8859_1)
  }

  /** Simple Base64 implementation using the Kotlin standard library's Base64 codec. */
  @OptIn(ExperimentalEncodingApi::class)
  data object Base64 : NodeBufferEncoding {
    override fun computeLength(string: String): Int {
      // TODO(@darvld): calculate the size without encoding first
      return encode(string).size
    }

    override fun encode(string: String): ByteArray {
      return Base64Encoding.decode(string)
    }

    override fun decode(bytes: ByteArray): String {
      return Base64Encoding.encode(bytes)
    }
  }

  /** Simple url-safe Base64 implementation using the Kotlin standard library's Base64 codec. */
  @OptIn(ExperimentalEncodingApi::class)
  data object Base64Url : NodeBufferEncoding {
    override fun computeLength(string: String): Int {
      // TODO(@darvld): calculate the size without encoding first
      return encode(string).size
    }

    override fun encode(string: String): ByteArray {
      return Base64Encoding.UrlSafe.decode(string)
    }

    override fun decode(bytes: ByteArray): String {
      return Base64Encoding.UrlSafe.encode(bytes)
    }
  }

  companion object {
    fun isSupported(encoding: String): Boolean = named(encoding) != null

    fun named(encoding: String): NodeBufferEncoding? = when (encoding.lowercase()) {
      "utf8", "utf-8" -> Utf8
      "utf16le", "utf-16le" -> Utf16LE
      "latin1" -> Latin1
      "base64" -> Base64
      "base64url" -> Base64Url
      "hex" -> Hex
      else -> null
    }
  }
}

@Suppress("nothing_to_inline")
internal inline fun NodeBufferEncoding.Companion.byteLength(string: String, encoding: String?): Int {
  return (encoding?.let { named(it) } ?: NodeBufferEncoding.Utf8).computeLength(string)
}

@Suppress("nothing_to_inline")
internal inline fun NodeBufferEncoding.Companion.encode(string: String, encoding: String?): ByteArray {
  return (encoding?.let { named(it) } ?: NodeBufferEncoding.Utf8).encode(string)
}

@Suppress("nothing_to_inline")
internal inline fun NodeBufferEncoding.Companion.decode(bytes: ByteArray, encoding: String?): String {
  return (encoding?.let { named(it) } ?: NodeBufferEncoding.Utf8).decode(bytes)
}
