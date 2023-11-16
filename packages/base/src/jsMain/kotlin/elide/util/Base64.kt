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

@file:OptIn(ExperimentalEncodingApi::class)

package elide.util

import org.w3c.dom.Window
import kotlinx.browser.window
import kotlin.io.encoding.ExperimentalEncodingApi

/** Cross-platform utilities for encoding and decoding to/from Base64. */
@Suppress("unused", "MemberVisibilityCanBePrivate") public actual object Base64: Encoder {
  private val isBrowser: Boolean = js("this[\"window\"] !== undefined") == true

  /** Array of Base64-allowable characters in web-safe mode.  */
  public val CHARACTER_SET_WEBSAFE: CharArray = run {
    listOf(
      ('A'..'Z'),
      ('a'..'z'),
      ('0'..'9')
    ).map { range ->
      range.toSet()
    }.reduce { a, b ->
      a + b
    }.toCharArray()
  }

  /** Array of Base64-allowable characters.  */
  public val CHARACTER_SET: CharArray = run {
    listOf(
      ('A'..'Z'),
      ('a'..'z'),
      ('0'..'9'),
    ).map { range ->
      range.toSet()
    }.plus(listOf(setOf(
      '+', '/', '='
    ))).reduce { a, b ->
      a + b
    }.toCharArray()
  }

  private fun <R> browserOrServer(browser: () -> R, nodejs: (() -> R)? = null): R {
    return if (isBrowser || nodejs == null) {
      browser.invoke()
    } else {
      nodejs.invoke()
    }
  }

  actual override fun encoding(): Encoding = Encoding.BASE64

  // -- Base64: Encoding -- //
  /**
   * Encode the provided [string] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param string String to encode with Base64.
   * @return Base64-encoded string.
   */
  actual override fun encode(string: String): ByteArray = browserOrServer({
    window.btoa(string).encodeToByteArray()
  }, {
    Base64Kt.encoder.encode(string.encodeToByteArray())
  })

  /**
   * Encode the provided [string] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param string String to encode with Base64.
   * @return Base64-encoded string.
   */
  actual override fun encodeToString(string: String): String = browserOrServer({
    window.btoa(string)
  }, {
    Base64Kt.encoder.encode(string.encodeToByteArray()).decodeToString()
  })

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, which includes padding if necessary.
   *
   * @param data Raw bytes to encode with Base64.
   * @return Base64-encoded bytes.
   */
  actual override fun encode(data: ByteArray): ByteArray = browserOrServer({
    window.btoa(data.decodeToString()).encodeToByteArray()
  }, {
    Base64Kt.encoder.encode(data)
  })

  /**
   * Encode the provided [data] into a Base64-encoded string, which includes padding if necessary.
   *
   * @param data Raw bytes to encode into a Base64 string.
   * @return Base64-encoded string.
   */
  actual override fun encodeToString(data: ByteArray): String = browserOrServer({
    window.btoa(data.decodeToString())
  }, {
    Base64Kt.encoder.encode(data).decodeToString()
  })

  // -- Base64: Encoding (Web-safe) -- //

  /**
   * Encode the provided [string] into a Base64-encoded string, omitting characters which are unsafe for use on the web,
   * including padding characters, which are not emitted.
   *
   * @param string String to encode with web-safe Base64.
   * @return Base64-encoded string, using only web-safe characters.
   */
  public actual fun encodeWebSafe(string: String): String = browserOrServer({
    kotlin.io.encoding.Base64.UrlSafe.encode(string.encodeToByteArray())
  })

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the
   * web, including padding characters, which are not emitted.
   *
   * @param data Raw bytes to encode with web-safe Base64.
   * @return Base64-encoded bytes, using only web-safe characters.
   */
  public actual fun encodeWebSafe(data: ByteArray): ByteArray = browserOrServer({
    kotlin.io.encoding.Base64.UrlSafe.encodeToByteArray(data)
  })

  // -- Base64: Decoding -- //

  /**
   * Decode the provided [data] from Base64, returning a raw set of bytes resulting from the decoding operation.
   *
   * @param data Data to decode from Base64.
   * @return Raw bytes of decoded data.
   */
  actual override fun decode(data: ByteArray): ByteArray = browserOrServer({
    window.atob(data.decodeToString()).encodeToByteArray()
  }, {
    kotlin.io.encoding.Base64.decode(data)
  })

  /**
   * Decode the provided [string] from Base64, returning a raw set of bytes resulting from the decoding operation.
   *
   * @param string String to decode from Base64.
   * @return Raw bytes of decoded data.
   */
  actual override fun decode(string: String): ByteArray = browserOrServer({
    window.atob(string).encodeToByteArray()
  }, {
    kotlin.io.encoding.Base64.decode(string)
  })

  /**
   * Decode the provided [data] from Base64, returning a regular string value, encoded as UTF-8.
   *
   * @param data Data to decode from Base64.
   * @return Decoded string value.
   */
  actual override fun decodeToString(data: ByteArray): String = browserOrServer({
    window.atob(data.decodeToString())
  }, {
    kotlin.io.encoding.Base64.decode(data).decodeToString()
  })

  /**
   * Decode the provided [string] from Base64, returning a regular string value, encoded as UTF-8.
   *
   * @param string String to decode from Base64.
   * @return Decoded string value.
   */
  actual override fun decodeToString(string: String): String = browserOrServer({
    window.atob(string)
  }, {
    kotlin.io.encoding.Base64.decode(string).decodeToString()
  })
}
