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
@file:Suppress("DataClassPrivateConstructor")

package elide.runtime.intrinsics.js.encoding

import org.graalvm.polyglot.Value
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import elide.annotations.API
import elide.runtime.gvm.js.JsError
import elide.vm.annotations.Polyglot

// Properties and methods of `TextDecoder` exported to guest contexts.
private val TEXT_DECODER_METHODS_AND_PROPS = arrayOf(
  "encoding",
  "fatal",
  "ignoreBOM",
  "decode",
)

/**
 * # Text Decoder
 *
 * `TextDecoder` is part of the Encoding API (in JavaScript), and is responsible for decoding byte streams into strings
 * using a specific character encoding.
 *
 * The `TextDecoder` interface represents a decoder for a specific method, that is a specific character encoding, like
 * UTF-8, ISO-8859-2, KOI8, etc.
 *
 * &nbsp;
 *
 * ## Summary
 *
 * Text decoders can be created anywhere within the JavaScript context, as the class is installed at the `TextDecoder`
 * symbol within the global namespace.
 */
@API public interface TextDecoder: EncodingUtility {
  /**
   * ## Decoder Options
   *
   * Provides host-side access to `TextDecoder` options.
   */
  public val options: Options

  /**
   * ## Decode
   *
   * Decodes a byte stream into a string.
   */
  @Polyglot public fun decode(): String = "" // no-op

  /**
   * ## Decode (Host)
   *
   * Decodes a byte array into a string.
   *
   * @param bytes The byte array to decode.
   * @return The decoded string.
   */
  public fun decode(bytes: ByteArray): String

  /**
   * ## Decode (Host Streams)
   *
   * Decodes an input stream of bytes into a string.
   *
   * @param stream The stream to decode.
   * @return The decoded string.
   */
  public fun decode(stream: InputStream): String

  /**
   * ## Decode (Host Channels)
   *
   * Consumes and decodes an input channel into a string.
   *
   * @param channel The channel to consume.
   * @return The decoded string.
   */
  public fun decode(channel: ReadableByteChannel): String

  /**
   * ## Decode (Host Buffer)
   *
   * Consumes and decodes an input buffer of bytes to a string.
   *
   * @param buffer The buffer to consume.
   * @return The decoded string.
   */
  public fun decode(buffer: ByteBuffer): String

  /**
   * ## Decode
   *
   * Decodes a byte stream into a string.
   *
   * @param buffer The buffer to decode.
   * @return The decoded string.
   */
  @Polyglot public fun decode(buffer: Value?): String {
    if (buffer == null || buffer.isNull || !buffer.hasArrayElements())
      throw JsError.valueError("Expected non-null buffer or array type for `TextDecoder.decode`")
    return decode(buffer, DecodeOptions.DEFAULTS)
  }

  /**
   * ## Decode with Options
   *
   * Decodes a byte stream into a string, potentially with options.
   *
   * @param buffer The buffer to decode.
   * @param options The options to use when decoding.
   */
  @Polyglot public fun decode(buffer: Value?, options: Value?): String {
    if (buffer == null || buffer.isNull || !buffer.hasArrayElements())
      throw JsError.valueError("Expected non-null buffer or array type for `TextDecoder.decode`")
    val opts = if (options == null || options.isNull) DecodeOptions.DEFAULTS else DecodeOptions.from(options)
    return decode(buffer, opts)
  }

  /**
   * ## Decode with Options
   *
   * Decodes a byte stream into a string, potentially with options; this method is for host-side dispatch only.
   *
   * @param buffer The buffer to decode.
   * @param options The options to use when decoding.
   */
  public fun decode(buffer: Value, options: DecodeOptions): String

  /**
   * ## Host Decode
   *
   * Decodes a [ByteBuffer] using the provided [options] and this text decoder's assigned encoding.
   *
   * @param buffer The buffer to decode.
   * @param options The options to use when decoding.
   */
  public fun decode(buffer: ByteBuffer, options: DecodeOptions? = null): String

  override fun getMemberKeys(): Array<String> = TEXT_DECODER_METHODS_AND_PROPS
  override fun hasMember(key: String?): Boolean = key != null && key in TEXT_DECODER_METHODS_AND_PROPS
  override fun putMember(key: String?, value: Value?) { /* no-op */ }
  override fun removeMember(key: String?): Boolean = false

  /** Constant defaults used in the JavaScript Encoding API. */
  public object Defaults {
    /** Default value for [Options.fatal]. */
    public const val DEFAULT_FATAL: Boolean = false

    /** Default value for [Options.ignoreBOM]. */
    public const val DEFAULT_IGNORE_BOM: Boolean = false

    /** Default value for [DecodeOptions.stream]. */
    public const val DEFAULT_DECODE_STREAM: Boolean = false
  }

