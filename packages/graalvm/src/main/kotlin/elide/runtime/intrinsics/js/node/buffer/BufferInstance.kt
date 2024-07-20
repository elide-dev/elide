package elide.runtime.intrinsics.js.node.buffer

import com.oracle.truffle.js.runtime.BigInt
import com.oracle.truffle.js.runtime.builtins.JSNumber
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue

/**
 * Represents an instance of the `Buffer` type in a Node.js-compatible runtime. Buffer instances are views over an
 * `ArrayBuffer`, which is a [PolyglotValue] with [buffer elements][org.graalvm.polyglot.Value.hasBufferElements]; the
 * [byteOffset] property indicates at which index in the backing [array buffer][buffer] the view starts, and the
 * [length] specifies the number of bytes available.
 *
 * All Buffer operations are performed using the backing array buffer, however it is up to the implementation to define
 * the nature of the backing buffer itself (e.g. it could be a NIO `ByteBuffer`).
 */
@DelicateElideApi public interface BufferInstance : ProxyArray, ProxyObject {
  /**
   * Returns the backing `ArrayBuffer` for this buffer instance. The [byteOffset] indicates the index of this backing
   * value at which the view represented by this Buffer begins.
   */
  public val buffer: PolyglotValue

  /**
   * The index in the backing [buffer] at which this view starts.
   */
  public val byteOffset: Int

  /**
   * The number of bytes exposed from the backing [buffer], starting at [byteOffset].
   */
  public val length: Int

  /**
   * Compare this buffer with either another buffer instance or a `Uint8Array`, optionally using only delimited
   * segments of both values.
   */
  public fun compare(
    target: PolyglotValue,
    targetStart: Int? = null,
    targetEnd: Int? = null,
    sourceStart: Int? = null,
    sourceEnd: Int? = null,
  ): Int

  /**
   * Copies the contents of this buffer to a [target], optionally starting at an offset and limiting the copied region
   * from the source.
   */
  public fun copy(
    target: PolyglotValue,
    targetStart: Int? = null,
    sourceStart: Int? = null,
    sourceEnd: Int? = null,
  ): Int

  /**
   * Fill the buffer starting at [offset] and stopping at [end] with the given [value]. If the value is a string, it
   * is converted to bytes using [encoding] (UTF-8 by default).
   *
   * If the value is a non-number (i.e. Buffer, Uint8Array, or string) and does not fill the requested range, it will
   * be looped to compensate for missing length.
   */
  public fun fill(
    value: PolyglotValue,
    offset: Int? = null,
    end: Int? = null,
    encoding: String? = null
  ): BufferInstance

  /**
   * Returns the byte index of the first occurrence of [value] in the buffer after [byteOffset], or -1 if it is not
   * found. The value may be a number (coerced into byte size), a string, another buffer, or a Uint8Array. The
   * [encoding] will be used for string values if available.
   */
  public fun indexOf(value: PolyglotValue, byteOffset: Int? = null, encoding: String? = null): Int

  /**
   * Returns the byte index of the last occurrence of [value] in the buffer after [byteOffset], or -1 if it is not
   * found. The value may be a number (coerced into byte size), a string, another buffer, or a Uint8Array. The
   * [encoding] will be used for string values if available.
   */
  public fun lastIndexOf(value: PolyglotValue, byteOffset: Int? = null, encoding: String? = null): Int

  /**
   * Returns a new Buffer instance as a view of this buffer's contents in the given range. Updates to this buffer's
   * data are visible to the returned instance.
   */
  public fun subarray(start: Int? = null, end: Int? = null): BufferInstance

  /**
   * Interpret this buffer as containing only 16-bit integers and swap their byte order in-place. If the buffer's byte
   * length is not a multiple of 2, an error will be thrown.
   */
  public fun swap16(): BufferInstance

  /**
   * Interpret this buffer as containing only 32-bit integers and swap their byte order in-place. If the buffer's byte
   * length is not a multiple of 4, an error will be thrown.
   */
  public fun swap32(): BufferInstance

  /**
   * Interpret this buffer as containing only 64-bit integers and swap their byte order in-place. If the buffer's byte
   * length is not a multiple of 8, an error will be thrown.
   */
  public fun swap64(): BufferInstance

