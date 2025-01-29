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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.intrinsics.js.codec

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import elide.core.api.Symbolic
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractJsIntrinsic
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asPublicJsSymbol
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.encoding.TextDecoder.DecodeOptions as TextDecodeOptions
import elide.runtime.intrinsics.js.encoding.TextDecoder as TextDecoderAPI
import elide.runtime.intrinsics.js.encoding.TextDecoder.Options as TextDecoderOptions
import elide.runtime.intrinsics.js.encoding.TextEncoder as TextEncoderAPI
import elide.vm.annotations.Polyglot

// Symbols where classes and functions are installed within the guest context.
private const val TEXT_ENCODER_SYMBOL_NAME: String = "TextEncoder"
private const val TEXT_DECODER_SYMBOL_NAME: String = "TextDecoder"

// Defaults for charsets and other parameters.
private const val JS_CHARSET_NAME_UTF8: String = "utf-8"
private const val JS_CHARSET_NAME_ISO8859_1: String = "iso-8859-1"
private const val JS_CHARSET_NAME_ASCII: String = "ascii"
private val DEFAULT_JS_CHARSET: Charset = Charsets.UTF_8

// Installs encoder intrinsics.
@Intrinsic(internal = false) internal class JsEncodingIntrinsics : AbstractJsIntrinsic() {
  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[TEXT_ENCODER_SYMBOL_NAME.asPublicJsSymbol()] = TextEncoder.Factory
    bindings[TEXT_DECODER_SYMBOL_NAME.asPublicJsSymbol()] = TextDecoder.Factory
  }
}

/** Enumerates supported JavaScript encodings. */
public enum class SupportedJsEncoding (override val symbol: String): Symbolic<String> {
  UTF_8(JS_CHARSET_NAME_UTF8),
  ISO_8859_1(JS_CHARSET_NAME_ISO8859_1),
  ASCII(JS_CHARSET_NAME_ASCII);

  public companion object: Symbolic.SealedResolver<String, SupportedJsEncoding> {
    override fun resolve(symbol: String): SupportedJsEncoding = when (symbol) {
      JS_CHARSET_NAME_UTF8 -> UTF_8
      JS_CHARSET_NAME_ISO8859_1 -> ISO_8859_1
      JS_CHARSET_NAME_ASCII -> ASCII
      else -> throw unresolved(symbol)
    }
  }
}

/**
 * Holds a resolved JavaScript encoding, paired with a [Charset].
 *
 * Please use symbolic resolution methods to obtain instances of this class.
 *
 * @param value The resolved encoding and its corresponding Java [Charset].
 */
@JvmInline public value class JsEncoding private constructor (
  private val value: Pair<Charset, SupportedJsEncoding>
) : Comparable<JsEncoding>, CharSequence, Symbolic<SupportedJsEncoding> {
  // Return the Java charset.
  public val charset: Charset get() = value.first

  // Return the corresponding JavaScript token describing this encoding.
  override val symbol: SupportedJsEncoding get() = value.second

  /** Resolve string symbols (JavaScript encoding "labels") to Java [Charset] objects, through [JsEncoding]. */
  @Suppress("MemberVisibilityCanBePrivate")
  public companion object: Symbolic.SealedResolver<SupportedJsEncoding, JsEncoding> {
    // UTF-8 encoding.
    public val UTF_8: JsEncoding = JsEncoding(DEFAULT_JS_CHARSET to SupportedJsEncoding.UTF_8)

    // ISO-8859-1 encoding.
    public val ISO_8859_1: JsEncoding = JsEncoding(Charsets.ISO_8859_1 to SupportedJsEncoding.ISO_8859_1)

    // ASCII encoding.
    public val ASCII: JsEncoding = JsEncoding(Charsets.US_ASCII to SupportedJsEncoding.ASCII)

    // Default encoding (UTF-8).
    public val DEFAULT: JsEncoding = UTF_8

    override fun resolve(symbol: SupportedJsEncoding): JsEncoding = when (symbol) {
      SupportedJsEncoding.UTF_8 -> UTF_8
      SupportedJsEncoding.ISO_8859_1 -> ISO_8859_1
      SupportedJsEncoding.ASCII -> ASCII
    }

    /** Sugar method to resolve a [JsEncoding] directly from a string [symbol]. */
    public fun resolve(symbol: String): JsEncoding = resolve(SupportedJsEncoding.resolve(symbol))
  }

  override fun toString(): String = value.second.symbol
  override fun compareTo(other: JsEncoding): Int = value.second.compareTo(other.value.second)
  override val length: Int get() = toString().length
  override fun get(index: Int): Char = toString()[index]
  override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = toString().subSequence(startIndex, endIndex)
}