  /**
   * ## Decode Options
   *
   * Specifies options for calls to [TextDecoder.decode].
   */
    @JvmRecord public data class DecodeOptions private constructor (
    /**
     * ## Stream
     *
     * When set to `true`, the decoder will treat the input as a stream of bytes, and will not throw an error if the
     * input is incomplete.
     *
     * From MDN:
     * > A boolean flag indicating whether additional data will follow in subsequent calls to decode(). Set to true if
     * > processing the data in chunks, and false for the final chunk or if the data is not chunked.
     * > It defaults to false.
     */
    @get:Polyglot val stream: Boolean = Defaults.DEFAULT_DECODE_STREAM,
  ) {
    public companion object {
      /** Default settings for text decoder options. */
      @JvmStatic public val DEFAULTS: DecodeOptions = DecodeOptions()

      /** Create a new instance of the decoder options with the specified settings. */
      @JvmStatic public fun of(stream: Boolean = Defaults.DEFAULT_DECODE_STREAM): DecodeOptions =
        DecodeOptions(stream)

      /** Decode options from a guest value. */
      @JvmStatic public fun from(value: Value?): DecodeOptions = when (value) {
        null -> DEFAULTS
        else -> when {
          value.isNull -> DEFAULTS
          value.hasMembers() -> DecodeOptions(
            stream = value.getMember("stream")?.asBoolean() ?: Defaults.DEFAULT_DECODE_STREAM,
          )

          else -> DEFAULTS
        }
      }
    }
  }

  /**
   * ## Decoder Options
   *
   * Specifies options for [TextDecoder] instances; instances of these options can be provided to the [TextDecoder]
   * constructor to specify how the decoder should behave.
   */
    @JvmRecord public data class Options private constructor (
    /**
     * ## Fatal
     *
     * When set to `true`, the decoder will throw an error if it encounters an invalid byte sequence.
     */
    @get:Polyglot val fatal: Boolean = Defaults.DEFAULT_FATAL,

    /**
     * ## Ignore BOM
     *
     * When set to `true`, the decoder will ignore a byte order mark (BOM) at the beginning of the input.
     */
    @get:Polyglot val ignoreBOM: Boolean = Defaults.DEFAULT_IGNORE_BOM,
  ) {
    /** Factories and default values for text decoder options. */
    public companion object {
      /** Default settings for text encoder options. */
      @JvmStatic public val DEFAULTS: Options = Options()

      /**
       * ## Create
       *
       * Creates a new instance of the decoder options with the specified settings.
       *
       * @param fatal Whether the decoder should throw an error on invalid byte sequences.
       * @param ignoreBOM Whether the decoder should ignore a byte order mark.
       * @return The new instance.
       */
      @JvmStatic public fun of(fatal: Boolean = false, ignoreBOM: Boolean = false): Options =
        Options(fatal, ignoreBOM)

      /** Decode options from a guest value. */
      @JvmStatic public fun from(value: Value?): Options = when (value) {
        null -> DEFAULTS
        else -> when {
          value.isNull -> DEFAULTS
          value.hasMembers() -> Options(
            fatal = value.getMember("fatal")?.asBoolean() ?: false,
            ignoreBOM = value.getMember("ignoreBOM")?.asBoolean() ?: false,
          )

          else -> DEFAULTS
        }
      }
    }
  }

  /**
   * ## Text Decoder Factory
   *
   * Models constructor methods for [TextDecoder] instances; such objects can be constructed with no parameters, with an
   * optional "label" (the encoding to decode from), and a set of options.
   */
  @API public interface Factory: EncodingUtility.Factory<TextDecoder> {
    /**
     * ## Create with Label
     *
     * Creates a new instance of the encoding utility without any parameters.
     *
     * @return The new instance.
     */
    public fun create(label: String): TextDecoder

    /**
     * ## Create with Label
     *
     * Creates a new instance of the encoding utility without any parameters.
     *
     * @param encoding Encoding to use, as a valid JavaScript encoding "label" (name)
     * @return The new instance.
     */
    @Polyglot override fun create(encoding: Value?): TextDecoder {
      if ((encoding != null && !encoding.isNull) && !encoding.isString)
        throw JsError.typeError("`TextDecoder.Factory.create` expects a string label")
      return when (encoding) {
        null -> create()
        else -> create(encoding.asString())
      }
    }

    /**
     * ## Create with Options
     *
     * Creates a new instance of the encoding utility without any parameters.
     *
     * @return The new instance.
     */
    public fun create(label: String, options: Options): TextDecoder

    /**
     * ## Create with Options
     *
     * Creates a new instance of the encoding utility without any parameters.
     *
     * @return The new instance.
     */
    @Polyglot public fun create(label: Value?, options: Value?): TextDecoder {
      if (label == null || label.isNull || !label.isString)
        throw JsError.typeError("`TextDecoder.Factory.create` expects a string label")
      val opts = if (options == null || options.isNull) Options.DEFAULTS else Options.from(options)
      return create(label.asString(), opts)
    }

    override fun newInstance(vararg arguments: Value?): Any {
      if (arguments.isEmpty())
        return create()
      if (arguments.size == 1)
        return create(arguments[0])
      if (arguments.size == 2)
        return create(arguments[0], arguments[1])
      throw JsError.typeError("Expected 0-2 arguments for `TextDecoder(...)`")
    }
  }
}