  /**
   * Encode a [string] and write it into the buffer at the given [offset], optionally truncating the resulting bytes
   * to match a specific [length].
   */
  public fun write(string: String, offset: Int? = null, length: Int? = null, encoding: String? = null): Int

  /* -- Byte -- */

  /**
   * Read an 8-bit integer from the buffer, optionally at an offset.
   */
  public fun readInt8(offset: Int?): Byte

  /**
   * Read an 8-bit unsigned integer from the buffer, optionally at an offset.
   */
  public fun readUInt8(offset: Int?): Int = readInt8(offset).toUByte().toInt()

  /**
   * Write an 8-bit signed integer to the buffer at an offset.
   */
  public fun writeInt8(value: Byte, offset: Int?): Int

  /**
   * Write an 8-bit unsigned integer to the buffer at an offset.
   */
  public fun writeUInt8(value: Int, offset: Int?): Int = writeInt8(value.toUByte().toByte(), offset)

  /* -- Short -- */

  /**
   * Read a 16-bit integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readInt16BE(offset: Int?): Short

  /**
   * Read a 16-bit unsigned integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readUInt16BE(offset: Int?): Int = readInt16BE(offset).toUShort().toInt()

  /**
   * Read a 16-bit integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readInt16LE(offset: Int?): Short

  /**
   * Read a 16-bit unsigned integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readUInt16LE(offset: Int?): Int = readInt16LE(offset).toUShort().toInt()

  /**
   * Write an 16-bit signed integer to the buffer at an offset using Big Endian order.
   */
  public fun writeInt16BE(value: Short, offset: Int?): Int

  /**
   * Write an 16-bit unsigned integer to the buffer at an offset using Big Endian order.
   */
  public fun writeUInt16BE(value: Int, offset: Int?): Int = writeInt16BE(value.toUShort().toShort(), offset)

  /**
   * Write an 16-bit signed integer to the buffer at an offset using Little Endian order.
   */
  public fun writeInt16LE(value: Short, offset: Int?): Int

  /**
   * Write an 16-bit unsigned integer to the buffer at an offset using Little Endian order.
   */
  public fun writeUInt16LE(value: Int, offset: Int?): Int = writeInt16LE(value.toUShort().toShort(), offset)

  /* -- Int -- */

  /**
   * Read a 32-bit integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readInt32BE(offset: Int?): Int

  /**
   * Read a 32-bit unsigned integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readUInt32BE(offset: Int?): Long = readInt32BE(offset).toUInt().toLong()

  /**
   * Read a 32-bit integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readInt32LE(offset: Int?): Int

  /**
   * Read a 32-bit unsigned integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readUInt32LE(offset: Int?): Long = readInt32LE(offset).toUInt().toLong()

  /**
   * Write an 32-bit signed integer to the buffer at an offset using Big Endian order.
   */
  public fun writeInt32BE(value: Int, offset: Int?): Int

  /**
   * Write an 32-bit unsigned integer to the buffer at an offset using Big Endian order.
   */
  public fun writeUInt32BE(value: Long, offset: Int?): Int = writeInt32BE(value.toUInt().toInt(), offset)

  /**
   * Write an 32-bit signed integer to the buffer at an offset using Little Endian order.
   */
  public fun writeInt32LE(value: Int, offset: Int?): Int

  /**
   * Write an 32-bit unsigned integer to the buffer at an offset using Little Endian order.
   */
  public fun writeUInt32LE(value: Long, offset: Int?): Int = writeInt32LE(value.toUInt().toInt(), offset)

  /* -- Long -- */

  /**
   * Read a 64-bit integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readBigInt64BE(offset: Int?): Long

  /**
   * Read a 64-bit unsigned integer from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readBigUInt64BE(offset: Int?): BigInt = BigInt.valueOfUnsigned(readBigInt64BE(offset))

  /**
   * Read a 64-bit integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readBigInt64LE(offset: Int?): Long

  /**
   * Read a 64-bit unsigned integer from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readBigUInt64LE(offset: Int?): BigInt = BigInt.valueOfUnsigned(readBigInt64LE(offset))

  /**
   * Write an 64-bit signed integer to the buffer at an offset using Big Endian order.
   */
  public fun writeBigInt64BE(value: Long, offset: Int?): Int