// Holds common logic for JavaScript encoder utilities.
public sealed class JsEncoderUtility protected constructor (protected val charset: JsEncoding)

/** Implements a JavaScript [TextEncoderAPI]. */
public class TextEncoder public constructor (encoding: JsEncoding): TextEncoderAPI, JsEncoderUtility(encoding) {
  /** Create a [TextEncoder] from a JavaScript encoding [label]. */
  public constructor(label: String?): this(label?.let { JsEncoding.resolve(it) } ?: JsEncoding.DEFAULT)

  /** Create a [TextEncoder] which uses the default JavaScript encoding (UTF-8). */
  public constructor(): this(JsEncoding.DEFAULT)

  public companion object Factory: TextEncoderAPI.Factory {
    override fun create(): TextEncoder = TextEncoder()

    override fun create(encoding: Value?): TextEncoder = when (encoding) {
      null -> create()
      else -> TextEncoder(when {
        encoding.isNull -> JsEncoding.DEFAULT
        !encoding.isString -> throw JsError.typeError("Expected string or null for `TextEncoder.create`")
        else -> JsEncoding.resolve(encoding.asString())
      })
    }
  }

  // Returns the JavaScript label for the assigned encoding.
  @get:Polyglot override val encoding: String get() = charset.symbol.symbol

  private fun asBytestring(text: String): ByteString = text.encodeToByteString(charset.charset)

  // Encoder implementation.
  @Polyglot override fun encode(text: String): ByteArray = asBytestring(text).toByteArray()

  override fun encodeInto(text: String, bytes: Value) {
    assert(bytes.hasArrayElements())
    encode(text).forEachIndexed { index, byte ->
      bytes.setArrayElement(index.toLong(), byte)
    }
  }

  override fun encodeInto(text: String, bytes: ByteArray) {
    require(bytes.size >= text.length) {
      throw IndexOutOfBoundsException("ByteArray capacity must be at least the length of the text")
    }
    encode(text).copyInto(bytes)
  }

  override fun encodeInto(text: String, bytes: ByteBuffer) {
    require(bytes.capacity() >= text.length) {
      throw IndexOutOfBoundsException("Buffer capacity must be at least the length of the text")
    }
    bytes.put(encode(text))
  }

  override fun encodeInto(text: String, bytes: WritableByteChannel) {
    bytes.write(ByteBuffer.wrap(encode(text)))
  }

  override fun encodeInto(text: String, bytes: OutputStream) {
    bytes.write(encode(text))
  }

  override fun getMember(key: String?): Any? = when (key) {
    "encoding" -> encoding
    "encode" -> ProxyExecutable { encode(it.getOrNull(0)) }
    "encodeInto" -> ProxyExecutable { encodeInto(it.getOrNull(0), it.getOrNull(1)) }
    else -> null
  }
}

