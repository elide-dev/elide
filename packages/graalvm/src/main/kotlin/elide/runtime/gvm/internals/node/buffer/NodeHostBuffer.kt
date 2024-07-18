package elide.runtime.gvm.internals.node.buffer

import com.oracle.truffle.js.runtime.objects.Undefined
import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import java.nio.ByteOrder
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.intrinsics.js.node.buffer.BufferInstance

/**
 * A [NodeBufferInstance] backed by a NIO [byteBuffer]. The entirety of the buffer is used, thus [byteOffset] is always
 * zero and [length] is set to the remaining bytes in the buffer.
 *
 * The backing buffer's position and limit are assumed to remain constant and should therefore not be modified in any
 * case; doing so will cause unspecified behavior.
 */
@DelicateElideApi internal class NodeHostBuffer private constructor(
  internal val byteBuffer: ByteBuffer
) : NodeBufferInstance() {
  /**
   * Unlike the original Node.js Buffer instances, no byte offset is used by this class, as each buffer is backed by
   * its own [ByteBuffer], making this value always zero.
   */
  override val byteOffset: Int = 0

  /**
   * Returns the number of usable bytes in the backing [byteBuffer]. Note that methods in this class do not move the
   * buffer's position or limit, and as such both values are assumed to remain constant, making this property also a
   * constant calculated during construction.
   */
  override val length: Int = byteBuffer.remaining()

  /**
   * A simple value wrapper for the backing [byteBuffer]; the JavaScript engine will treat it as an `ArrayBuffer`
   * automatically, making it compatible with the Node.js API.
   */
  override val buffer: PolyglotValue = PolyglotValue.asValue(byteBuffer)

  override fun getBytes(target: ByteArray, offset: Int) {
    byteBuffer.get(offset, target, 0, target.size)
  }

  override fun getByte(index: Int): Byte {
    return byteBuffer[index]
  }

  override fun setByte(index: Int, value: Byte) {
    byteBuffer.put(index, value)
  }

  override fun get(index: Long): Any {
    // The semantics of the indexing operator in the Node Buffer type
    // indicate that out-of-bound indices return 'undefined'
    return if (index < 0 || index > length) Undefined.instance
    else byteBuffer[index.toInt()].toUByte().toInt()
  }

  override fun set(index: Long, value: Value?) {
    // The semantics of the indexing operator in the Node Buffer type
    // indicate that out-of-bound indices are treated as a noop rather
    // than throwing, and values are coerced to byte size
    if (index < 0 || index > length) return
    byteBuffer.put(index.toInt(), value?.asInt()?.toByte() ?: 0)
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
    // save state for both buffers
    val srcLimit = byteBuffer.limit()
    val srcPos = byteBuffer.position()

    val dstLimit = target.byteBuffer.limit()
    val dstPos = target.byteBuffer.position()

    // constrain buffers to parameters
    byteBuffer.position(sourceStart).limit(sourceEnd)
    target.byteBuffer.position(targetStart).limit(targetEnd)

    val comp = byteBuffer.compareTo(target.byteBuffer)

    // restore state
    byteBuffer.position(srcPos).limit(srcLimit)
    target.byteBuffer.position(dstPos).limit(dstLimit)

    return comp.coerceIn(-1, 1)
  }

  override fun copyTo(target: NodeHostBuffer, targetStart: Int, targetEnd: Int, sourceStart: Int, sourceEnd: Int): Int {
    val length = minOf(targetEnd - targetStart, sourceEnd - sourceStart)
    target.byteBuffer.put(targetStart, this.byteBuffer, sourceStart, length)

    return length
  }

  override fun fillWith(bytes: ByteArray, offset: Int, length: Int) {
    // optimized bulk fill, writing the entire source on each iteration until
    // the requested section is filled
    var i = 0
    while (i < length) {
      this.byteBuffer.put(offset + i, bytes, 0, minOf(length - i, bytes.size))
      i += bytes.size
    }
  }

  override fun fillWith(bytes: NodeHostBuffer, offset: Int, length: Int) {
    // optimized bulk fill, writing the entire source on each iteration until
    // the requested section is filled
    var i = 0
    while (i < length) {
      this.byteBuffer.put(offset + i, bytes.byteBuffer, 0, minOf(length - i, bytes.length))
      i += bytes.length
    }
  }

  override fun subarray(start: Int?, end: Int?): BufferInstance {
    val sliceStart = start ?: 0
    val sliceLength = (end ?: length) - sliceStart

    return NodeHostBuffer(byteBuffer.slice(start ?: 0, sliceLength))
  }

  override fun swap16(): BufferInstance {
    if (length % 2 != 0) throw JsError.valueError("Buffer length is not a multiple of 2")
    val leBuffer = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until length / 2) byteBuffer.putShort(i * 2, leBuffer.getShort(i * 2))
    return this
  }

  override fun swap32(): BufferInstance {
    if (length % 4 != 0) throw JsError.valueError("Buffer length is not a multiple of 4")
    val leBuffer = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until length / 4) byteBuffer.putInt(i * 4, leBuffer.getInt(i * 4))
    return this
  }

  override fun swap64(): BufferInstance {
    if (length % 8 != 0) throw JsError.valueError("Buffer length is not a multiple of 8")
    val leBuffer = byteBuffer.slice().order(ByteOrder.LITTLE_ENDIAN)

    for (i in 0 until length / 8) byteBuffer.putLong(i * 8, leBuffer.getLong(i * 8))
    return this
  }

  override fun readInt8(offset: Int?): Byte {
    return byteBuffer[offset ?: 0]
  }

  override fun readInt16BE(offset: Int?): Short {
    return byteBuffer.getShort(offset ?: 0)
  }

  override fun readInt16LE(offset: Int?): Short {
    return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort(offset ?: 0)
      .also { byteBuffer.order(ByteOrder.BIG_ENDIAN) }
  }

  override fun readInt32BE(offset: Int?): Int {
    return byteBuffer.getInt(offset ?: 0)
  }

  override fun readInt32LE(offset: Int?): Int {
    return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt(offset ?: 0)
      .also { byteBuffer.order(ByteOrder.BIG_ENDIAN) }
  }

  override fun readBigInt64BE(offset: Int?): Long {
    return byteBuffer.getLong(offset ?: 0)
  }

  override fun readBigInt64LE(offset: Int?): Long {
    return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getLong(offset ?: 0)
      .also { byteBuffer.order(ByteOrder.BIG_ENDIAN) }
  }

  override fun readIntBE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength) { i -> byteBuffer[offset + i] }
  }

  override fun readIntLE(offset: Int, byteLength: Int): Long {
    return readVarInt(offset, byteLength) { i -> byteBuffer[offset + (byteLength - i - 1)] }
  }

  override fun readFloatBE(offset: Int?): Float {
    return byteBuffer.getFloat(offset ?: 0)
  }

  override fun readFloatLE(offset: Int?): Float {
    return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getFloat(offset ?: 0)
      .also { byteBuffer.order(ByteOrder.BIG_ENDIAN) }
  }

  override fun readDoubleBE(offset: Int?): Double {
    return byteBuffer.getDouble(offset ?: 0)
  }

  override fun readDoubleLE(offset: Int?): Double {
    return byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getDouble(offset ?: 0)
      .also { byteBuffer.order(ByteOrder.BIG_ENDIAN) }
  }

  /* -- Write -- */

  override fun write(string: String, offset: Int?, length: Int?, encoding: String?) {
    val bytes = NodeBufferEncoding.encode(string, encoding)
    val start = offset ?: 0
    val size = length ?: bytes.size.coerceAtMost(this.length - start)

    byteBuffer.put(start, bytes, 0, size)
  }

  override fun writeInt8(value: Byte, offset: Int?): Int {
    byteBuffer.put(offset ?: 0, value)
    return (offset ?: 0) + 1
  }

  override fun writeInt16BE(value: Short, offset: Int?): Int {
    byteBuffer.putShort(offset ?: 0, value)
    return (offset ?: 0) + 2
  }

  override fun writeInt16LE(value: Short, offset: Int?): Int {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putShort(offset ?: 0, value).order(ByteOrder.BIG_ENDIAN)
    return (offset ?: 0) + 2
  }

  override fun writeInt32BE(value: Int, offset: Int?): Int {
    byteBuffer.putInt(offset ?: 0, value)
    return (offset ?: 0) + 4
  }

  override fun writeInt32LE(value: Int, offset: Int?): Int {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putInt(offset ?: 0, value).order(ByteOrder.BIG_ENDIAN)
    return (offset ?: 0) + 4
  }

  override fun writeBigInt64BE(value: Long, offset: Int?): Int {
    byteBuffer.putLong(offset ?: 0, value)
    return (offset ?: 0) + 8
  }

  override fun writeBigInt64LE(value: Long, offset: Int?): Int {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putLong(offset ?: 0, value).order(ByteOrder.BIG_ENDIAN)
    return (offset ?: 0) + 8
  }

  override fun writeIntBE(value: Long, offset: Int, byteLength: Int): Int {
    writeVarInt(value, offset, byteLength) { i, b -> byteBuffer.put(offset + i, b) }
    return offset + byteLength
  }

  override fun writeIntLE(value: Long, offset: Int, byteLength: Int): Int {
    writeVarInt(value, offset, byteLength) { i, b -> byteBuffer.put(offset + (byteLength - i), b) }
    return offset + byteLength
  }

  override fun writeFloatBE(value: Float, offset: Int?): Int {
    byteBuffer.putFloat(offset ?: 0, value)
    return (offset ?: 0) + 4
  }

  override fun writeFloatLE(value: Float, offset: Int?): Int {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putFloat(offset ?: 0, value).order(ByteOrder.BIG_ENDIAN)
    return (offset ?: 0) + 4
  }

  override fun writeDoubleBE(value: Double, offset: Int?): Int {
    byteBuffer.putDouble(offset ?: 0, value)
    return (offset ?: 0) + 8
  }

  override fun writeDoubleLE(value: Double, offset: Int?): Int {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN).putDouble(offset ?: 0, value).order(ByteOrder.BIG_ENDIAN)
    return (offset ?: 0) + 8
  }

  internal companion object {
    /**
     * Returns a new [NodeHostBuffer] wrapping the given [bytes] with a NIO buffer.
     */
    internal fun wrap(bytes: ByteArray): NodeHostBuffer {
      return NodeHostBuffer(ByteBuffer.wrap(bytes))
    }

    /**
     * Returns a new [NodeHostBuffer] wrapping a [ByteArray] of the given [length] with a NIO buffer.
     */
    internal fun allocate(size: Int): NodeHostBuffer {
      return NodeHostBuffer(ByteBuffer.allocate(size))
    }
  }
}
