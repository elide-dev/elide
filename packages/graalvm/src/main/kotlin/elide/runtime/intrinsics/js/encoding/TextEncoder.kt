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
package elide.runtime.intrinsics.js.encoding

import org.graalvm.polyglot.Value
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.WritableByteChannel
import elide.annotations.API
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.vm.annotations.Polyglot

// Properties and methods of `TextEncoder` exported to guest contexts.
private val TEXT_ENCODER_METHODS_AND_PROPS = arrayOf(
  "encoding",
  "encode",
  "encodeInto",
)

/**
 * # Text Encoder
 *
 * `TextEncoder` is part of the Encoding API, and provides facilities for encoding strings into, and decoding strings
 * from, byte streams.
 *
 * The `TextEncoder` interface represents an encoder for a specific method, that is a specific character encoding, like
 * UTF-8, ISO-8859-2, KOI8, etc.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * Text encoders can be created anywhere within the JavaScript context, as the class is installed at the `TextEncoder`
 * symbol within the global namespace.
 */
@API public interface TextEncoder: EncodingUtility {
  /**
   * ## Encode
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string.
   *
   * @param text The string to encode.
   * @return The bytes containing the UTF-8 representation of the string
   */
  public fun encode(text: String): ByteArray

  /**
   * ## Encode
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string.
   *
   * @param text The string to encode.
   * @return The bytes containing the UTF-8 representation of the string
   */
  @Polyglot public fun encode(text: Value?): ByteArray {
    if (text == null || text.isNull || !text.isString)
      throw JsError.typeError("`TextEncoder.encode` expects a string")
    return encode(text.asString())
  }

  /**
   * ## Encode Into (Host Arrays)
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided byte array.
   *
   * This method uses host types, and is meant for host dispatch only.
   *
   * @param text The string to encode.
   * @param bytes The byte array to copy the encoded string into.
   */
  public fun encodeInto(text: String, bytes: ByteArray)

  /**
   * ## Encode Into (Host Buffers)
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided byte buffer.
   *
   * This method uses host types, and is meant for host dispatch only.
   *
   * @param text The string to encode.
   * @param bytes The byte buffer to copy the encoded string into.
   */
  public fun encodeInto(text: String, bytes: ByteBuffer)

  /**
   * ## Encode Into (Host Channels)
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided writable channel.
   *
   * This method uses host types, and is meant for host dispatch only.
   *
   * @param text The string to encode.
   * @param bytes Output channel to write into.
   */
  public fun encodeInto(text: String, bytes: WritableByteChannel)

  /**
   * ## Encode Into (Host Streams)
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided output stream.
   *
   * This method uses host types, and is meant for host dispatch only.
   *
   * Note: The stream is not closed after writing.
   *
   * @param text The string to encode.
   * @param bytes Output stream to write into.
   */
  public fun encodeInto(text: String, bytes: OutputStream)

  /**
   * ## Encode Into
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided byte array.
   *
   * @param text The string to encode.
   * @param bytes The byte array to copy the encoded string into.
   */
  public fun encodeInto(text: String, bytes: Value)

  /**
   * ## Encode Into
   *
   * Encodes a string into a [ByteArray] containing the UTF-8 representation of the string, and then copies it into the
   * provided byte array.
   *
   * @param text The string to encode.
   * @param bytes The byte array to copy the encoded string into.
   */
  @Polyglot public fun encodeInto(text: Value?, bytes: Value?) {
    if (text == null || text.isNull || !text.isString)
      throw JsError.typeError("`TextEncoder.encodeInto` expects a string as the 1st param")
    if (bytes == null || bytes.isNull || !bytes.hasArrayElements())
      throw JsError.typeError("`TextEncoder.encodeInto` expects a writable array or buffer as the 2nd param")
    encodeInto(text.asString(), bytes)
  }

  override fun getMemberKeys(): Array<String> = TEXT_ENCODER_METHODS_AND_PROPS
  override fun hasMember(key: String?): Boolean = key != null && key in TEXT_ENCODER_METHODS_AND_PROPS
  override fun putMember(key: String?, value: Value?) { /* no-op */ }
  override fun removeMember(key: String?): Boolean = false

  /**
   * ## Text Encoder Factory
   *
   * Extends the base [EncodingUtility.Factory] to specialize it for the type [TextEncoder].
   */
  @API public interface Factory: EncodingUtility.Factory<TextEncoder>
}