/** Implements a JavaScript [TextDecoderAPI]. */
public class TextDecoder private constructor (readerCharset: JsEncoding, override val options: TextDecoderOptions):
  TextDecoderAPI,
  JsEncoderUtility(readerCharset) {
  /** Shortcut methods for creating [TextDecoderAPI.Options] instances. */
  public object Options {
    /** Default options for [TextDecoderAPI]. */
    @JvmStatic public fun defaults(): TextDecoderOptions = TextDecoderOptions.DEFAULTS

    /** Create options for [TextDecoderAPI]. */
    @JvmStatic public fun of(
      fatal: Boolean = TextDecoderAPI.Defaults.DEFAULT_FATAL,
      ignoreBOM: Boolean = TextDecoderAPI.Defaults.DEFAULT_IGNORE_BOM,
    ): TextDecoderOptions = TextDecoderOptions.of(fatal = fatal, ignoreBOM = ignoreBOM)
  }

  /** Shortcut methods for creating [TextDecoderAPI.DecodeOptions] instances. */
  public object DecodeOptions {
    /** Default options for [TextDecoderAPI.decode]. */
    @JvmStatic public fun defaults(): TextDecodeOptions = TextDecodeOptions.DEFAULTS

    /** Create options for [TextDecoderAPI.decode]. */
    @JvmStatic public fun of(
      stream: Boolean = TextDecoderAPI.Defaults.DEFAULT_DECODE_STREAM,
    ): TextDecodeOptions = TextDecodeOptions.of(stream = stream)
  }

  /** Public host-side encoding label constructor. */
  public constructor(label: String, options: TextDecoderOptions): this(JsEncoding.resolve(label), options)

  /** Public host-side encoding label constructor. */
  public constructor(label: String): this(label, TextDecoderOptions.DEFAULTS)

  /** Public host-side encoding constructor. */
  public constructor(encoding: JsEncoding): this(encoding, TextDecoderOptions.DEFAULTS)

  /** Public no-arg constructor. */
  @Polyglot public constructor(): this(JsEncoding.DEFAULT, TextDecoderOptions.DEFAULTS)

  /** Constructors for [TextDecoder] instances. */
  public companion object Factory: TextDecoderAPI.Factory {
    override fun create(): TextDecoder = TextDecoder()

    override fun create(label: String): TextDecoder =
      TextDecoder(JsEncoding.resolve(label), TextDecoderOptions.DEFAULTS)

    override fun create(label: String, options: TextDecoderOptions): TextDecoder =
      TextDecoder(JsEncoding.resolve(label), options)
  }

  // Returns the JavaScript label for the assigned encoding.
  @get:Polyglot override val encoding: String get() = charset.symbol.symbol

  override fun decode(buffer: ByteBuffer): String = decode(buffer, null)
  override fun decode(bytes: ByteArray): String = bytes.decodeToString()
  override fun decode(stream: InputStream): String = stream.readBytes().decodeToString()
  override fun decode(channel: ReadableByteChannel): String = Channels.newInputStream(channel).use {
    it.readBytes().decodeToString()
  }

  // @TODO: honor `DecodeOptions` from host?
  override fun decode(buffer: ByteBuffer, options: TextDecodeOptions?): String =
    charset.charset.decode(buffer).toString()

  // @TODO honor `DecodeOptions`
  @Polyglot override fun decode(buffer: Value, options: TextDecodeOptions): String {
    assert(buffer.hasArrayElements())
    val size = buffer.arraySize.toInt()
    val bytes = ByteArray(size)

    for (i in 0 until size) {
      val elem = buffer.getArrayElement(i.toLong())
      if (elem.fitsInByte()) {
        bytes[i] = elem.asByte()
      } else when {
        elem.fitsInInt() -> bytes[i] = elem.asInt().toByte()
        else -> throw JsError.typeError(
          "Expected byte or int for `TextDecoder.decode`, at index $i (value '$elem')"
        )
      }
    }
    return String(bytes, charset.charset)
  }

  override fun getMember(key: String?): Any? = when (key) {
    "encoding" -> encoding
    "decode" -> ProxyExecutable {
      when (it.size) {
        0 -> decode()
        1 -> decode(it.getOrNull(0))
        2 -> decode(it.getOrNull(0), it.getOrNull(1))
        else -> throw JsError.typeError("Expected 0-2 arguments for `TextDecoder.decode`")
      }
    }
    else -> null
  }
}
