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
@file:Suppress("MagicNumber", "TooManyFunctions")

package elide.runtime.gvm.internals.node.buffer

import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import java.nio.ByteOrder
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.js.node.buffer.BufferInstance

/**
 * A [NodeBufferInstance] using a guest [PolyglotValue] with buffer elements as a backing buffer. Only the part of the
 * [buffer] starting at [byteOffset] and ending at [byteOffset] + [length] is used in buffer operations.
 */
@DelicateElideApi internal class NodeGuestBuffer private constructor(
  override val buffer: PolyglotValue,
  override val byteOffset: Int,
  override val length: Int,
) : NodeBufferInstance() {
  /**
   * Adjust an [index] using this buffer's [byteOffset], effectively mapping a relative input index to an absolute
   * index in the backing [buffer].
   */
  private fun mapIndex(index: Long): Long {
    return byteOffset + index
  }

  /**
   * Adjust an [index] using this buffer's [byteOffset], effectively mapping a relative input index to an absolute
   * index in the backing [buffer].
   */
  private fun mapIndex(index: Int): Long {
    return (byteOffset + index).toLong()
  }

  override fun getBytes(target: ByteArray, offset: Int) {
    buffer.readBuffer(
      /* byteOffset = */ mapIndex(offset),
      /* destination = */ target,
      /* destinationOffset = */0,
      /* length = */ target.size,
    )
  }

  override fun getByte(index: Int): Byte {
    return buffer.readBufferByte(mapIndex(index))
  }

  override fun setByte(index: Int, value: Byte) {
    buffer.writeBufferByte(mapIndex(index), value)
  }

  override fun get(index: Long): Any {
    val mappedIndex = mapIndex(index)

    // The semantics of the indexing operator in the Node Buffer type
    // indicate that out-of-bound indices return 'undefined'
    return if (mappedIndex < 0 || mappedIndex > length) Undefined.instance
    else buffer.readBufferByte(mappedIndex).toUByte().toInt()
  }

  override fun set(index: Long, value: Value?) {
    val mappedIndex = mapIndex(index)

    // The semantics of the indexing operator in the Node Buffer type
    // indicate that out-of-bound indices are treated as a noop rather
    // than throwing, and values are coerced to byte size
    if (mappedIndex < 0 || mappedIndex > length) return
    buffer.writeBufferByte(mappedIndex, value?.asInt()?.toByte() ?: 0)
  }

  override fun getSize(): Long {
    return length.toLong()
  }

  override fun compareTo(
    target: NodeHostBuffer,
    targetStart: Int,
    targetEnd: Int,
    sourceStart: Int,
    sourceEnd: Int
  ): Int {
    return compareUsing(
      targetStart, targetEnd, sourceStart, sourceEnd,
      readTarget = { i -> target.byteBuffer[i] },
    )
  }

  override fun copyTo(target: NodeHostBuffer, targetStart: Int, targetEnd: Int, sourceStart: Int, sourceEnd: Int): Int {
    return copyUsing(
      targetStart, targetEnd, sourceStart, sourceEnd,
      writeTarget = { i, b -> target.byteBuffer.put(i, b) },
    )
  }

  override fun subarray(start: Int?, end: Int?): BufferInstance {
    val subStart = start ?: 0
    val subEnd = end ?: length

    return NodeGuestBuffer(buffer, byteOffset + subStart, subEnd - subStart)
  }

  override fun swap16(): BufferInstance {
    if (length % 2 != 0) throw JsError.valueError("Buffer length is not a multiple of 2")
    for (i in 0 until length / 2) {
      val mappedIndex = mapIndex(i * 2)
      buffer.writeBufferShort(
        /* order = */ ByteOrder.LITTLE_ENDIAN,
        /* byteOffset = */ mappedIndex,
        /* value = */ buffer.readBufferShort(ByteOrder.BIG_ENDIAN, mappedIndex),
      )
    }

    return this
  }

  override fun swap32(): BufferInstance {
    if (length % 4 != 0) throw JsError.valueError("Buffer length is not a multiple of 4")
    for (i in 0 until length / 4) {
      val mappedIndex = mapIndex(i * 4)
      buffer.writeBufferInt(
        /* order = */ ByteOrder.LITTLE_ENDIAN,
        /* byteOffset = */ mappedIndex,
        /* value = */ buffer.readBufferInt(ByteOrder.BIG_ENDIAN, mappedIndex),
      )
    }

    return this
  }

  override fun swap64(): BufferInstance {
    if (length % 8 != 0) throw JsError.valueError("Buffer length is not a multiple of 8")
    for (i in 0 until length / 8) {
      val mappedIndex = mapIndex(i * 8)
      buffer.writeBufferLong(
        /* order = */ ByteOrder.LITTLE_ENDIAN,
        /* byteOffset = */ mappedIndex,
        /* value = */ buffer.readBufferLong(ByteOrder.BIG_ENDIAN, mappedIndex),
      )
    }

    return this
  }

  /* -- Read -- */

  override fun readInt8(offset: Int?): Byte {
    return buffer.readBufferByte(mapIndex(offset ?: 0))
  }

  override fun readInt16BE(offset: Int?): Short {
    return buffer.readBufferShort(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readInt16LE(offset: Int?): Short {
    return buffer.readBufferShort(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readInt32BE(offset: Int?): Int {
    return buffer.readBufferInt(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readInt32LE(offset: Int?): Int {
    return buffer.readBufferInt(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readBigInt64BE(offset: Int?): Long {
    return buffer.readBufferLong(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readBigInt64LE(offset: Int?): Long {
    return buffer.readBufferLong(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readIntBE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength) { i -> getByte(offset + i) }
  }

  override fun readUIntBE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength, signed = false) { i -> getByte(offset + i) }
  }

  override fun readIntLE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength) { i -> getByte(offset + (byteLength - i)) }
  }

  override fun readUIntLE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength, signed = false) { i -> getByte(offset + (byteLength - i)) }
  }

  override fun readFloatBE(offset: Int?): Float {
    return buffer.readBufferFloat(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readFloatLE(offset: Int?): Float {
    return buffer.readBufferFloat(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readDoubleBE(offset: Int?): Double {
    return buffer.readBufferDouble(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0))
  }

  override fun readDoubleLE(offset: Int?): Double {
    return buffer.readBufferDouble(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0))
  }

  /* -- Write -- */

  override fun write(string: String, offset: Int?, length: Int?, encoding: String?): Int {
    val bytes = NodeBufferEncoding.encode(string, encoding)
    val start = offset ?: 0
    val size = length ?: bytes.size.coerceAtMost(this.length - start)

    for (i in 0 until size) setByte(start + i, bytes[i])
    return size
  }

  override fun writeInt8(value: Byte, offset: Int?): Int {
    buffer.writeBufferByte(mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 1
  }

  override fun writeInt16BE(value: Short, offset: Int?): Int {
    buffer.writeBufferShort(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 2
  }

  override fun writeInt16LE(value: Short, offset: Int?): Int {
    buffer.writeBufferShort(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 2
  }

  override fun writeInt32BE(value: Int, offset: Int?): Int {
    buffer.writeBufferInt(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 4
  }

  override fun writeInt32LE(value: Int, offset: Int?): Int {
    buffer.writeBufferInt(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 4
  }

  override fun writeBigInt64BE(value: Long, offset: Int?): Int {
    buffer.writeBufferLong(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 8
  }

  override fun writeBigInt64LE(value: Long, offset: Int?): Int {
    buffer.writeBufferLong(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 8
  }

  override fun writeIntBE(value: Long, offset: Int, byteLength: Int): Int {
    writeVarInt(value, offset, byteLength) { i, b -> setByte(offset + i, b) }
    return offset + byteLength
  }

  override fun writeIntLE(value: Long, offset: Int, byteLength: Int): Int {
    writeVarInt(value, offset, byteLength) { i, b -> setByte(offset + (byteLength - i), b) }
    return offset + byteLength
  }

  override fun writeFloatBE(value: Float, offset: Int?): Int {
    buffer.writeBufferFloat(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 4
  }

  override fun writeFloatLE(value: Float, offset: Int?): Int {
    buffer.writeBufferFloat(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 4
  }

  override fun writeDoubleBE(value: Double, offset: Int?): Int {
    buffer.writeBufferDouble(ByteOrder.BIG_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 8
  }

  override fun writeDoubleLE(value: Double, offset: Int?): Int {
    buffer.writeBufferDouble(ByteOrder.LITTLE_ENDIAN, mapIndex(offset ?: 0), value)
    return (offset ?: 0) + 8
  }

  internal companion object {
    /**
     * Constructs a new [NodeGuestBuffer] instance using an `ArrayBuffer` [value]. Input parameters are checked to
     * ensure a valid view over the backing buffer is created.
     */
    internal fun from(value: PolyglotValue, byteOffset: Int, length: Int): NodeGuestBuffer {
      require(value.hasBufferElements()) { "Backing value must have buffer elements" }
      require(byteOffset + length < value.bufferSize) { "Offset + length must not exceed backing buffer bounds" }

      return NodeGuestBuffer(value, byteOffset, length)
    }
  }
}
