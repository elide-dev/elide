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

package elide.core.encoding.base64

import kotlin.math.min
import kotlin.native.concurrent.SharedImmutable
import elide.core.annotations.Static
import elide.core.encoding.Codec
import elide.core.encoding.Encoding

// Default globally-shared Base64 encoder.
@SharedImmutable private val defaultEncoder: Base64.Encoder = Base64.Encoder(
  null,
  -1,
  true,
)

// Default globally-shared Base64 encoder for web-safe outputs.
@SharedImmutable private val defaultEncoderWebsafe: Base64.Encoder = Base64.Encoder(
  null,
  -1,
  false,
)

// Default globally-shared decoder.
@SharedImmutable private val defaultDecoder: Base64.Decoder = Base64.Decoder()

/**
 * Base64: Native.
 */
@Suppress(
  "DuplicatedCode",
  "MagicNumber",
  "LoopWithTooManyJumpStatements",
  "ComplexMethod",
  "LongMethod",
  "NestedBlockDepth"
)
public actual object Base64 : Codec<Base64Data> {
  /**
   * This array is a lookup table that translates 6-bit positive integer index values into their "Base64 Alphabet"
   * equivalents as specified in "Table 1: The Base64 Alphabet" of RFC 2045 (and RFC 4648).
   */
  private val toBase64: CharArray = charArrayOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'
  )

  /**
   * It's the lookup table for "URL and Filename safe Base64" as specified in Table 2 of the RFC 4648, with the '+' and
   * '/' changed to '-' and '_'. This table is used when BASE64_URL is specified.
   */
  private val toBase64URL: CharArray = charArrayOf(
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
    'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
    'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
  )

  /**
   * Lookup table for decoding unicode characters drawn from the "Base64 Alphabet" (as specified in Table 1 of RFC 2045)
   * into their 6-bit positive integer equivalents. Characters that are not in the Base64 alphabet but fall within the
   * bounds of the array are encoded to -1.
   */
  private val fromBase64 = IntArray(256)

  /**
   * Lookup table for decoding "URL and Filename safe Base64 Alphabet" as specified in Table2 of the RFC 4648.
   */
  private val fromBase64URL = IntArray(256)

  init {
    fromBase64.fill(-1)
    for (i in toBase64.indices) fromBase64[toBase64[i].code] = i
    fromBase64['='.code] = -2

    fromBase64URL.fill(-1)
    for (i in toBase64URL.indices) fromBase64URL[toBase64URL[i].code] = i
    fromBase64URL['='.code] = -2
  }

  /**
   * Returns a [Encoder] that encodes using the [Basic](#basic) type base64 encoding scheme.
   *
   * @return A Base64 encoder.
   */
  private val encoder: Encoder = defaultEncoder

  /**
   * Returns a [Encoder] that encodes using the [Basic](#basic) type base64 encoding scheme, which does not use or apply
   * any padding (i.e. "web-safe" mode).
   *
   * @return A web-safe Base64 encoder.
   */
  private val encoderWebsafe: Encoder = defaultEncoderWebsafe

  /**
   * Returns a [Decoder] that decodes using the [Basic](#basic) type base64 encoding scheme.
   *
   * @return A Base64 decoder.
   */
  private val decoder: Decoder = defaultDecoder

  /**
   * This class implements an encoder for encoding byte data using the Base64 encoding scheme as specified in RFC 4648
   * and RFC 2045.
   *
   * Instances of [Encoder] class are safe for use by multiple concurrent threads.
   *
   * Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to
   * be thrown.
   *
   * @see Decoder for the corresponding decoder.
   */
  public actual class Encoder internal constructor(
    private val newline: ByteArray?,
    private val linemax: Int,
    private val doPadding: Boolean
  ) {
    // Empty constructor.
    public actual constructor(): this(null, -1, true)

    public actual companion object {
      /** Default encoder instance. */
      @Static
      public actual val DEFAULT: Encoder = encoder

      /** Default encoder instance for un-padded encoding. */
      @Static
      public actual val DEFAULT_WEBSAFE: Encoder = encoderWebsafe
    }

    private fun outLength(srclen: Int): Int {
      var len: Int = if (doPadding) {
        4 * ((srclen + 2) / 3)
      } else {
        val n = srclen % 3
        4 * (srclen / 3) + if (n == 0) 0 else n + 1
      }
      if (linemax > 0) // line separators
        len += (len - 1) / linemax * newline!!.size
      return len
    }

    /**
     * Encodes all bytes from the specified byte array into a newly-allocated byte array using the [Base64] encoding
     * scheme.The returned byte array is of the length of the resulting bytes.
     *
     * @param src the byte array to encode
     * @return A newly-allocated byte array containing the resulting encoded bytes.
     */
    public actual fun encode(src: ByteArray): ByteArray {
      val len = outLength(src.size) // dst array size
      val dst = ByteArray(len)
      val ret = encode0(src, src.size, dst)
      return if (ret != dst.size) dst.copyOf(ret) else dst
    }

    private fun encodeBlock(src: ByteArray, sp: Int, sl: Int, dst: ByteArray, dp: Int) {
      var sp0 = sp
      var dp0 = dp
      while (sp0 < sl) {
        val bits: Int = src[sp0++].toInt() and 0xff shl 16 or (
          src[sp0++].toInt() and 0xff shl 8) or
          (src[sp0++].toInt() and 0xff)
        dst[dp0++] = toBase64[bits ushr 18 and 0x3f].code.toByte()
        dst[dp0++] = toBase64[bits ushr 12 and 0x3f].code.toByte()
        dst[dp0++] = toBase64[bits ushr 6 and 0x3f].code.toByte()
        dst[dp0++] = toBase64[bits and 0x3f].code.toByte()
      }
    }

    @Suppress("UNUSED_CHANGED_VALUE")
    private fun encode0(src: ByteArray, end: Int, dst: ByteArray): Int {
      val base64 = toBase64
      val off = 0
      var sp = off
      var slen = (end - off) / 3 * 3
      val sl = off + slen
      if (linemax > 0 && slen > linemax / 4 * 3) slen = linemax / 4 * 3
      var dp = 0
      while (sp < sl) {
        val sl0: Int = min(sp + slen, sl)
        encodeBlock(src, sp, sl0, dst, dp)
        val dlen = (sl0 - sp) / 3 * 4
        dp += dlen
        sp = sl0
        if (dlen == linemax && sp < end) {
          for (b in newline!!) {
            dst[dp++] = b
          }
        }
      }
      if (sp < end) { // 1 or 2 leftover bytes
        val b0: Int = src[sp++].toInt() and 0xff
        dst[dp++] = base64[b0 shr 2].code.toByte()
        if (sp == end) {
          dst[dp++] = base64[b0 shl 4 and 0x3f].code.toByte()
          if (doPadding) {
            dst[dp++] = '='.code.toByte()
            dst[dp++] = '='.code.toByte()
          }
        } else {
          val b1: Int = src[sp++].toInt() and 0xff
          dst[dp++] = base64[b0 shl 4 and 0x3f or (b1 shr 4)].code.toByte()
          dst[dp++] = base64[b1 shl 2 and 0x3f].code.toByte()
          if (doPadding) {
            dst[dp++] = '='.code.toByte()
          }
        }
      }
      return dp
    }
  }

  /**
   * This class implements a decoder for decoding byte data using the Base64 encoding scheme as specified in RFC 4648
   * and RFC 2045.
   *
   * The Base64 padding character `'='` is accepted and interpreted as the end of the encoded byte data, but is not
   * required. So if the final unit of the encoded byte data only has two or three Base64 characters (without the
   * corresponding padding character(s) padded), they are decoded as if followed by padding character(s). If there is a
   * padding character present in the final unit, the correct number of padding character(s) must be present, otherwise
   * `IllegalArgumentException` (`IOException` when reading from a Base64 stream) is thrown during decoding.
   *
   * Instances of [Decoder] class are safe for use by multiple concurrent threads.
   *
   * Unless otherwise noted, passing a `null` argument to a method of this class will cause a `NullPointerException` to
   * be thrown.
   *
   * @see Encoder
   */
  public actual class Decoder {
    public actual companion object {
      /** Decoder singleton. */
      @Static
      public actual val DEFAULT: Decoder = decoder
    }

    /**
     * Decodes all bytes from the input byte array using the [Base64] encoding scheme, writing the results into a
     * newly-allocated output byte array. The returned byte array is of the length of the resulting bytes.
     *
     * @param src the byte array to decode
     * @return A newly-allocated byte array containing the decoded bytes.
     * @throws IllegalArgumentException if `src` is not in valid Base64 scheme
     */
    public actual fun decode(src: ByteArray): ByteArray {
      var dst = ByteArray(outLength(src, src.size))
      val ret = decode0(src, src.size, dst)
      if (ret != dst.size) {
        dst = dst.copyOf(ret)
      }
      return dst
    }

    private fun outLength(src: ByteArray, sl: Int): Int {
      var paddings = 0
      val sp = 0
      val len = sl - sp
      if (len == 0) return 0
      if (len < 2) {
        throw IllegalArgumentException(
          "Input byte[] should at least have 2 bytes for base64 bytes"
        )
      }
      if (src[sl - 1].toInt().toChar() == '=') {
        paddings++
        if (src[sl - 2].toInt().toChar() == '=') paddings++
      }
      if (paddings == 0 && len and 0x3 != 0) paddings = 4 - (len and 0x3)
      return 3 * ((len + 3) / 4) - paddings
    }

    private fun decode0(src: ByteArray, sl: Int, dst: ByteArray): Int {
      val spi = 0
      var sp = spi
      val base64 = fromBase64
      var dp = 0
      var bits = 0
      var shiftto = 18 // pos of first byte of 4-byte atom
      while (sp < sl) {
        if (shiftto == 18 && sp + 4 < sl) {       // fast path
          val sl0 = sp + (sl - sp and 3.inv())
          while (sp < sl0) {
            val b1 = base64[src[sp++].toInt() and 0xff]
            val b2 = base64[src[sp++].toInt() and 0xff]
            val b3 = base64[src[sp++].toInt() and 0xff]
            val b4 = base64[src[sp++].toInt() and 0xff]
            if (b1 or b2 or b3 or b4 < 0) {    // non base64 byte
              sp -= 4
              break
            }
            val bits0 = b1 shl 18 or (b2 shl 12) or (b3 shl 6) or b4
            dst[dp++] = (bits0 shr 16).toByte()
            dst[dp++] = (bits0 shr 8).toByte()
            dst[dp++] = bits0.toByte()
          }
          if (sp >= sl) break
        }
        var b: Int = src[sp++].toInt() and 0xff
        if (base64[b].also { b = it } < 0) {
          if (b == -2) {         // padding byte '='
            // =     shiftto==18 unnecessary padding
            // x=    shiftto==12 a dangling single x
            // x     to be handled together with non-padding case
            // xx=   shiftto==6&&sp==sl missing last =
            // xx=y  shiftto==6 last is not =
            require(
              !(shiftto == 6 && (sp == sl || src[sp++].toInt().toChar() != '=') ||
                shiftto == 18)
            ) { "Input byte array has wrong 4-byte ending unit" }
            break
          }
          throw IllegalArgumentException("Illegal base64 character " + src[sp - 1].toInt().toString(16))
        }
        bits = bits or (b shl shiftto)
        shiftto -= 6
        if (shiftto < 0) {
          dst[dp++] = (bits shr 16).toByte()
          dst[dp++] = (bits shr 8).toByte()
          dst[dp++] = bits.toByte()
          shiftto = 18
          bits = 0
        }
      }
      // reached end of byte array or hit padding '=' characters.
      when (shiftto) {
        6 -> {
          dst[dp++] = (bits shr 16).toByte()
        }
        0 -> {
          dst[dp++] = (bits shr 16).toByte()
          dst[dp++] = (bits shr 8).toByte()
        }
        else -> require(shiftto != 12) {
          // dangling single "x", incorrectly encoded.
          "Last unit does not have enough valid bits"
        }
      }
      // anything left is invalid, if is not MIME.
      // if MIME, ignore all non-base64 character
      while (sp < sl) {
        throw IllegalArgumentException(
          "Input byte array has incorrect ending byte at $sp"
        )
      }
      return dp
    }
  }

  /** @inheritDoc */
  actual override fun encoding(): Encoding = Encoding.BASE64

  /** @inheritDoc */
  override fun decodeBytes(data: ByteArray): ByteArray = decoder.decode(data)

  /** @inheritDoc */
  override fun encode(data: ByteArray): Base64Data = Base64Data(encoder.encode(data))

  /**
   * Encode the provided [string] into a Base64-encoded string, omitting characters which are unsafe for use on the web,
   * including padding characters, which are not emitted.
   *
   * @param string String to encode with web-safe Base64.
   * @return Base64-encoded string, using only web-safe characters.
   */
  public actual fun encodeWebSafe(string: String): String = encodeWebSafe(string.encodeToByteArray()).decodeToString()

  /**
   * Encode the provided [data] into a Base64-encoded set of bytes, omitting characters which are unsafe for use on the
   * web, including padding characters, which are not emitted.
   *
   * @param data Raw bytes to encode with web-safe Base64.
   * @return Base64-encoded bytes, using only web-safe characters.
   */
  public actual fun encodeWebSafe(data: ByteArray): ByteArray = encoderWebsafe.encode(data)
}