  /**
   * Write an 64-bit unsigned integer to the buffer at an offset using Big Endian order.
   */
  public fun writeBigUInt64BE(value: Long, offset: Int?): Int = writeBigInt64BE(value, offset)

  /**
   * Write an 64-bit signed integer to the buffer at an offset using Little Endian order.
   */
  public fun writeBigInt64LE(value: Long, offset: Int?): Int

  /**
   * Write an 64-bit unsigned integer to the buffer at an offset using Little Endian order.
   */
  public fun writeBigUInt64LE(value: Long, offset: Int?): Int = writeBigInt64LE(value, offset)

  /* -- Variable Length Integer -- */

  /**
   * Read an integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Big Endian byte order,
   * at the given [offset].
   */
  public fun readIntBE(offset: Int, byteLength: Int): Long

  /**
   * Read an unsigned integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Big Endian
   * byte order, at the given [offset].
   */
  public fun readUIntBE(offset: Int, byteLength: Int): Long

  /**
   * Read an integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Little Endian byte
   * order, at the given [offset].
   */
  public fun readIntLE(offset: Int, byteLength: Int): Long

  /**
   * Read an unsigned integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Little Endian
   * byte order, at the given [offset].
   */
  public fun readUIntLE(offset: Int, byteLength: Int): Long

  /**
   * WRite an integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Big Endian byte order,
   * at the given [offset].
   */
  public fun writeIntBE(value: Long, offset: Int, byteLength: Int): Int

  /**
   * Read an unsigned integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Big Endian
   * byte order, at the given [offset].
   */
  public fun writeUIntBE(value: Long, offset: Int, byteLength: Int): Int = writeIntBE(value, offset, byteLength)

  /**
   * Read an integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Little Endian byte
   * order, at the given [offset].
   */
  public fun writeIntLE(value: Long, offset: Int, byteLength: Int): Int

  /**
   * Read an unsigned integer with the specified [byteLength] (in bytes, up to 6 bytes/48 bits) using the Little Endian
   * byte order, at the given [offset].
   */
  public fun writeUIntLE(value: Long, offset: Int, byteLength: Int): Int = writeIntLE(value, offset, byteLength)

  /* -- Float -- */

  /**
   * Read a 32-bit floating point value from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun readFloatBE(offset: Int?): Float

  /**
   * Read a 32-bit floating point value from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun readFloatLE(offset: Int?): Float

  /**
   * Write a 32-bit floating point value from the buffer using the Big Endian byte order, optionally at an offset.
   */
  public fun writeFloatBE(value: Float, offset: Int?): Int

  /**
   * Write a 32-bit floating point value from the buffer using the Little Endian byte order, optionally at an offset.
   */
  public fun writeFloatLE(value: Float, offset: Int?): Int

  /* -- Double -- */

  /**
   * Read a 64-bit double-precision floating point value from the buffer using the Big Endian byte order, optionally
   * at an offset.
   */
  public fun readDoubleBE(offset: Int?): Double

  /**
   * Read a 64-bit double-precision floating point value from the buffer using the Little Endian byte order, optionally
   * at an offset.
   */
  public fun readDoubleLE(offset: Int?): Double

  /**
   * Write a 64-bit double-precision floating point value from the buffer using the Big Endian byte order, optionally
   * at an offset.
   */
  public fun writeDoubleBE(value: Double, offset: Int?): Int

  /**
   * Write a 64-bit double-precision floating point value from the buffer using the Little Endian byte order,
   * optionally at an offset.
   */
  public fun writeDoubleLE(value: Double, offset: Int?): Int

  /* -- Misc -- */

  /**
   * Returns a JSON object representation of this buffer, with the data rendered into an array of byte values.
   */
  public fun toJSON(): PolyglotValue

  /**
   * Decodes the buffer's data as a string using the given encoding.
   */
  public fun toString(encoding: String?, start: Int?, end: Int?): String

  /**
   * Returns an iterable value which yields the buffer's keys (indices).
   */
  public fun keys(): PolyglotValue

  /**
   * Returns an iterable value which yields the buffer's byte values.
   */
  public fun values(): PolyglotValue

  /**
   * Returns an iterable value which yields, for every byte in the buffer, an array with the index as the first element
   * and the byte as a second element.
   */
  public fun entries(): PolyglotValue
}
