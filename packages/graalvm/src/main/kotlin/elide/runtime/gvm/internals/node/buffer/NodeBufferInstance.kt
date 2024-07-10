package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyIterable
import org.graalvm.polyglot.proxy.ProxyObject
import kotlin.experimental.and
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsError
import elide.runtime.gvm.internals.intrinsics.js.JsProxy
import elide.runtime.intrinsics.js.node.buffer.BufferInstance

/**
 * A base implementation for the Node.js [BufferInstance] API, serving as a template for Buffer instances without
 * specifying the backing `ArrayBuffer` value.
 */
@DelicateElideApi internal sealed class NodeBufferInstance : BufferInstance, ProxyObject {
  /** Attempts to interpret this value as a [NodeBufferInstance] and unwrap it, returning `null` on failure. */
  protected fun PolyglotValue.asBufferInstance(): NodeBufferInstance? {
    return if (isProxyObject) runCatching { asProxyObject<NodeBufferInstance>() }.getOrNull()
    else null
  }

  /** Attempts to interpret this value as a [GuestBufferView] and unwrap it, returning `null` on failure. */
  protected fun PolyglotValue.asBufferView(): GuestBufferView? {
    return GuestBufferView.tryFrom(this)
  }

  /**
   * Interprets this value as either a [NodeBufferInstance] or a [GuestBufferView], serving as a quick type safety
   * helper in cases where only those two types matter.
   */
  protected inline fun <R> PolyglotValue.whenBufferOrView(
    onBuffer: (NodeBufferInstance) -> R,
    onView: (GuestBufferView) -> R,
    default: () -> R = { throw JsError.typeError("Expected Buffer or UintArray value") },
  ): R {
    // check if it's another Buffer
    this.asBufferInstance()?.let { return onBuffer(it) }

    // should be an Uint8Array instead
    this.asBufferView()?.let { return onView(it) }

    // fallback
    return default()
  }

  /* ------- Helpers ------- */

  /** Read the byte value at the given [index] in the buffer. */
  protected abstract fun getByte(index: Int): Byte

  /** Set the byte [value] at the given [index] in the buffer. */
  protected abstract fun setByte(index: Int, value: Byte)

  /**
   * Read the contents of the buffer into the [target] array until it is filled, starting at the given [offset].
   */
  protected abstract fun getBytes(target: ByteArray, offset: Int = 0)

  /**
   * Read the contents of this buffer into a [ByteArray] and return them. This is the equivalent of creating a new
   * [ByteArray] with this buffer's [length] and using [getBytes] to fill it.
   */
  protected fun getBytes(start: Int = 0, end: Int = length): ByteArray {
    return ByteArray(end - start).also { getBytes(it, start) }
  }

  /* ------- Compare ------- */

  /**
   * Compare two buffer-like values using the given offsets and limits. The [readTarget] parameter and the [getByte]
   * function are used to obtain one byte from each operand at the time.
   *
   * This is considered a slow path for comparison as it will likely not be vectorized automatically during
   * compilation.
   */
  protected inline fun compareUsing(
    targetStart: Int,
    targetEnd: Int,
    sourceStart: Int,
    sourceEnd: Int,
    readTarget: (Int) -> Byte,
  ): Int {
    val sourceSize = sourceEnd - sourceStart
    val targetSize = targetEnd - targetStart

    for (i in 0 until minOf(sourceSize, targetSize)) {
      val c = getByte(sourceStart + i).compareTo(readTarget(targetStart + i))
      if (c != 0) return c.coerceIn(-1, 1)
    }

    return sourceSize.compareTo(targetSize).coerceIn(-1, 1)
  }

  /**
   * Compare to a [target] host buffer using the given offsets. This specialized method allows optimized comparisons
   * when both values are host buffers, and faster reads in the other cases.
   */
  protected abstract fun compareTo(
    target: NodeHostBuffer,
    targetStart: Int,
    targetEnd: Int,
    sourceStart: Int,
    sourceEnd: Int
  ): Int

  override fun compare(
    target: PolyglotValue,
    targetStart: Int?,
    targetEnd: Int?,
    sourceStart: Int?,
    sourceEnd: Int?
  ): Int = target.whenBufferOrView(
    onBuffer = { other ->
      val targetOffset = targetStart ?: 0
      val sourceOffset = sourceStart ?: 0

      when (other) {
        is NodeHostBuffer -> compareTo(
          target = other,
          targetStart = targetOffset,
          targetEnd = targetEnd ?: (targetOffset + other.size).toInt(),
          sourceStart = sourceOffset,
          sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
        )

        is NodeGuestBuffer -> compareUsing(
          targetStart = other.byteOffset + targetOffset,
          targetEnd = other.byteOffset + (targetEnd ?: (targetOffset + other.size).toInt()),
          sourceStart = sourceOffset,
          sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
          readTarget = { other.buffer.readBufferByte(it.toLong()) },
        )
      }
    },
    onView = { view ->
      val targetBaseOffset = view.byteOffset()
      val sourceOffset = sourceStart ?: 0
      val bytes = view.bytes()

      compareUsing(
        targetStart = targetBaseOffset + (targetStart ?: 0),
        targetEnd = targetBaseOffset + (targetEnd ?: view.byteSize()),
        sourceStart = sourceOffset,
        sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
        readTarget = { bytes[it] },
      )
    },
  )

  /* ------- Copy ------- */

  /**
   * Copy between two buffer-like values using the given offsets and limits. The [getByte] and [writeTarget]
   * functions are used to transfer one byte at the time.
   *
   * This is considered a slow path for comparison as it will likely not be vectorized automatically during
   * compilation.
   */
  protected inline fun copyUsing(
    targetStart: Int,
    targetEnd: Int,
    sourceStart: Int,
    sourceEnd: Int,
    writeTarget: (Int, Byte) -> Unit,
  ): Int {
    val totalBytes = minOf(sourceEnd - sourceStart, targetEnd - targetStart)
    for (i in 0 until totalBytes) writeTarget(targetStart + i, getByte(sourceStart + i))
    return totalBytes
  }

  /**
   * Copy to a [target] host buffer using the given offsets. This specialized method allows optimized copying when both
   * values are host buffers, and faster reads in the other cases.
   */
  protected abstract fun copyTo(
    target: NodeHostBuffer,
    targetStart: Int,
    targetEnd: Int,
    sourceStart: Int,
    sourceEnd: Int
  ): Int

  override fun copy(target: PolyglotValue, targetStart: Int?, sourceStart: Int?, sourceEnd: Int?): Int =
    target.whenBufferOrView(
      onBuffer = { other ->
        val targetOffset = targetStart ?: 0
        val sourceOffset = sourceStart ?: 0

        when (other) {
          is NodeHostBuffer -> copyTo(
            target = other,
            targetStart = targetOffset,
            targetEnd = (targetOffset + other.size).toInt(),
            sourceStart = sourceOffset,
            sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
          )

          is NodeGuestBuffer -> copyUsing(
            targetStart = other.byteOffset + targetOffset,
            targetEnd = other.byteOffset + (targetOffset + other.size).toInt(),
            sourceStart = sourceOffset,
            sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
            writeTarget = { i, b -> other.buffer.writeBufferByte(i.toLong(), b) },
          )
        }
      },
      onView = { view ->
        val targetBaseOffset = view.byteOffset()
        val sourceOffset = sourceStart ?: 0
        val bytes = view.bytes()

        copyUsing(
          targetStart = targetBaseOffset + (targetStart ?: 0),
          targetEnd = targetBaseOffset + view.byteSize(),
          sourceStart = sourceOffset,
          sourceEnd = sourceEnd ?: (sourceOffset + this.size).toInt(),
          writeTarget = { i, b -> bytes[i] = b },
        )
      },
    )

  /* ------- Fill ------- */

  /**
   * Fill the segment of the buffer of [length] starting at [offset] with [byte].
   */
  protected open fun fillWith(byte: Byte, offset: Int, length: Int) {
    for (i in 0 until length) setByte(offset + i, byte)
  }

  /**
   * Fill the segment of the buffer of [length] starting at [offset] with [bytes]. If the segment is longer than the
   * source then the [bytes] will be looped until no more fill is needed.
   */
  internal open fun fillWith(bytes: ByteArray, offset: Int, length: Int) {
    // repeat the source bytes until the length is reached
    for (i in 0 until length) setByte(i + offset, bytes[i % bytes.size])
  }

  /**
   * Fill the segment of the buffer of [length] starting at [offset] with [bytes]. If the segment is longer than the
   * source then the [bytes] will be looped until no more fill is needed.
   */
  internal open fun fillWith(bytes: GuestBytes, offset: Int, length: Int, targetOffset: Int) {
    // repeat the source bytes until the length is reached
    val bytesSize = bytes.size
    for (i in 0 until length) setByte(i + offset, bytes[i % bytesSize])
  }

  /**
   * Fill the segment of the buffer of [length] starting at [offset] with [bytes]. If the segment is longer than the
   * source then the [bytes] will be looped until no more fill is needed.
   */
  internal open fun fillWith(bytes: NodeHostBuffer, offset: Int, length: Int) {
    // repeat the source bytes until the length is reached
    val bytesSize = bytes.length
    for (i in 0 until length) setByte(i + offset, bytes.byteBuffer[i % bytesSize])
  }

  override fun fill(value: PolyglotValue, offset: Int?, end: Int?, encoding: String?): BufferInstance {
    val start = offset ?: 0
    val length = if (end != null) end - start else this.length

    when {
      // numbers are coerced into Byte
      value.isNumber -> fillWith(value.asLong().toByte(), start, length)

      // strings are encoded (by default in UTF-8)
      value.isString -> fillWith(NodeBufferEncoding.encode(value.asString(), encoding), start, length)

      // otherwise it could be a Buffer or Uint8Array
      else -> value.whenBufferOrView(
        onBuffer = {
          when (it) {
            is NodeHostBuffer -> fillWith(it, start, length)
            is NodeGuestBuffer -> fillWith(GuestBytes(it.buffer), start, length, it.byteOffset)
          }
        },
        onView = { fillWith(it.bytes(), start, length, it.byteOffset()) },
        default = { throw JsError.typeError("Expected String, Buffer, Uit8Array, or Number as fill value") },
      )
    }

    return this
  }

  /* ------- IndexOf ------- */

  override fun indexOf(value: PolyglotValue, byteOffset: Int?, encoding: String?): Int {
    if (value.isNumber) {
      val byteValue = value.asLong().toByte()
      for (i in (byteOffset ?: 0) until length) if (getByte(i) == byteValue) return i
      return -1
    }

    if (value.isString) {
      val bytes = NodeBufferEncoding.encode(value.asString(), encoding)
      return search(byteOffset ?: 0, bytes.size) { bytes[it] }
    }

    return value.whenBufferOrView(
      onBuffer = { other ->
        search(byteOffset ?: 0, other.length) { other.getByte(it) }
      },
      onView = { view ->
        val viewOffset = view.byteOffset()
        val viewBytes = view.bytes()

        search(byteOffset ?: 0, viewBytes.size) { viewBytes[viewOffset + it] }
      },
    )
  }

  override fun lastIndexOf(value: PolyglotValue, byteOffset: Int?, encoding: String?): Int {
    if (value.isNumber) {
      val byteValue = value.asLong().toByte()
      for (i in (byteOffset ?: 0) until length) if (getByte(length - i) == byteValue) return i
      return -1
    }

    if (value.isString) {
      val bytes = NodeBufferEncoding.encode(value.asString(), encoding)
      return invertedSearch(byteOffset ?: (length - 1), bytes.size) { bytes[it] }
    }

    return value.whenBufferOrView(
      onBuffer = { other ->
        invertedSearch(byteOffset ?: (length - 1), other.length) { other.getByte(it) }
      },
      onView = { view ->
        val viewOffset = view.byteOffset()
        val viewBytes = view.bytes()

        invertedSearch(byteOffset ?: (length - 1), viewBytes.size) { viewBytes[viewOffset + it] }
      },
    )
  }

  /**
   * A greedy search with an abstract subject that must be read using the given [readSubject] function. Starting at
   * [startOffset], the bytes in the buffer are checked to see if the bytes in the subject are found next to each
   * other.
   */
  private inline fun search(startOffset: Int, subjectLength: Int, readSubject: (Int) -> Byte): Int {
    var current = startOffset

    while (current < length) {
      // not enough remaining bytes to find the subject
      if (length - current < subjectLength) return -1

      // attempt to match the entire subject
      for (i in 0 until subjectLength) {
        if (getByte(current + i) != readSubject(i)) break
        return current
      }

      // no match, try again with increased offset
      current++
    }

    return current
  }

  /**
   * A greedy search with an abstract subject that must be read using the given [readSubject] function. Starting at
   * [startOffset], the bytes in the buffer are checked to see if the bytes in the subject are found next to each
   * other.
   */
  private inline fun invertedSearch(startOffset: Int, subjectLength: Int, readSubject: (Int) -> Byte): Int {
    var current = startOffset

    while (current >= 0) {
      // not enough remaining bytes to find the subject
      if (current < subjectLength - 1) return -1

      // attempt to match the entire subject
      for (i in 0 until subjectLength) {
        if (getByte(current - i) != readSubject(subjectLength - i)) break
        return current
      }

      // no match, try again with increased offset
      current--
    }

    return current - subjectLength
  }

  /**
   * Read an integer of variable [byteLength] at the given [offset]. The [getByte] function must return the byte value
   * corresponding to the i-th byte of the integer; this allows both little-endian and big-endian implementations to
   * use the same helper.
   */
  protected inline fun readVarInt(offset: Int, byteLength: Int, getByte: (Int) -> Byte): Long {
    if (offset + byteLength >= length) throw JsError.rangeError("Out of range")
    if (byteLength <= 0 || byteLength > 6) throw JsError.rangeError("Out of range")

    var value = 0L
    for (i in 0 until byteLength) value = (value shl 8) + (getByte(i) and 0xFF.toByte())

    return value
  }

  /**
   * Read an integer of variable [byteLength] at the given [offset]. The [getByte] function must return the byte value
   * corresponding to the i-th byte of the integer; this allows both little-endian and big-endian implementations to
   * use the same helper.
   */
  protected inline fun writeVarInt(value: Long, offset: Int, byteLength: Int, setByte: (Int, Byte) -> Unit) {
    if (offset + byteLength >= length) throw JsError.rangeError("Out of range")
    if (byteLength <= 0 || byteLength > 6) throw JsError.rangeError("Out of range")

    var rem = value
    for (i in 0 until byteLength) {
      setByte(byteLength - i - 1, (rem and 0xFF).toByte())
      rem = rem shr 8
    }
  }

  /* -- Encoding -- */

  override fun toJSON(): PolyglotValue {
    val jsonProxy = JsProxy.build {
      put("type", "Buffer")

      val bytes = getBytes()
      val bytesProxy = object : ProxyArray {
        override fun getSize(): Long = bytes.size.toLong()
        override fun get(index: Long): Any = bytes[index.toInt()]
        override fun set(index: Long, value: Value?) {
          bytes[index.toInt()] = value?.takeIf { it.isNumber }?.asLong()?.toByte() ?: 0
        }
      }

      put("data", bytesProxy)
    }

    return PolyglotValue.asValue(jsonProxy)
  }

  override fun toString(encoding: String?, start: Int?, end: Int?): String {
    val bytes = getBytes(start ?: 0, end ?: length)
    return NodeBufferEncoding.decode(bytes, encoding)
  }

  override fun keys(): PolyglotValue {
    return PolyglotValue.asValue(ProxyIterable.from(0 until length))
  }

  override fun values(): PolyglotValue {
    return Iterable {
      iterator { for (i in 0 until length) yield(getByte(i)) }
    }.let {
      PolyglotValue.asValue(ProxyIterable.from(it))
    }
  }

  override fun entries(): PolyglotValue {
    return Iterable {
      // note that we need to yield two-element arrays here, which are effectively index-value pairs;
      // we use the static ProxyArray factory which treats all elements in the array as objects
      iterator { for (i in 0 until length) yield(ProxyArray.fromArray(i, getByte(i))) }
    }.let {
      PolyglotValue.asValue(ProxyIterable.from(it))
    }
  }

  override fun getMemberKeys(): Any {
    return instanceMembers
  }

  override fun hasMember(key: String?): Boolean {
    if (key == null) return false
    return instanceMembers.binarySearch(key) >= 0
  }

  override fun getMember(key: String?): Any = when (key) {
    "buffer" -> this.buffer
    "byteOffset" -> this.byteOffset
    "compare" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> compare(args[0])
        2 -> compare(args[0], args[1].asInt())
        3 -> compare(args[0], args[1].asInt(), args[2].asInt())
        4 -> compare(args[0], args[1].asInt(), args[2].asInt(), args[3].asInt())
        5 -> compare(args[0], args[1].asInt(), args[2].asInt(), args[3].asInt(), args[4].asInt())
        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "copy" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> copy(args[0])
        2 -> copy(args[0], args[1].asInt())
        3 -> copy(args[0], args[1].asInt(), args[2].asInt())
        4 -> copy(args[0], args[1].asInt(), args[2].asInt(), args[3].asInt())
        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "entries" -> ProxyExecutable { entries() }
    "equals" -> ProxyExecutable { args ->
      val other = args.singleOrNull() ?: error("Expected a single argument, received ${args.size}")
      compare(other) == 0
    }

    "fill" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> fill(args[0])
        2 -> {
          val offsetOrEncoding = args[1]
          fill(
            value = args[0],
            offset = offsetOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = offsetOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        3 -> {
          val endOrEncoding = args[2]
          fill(
            value = args[0],
            offset = args[1].asInt(),
            end = endOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = endOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        4 -> fill(
          value = args[0],
          offset = args[1].asInt(),
          end = args[2].asInt(),
          encoding = args[3].asString(),
        )

        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "includes" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> indexOf(args[0]) >= 0
        2 -> {
          val offsetOrEncoding = args[1]
          indexOf(
            value = args[0],
            byteOffset = offsetOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = offsetOrEncoding.takeIf { it.isString }?.asString(),
          ) >= 0
        }

        3 -> indexOf(args[0], args[1].asInt(), args[2].asString()) >= 0
        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "indexOf" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> indexOf(args[0])
        2 -> {
          val offsetOrEncoding = args[1]
          indexOf(
            value = args[0],
            byteOffset = offsetOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = offsetOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        3 -> indexOf(args[0], args[1].asInt(), args[2].asString())
        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "keys" -> ProxyExecutable { keys() }

    "lastIndexOf" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> lastIndexOf(args[0])
        2 -> {
          val offsetOrEncoding = args[1]
          lastIndexOf(
            value = args[0],
            byteOffset = offsetOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = offsetOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        3 -> lastIndexOf(args[0], args[1].asInt(), args[2].asString())
        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "length" -> length

    "readBigInt64BE" -> ProxyExecutable { args -> readBigInt64BE(args.firstOrNull()?.asInt()) }
    "readBigInt64LE" -> ProxyExecutable { args -> readBigInt64LE(args.firstOrNull()?.asInt()) }
    "readBigUInt64BE" -> ProxyExecutable { args -> readBigUInt64BE(args.firstOrNull()?.asInt()) }
    "readBigUInt64LE" -> ProxyExecutable { args -> readBigUInt64LE(args.firstOrNull()?.asInt()) }

    "readDoubleBE" -> ProxyExecutable { args -> readDoubleBE(args.firstOrNull()?.asInt()) }
    "readDoubleLE" -> ProxyExecutable { args -> readDoubleLE(args.firstOrNull()?.asInt()) }

    "readFloatBE" -> ProxyExecutable { args -> readFloatBE(args.firstOrNull()?.asInt()) }
    "readFloatLE" -> ProxyExecutable { args -> readFloatLE(args.firstOrNull()?.asInt()) }

    "readInt8" -> ProxyExecutable { args -> readInt8(args.firstOrNull()?.asInt()) }

    "readInt16BE" -> ProxyExecutable { args -> readInt16BE(args.firstOrNull()?.asInt()) }
    "readInt16LE" -> ProxyExecutable { args -> readInt16LE(args.firstOrNull()?.asInt()) }

    "readInt32BE" -> ProxyExecutable { args -> readInt32BE(args.firstOrNull()?.asInt()) }
    "readInt32LE" -> ProxyExecutable { args -> readInt32LE(args.firstOrNull()?.asInt()) }

    "readIntBE" -> ProxyExecutable { args -> readIntBE(args[0].asInt(), args[1].asInt()) }
    "readIntLE" -> ProxyExecutable { args -> readIntLE(args[0].asInt(), args[1].asInt()) }

    "readUInt8" -> ProxyExecutable { args -> readUInt8(args.firstOrNull()?.asInt()) }

    "readUInt16BE" -> ProxyExecutable { args -> readUInt16BE(args.firstOrNull()?.asInt()) }
    "readUInt16LE" -> ProxyExecutable { args -> readUInt16LE(args.firstOrNull()?.asInt()) }

    "readUInt32BE" -> ProxyExecutable { args -> readUInt32BE(args.firstOrNull()?.asInt()) }
    "readUInt32LE" -> ProxyExecutable { args -> readUInt32LE(args.firstOrNull()?.asInt()) }

    "readUIntBE" -> ProxyExecutable { args -> readUIntBE(args[0].asInt(), args[1].asInt()) }
    "readUIntLE" -> ProxyExecutable { args -> readUIntLE(args[0].asInt(), args[1].asInt()) }

    "subarray" -> ProxyExecutable { args ->
      subarray(args.getOrNull(0)?.asInt(), args.getOrNull(1)?.asInt())
    }

    "swap16" -> ProxyExecutable { swap16() }
    "swap32" -> ProxyExecutable { swap32() }
    "swap64" -> ProxyExecutable { swap64() }

    "toJSON" -> ProxyExecutable { toJSON() }
    "toString" -> ProxyExecutable { args ->
      toString(
        encoding = args.getOrNull(0)?.asString(),
        start = args.getOrNull(1)?.asInt(),
        end = args.getOrNull(2)?.asInt(),
      )
    }

    "values" -> ProxyExecutable { values() }
    "write" -> ProxyExecutable { args ->
      when (args.size) {
        1 -> lastIndexOf(args[0])
        2 -> {
          val offsetOrEncoding = args[1]
          write(
            string = args[0].asString(),
            offset = offsetOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = offsetOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        3 -> {
          val lengthOrEncoding = args[2]
          write(
            string = args[0].asString(),
            offset = args[1].asInt(),
            length = lengthOrEncoding.takeIf { it.isNumber }?.asInt(),
            encoding = lengthOrEncoding.takeIf { it.isString }?.asString(),
          )
        }

        4 -> {
          write(
            string = args[0].asString(),
            offset = args[1].asInt(),
            length = args[2].asInt(),
            encoding = args[3].asString(),
          )
        }

        else -> error("Invalid argument count: ${args.size}")
      }
    }

    "writeBigInt64BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeBigInt64BE(args[0].asLong(), args.getOrNull(1)?.asInt())
    }

    "writeBigInt64LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeBigInt64LE(args[0].asLong(), args.getOrNull(1)?.asInt())
    }

    "writeBigUInt64BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeBigUInt64BE(args[0].asLong(), args.getOrNull(1)?.asInt())
    }

    "writeBigUInt64LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeBigUInt64LE(args[0].asLong(), args.getOrNull(1)?.asInt())
    }

    "writeDoubleBE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeDoubleBE(args[0].asDouble(), args.getOrNull(1)?.asInt())
    }

    "writeDoubleLE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeDoubleLE(args[0].asDouble(), args.getOrNull(1)?.asInt())
    }

    "writeFloatBE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeFloatBE(args[0].asFloat(), args.getOrNull(1)?.asInt())
    }

    "writeFloatLE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeFloatLE(args[0].asFloat(), args.getOrNull(1)?.asInt())
    }

    "writeInt8" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeInt8(args[0].asByte(), args.getOrNull(1)?.asInt())
    }

    "writeInt16BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeInt16BE(args[0].asShort(), args.getOrNull(1)?.asInt())
    }

    "writeInt16LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeInt16LE(args[0].asShort(), args.getOrNull(1)?.asInt())
    }

    "writeInt32BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeInt32BE(args[0].asInt(), args.getOrNull(1)?.asInt())
    }

    "writeInt32LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeInt32LE(args[0].asInt(), args.getOrNull(1)?.asInt())
    }

    "writeIntBE" -> ProxyExecutable { args ->
      check(args.size == 3) { "Expected 3 arguments" }
      writeIntBE(args[0].asLong(), args[1].asInt(), args[2].asInt())
    }

    "writeIntLE" -> ProxyExecutable { args ->
      check(args.size == 3) { "Expected 3 arguments" }
      writeIntLE(args[0].asLong(), args[1].asInt(), args[2].asInt())
    }

    "writeUInt8" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeUInt8(args[0].asByte(), args.getOrNull(1)?.asInt())
    }

    "writeUInt16BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeUInt16BE(args[0].asShort(), args.getOrNull(1)?.asInt())
    }

    "writeUInt16LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeUInt16LE(args[0].asShort(), args.getOrNull(1)?.asInt())
    }

    "writeUInt32BE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeUInt32BE(args[0].asInt(), args.getOrNull(1)?.asInt())
    }

    "writeUInt32LE" -> ProxyExecutable { args ->
      check(args.isNotEmpty()) { "Expected at least one argument" }
      writeUInt32LE(args[0].asInt(), args.getOrNull(1)?.asInt())
    }

    "writeUIntBE" -> ProxyExecutable { args ->
      check(args.size == 3) { "Expected 3 arguments" }
      writeUIntBE(args[0].asLong(), args[1].asInt(), args[2].asInt())
    }

    "writeUIntLE" -> ProxyExecutable { args ->
      check(args.size == 3) { "Expected 3 arguments" }
      writeUIntLE(args[0].asLong(), args[1].asInt(), args[2].asInt())
    }

    null -> error("Invalid member key (null)")
    else -> error("Unknown member: $key")
  }

  override fun putMember(key: String?, value: Value?) {
    error("Modifying Buffer instances is not allowed")
  }

  override fun removeMember(key: String?): Boolean {
    error("Modifying Buffer instances is not allowed")
  }

  private companion object {
    private val instanceMembers = arrayOf(
      "buffer",
      "byteOffset",
      "compare",
      "copy",
      "entries",
      "equals",
      "fill",
      "includes",
      "indexOf",
      "keys",
      "lastIndexOf",
      "length",
      "readBigInt64BE",
      "readBigInt64LE",
      "readBigUInt64BE",
      "readBigUInt64LE",
      "readDoubleBE",
      "readDoubleLE",
      "readFloatBE",
      "readFloatLE",
      "readInt8",
      "readInt16BE",
      "readInt16LE",
      "readInt32BE",
      "readInt32LE",
      "readIntBE",
      "readIntLE",
      "readUInt8",
      "readUInt16BE",
      "readUInt16LE",
      "readUInt32BE",
      "readUInt32LE",
      "readUIntBE",
      "readUIntLE",
      "subarray",
      "swap16",
      "swap32",
      "swap64",
      "toJSON",
      "toString",
      "values",
      "write",
      "writeBigInt64BE",
      "writeBigInt64LE",
      "writeBigUInt64BE",
      "writeBigUInt64LE",
      "writeDoubleBE",
      "writeDoubleLE",
      "writeFloatBE",
      "writeFloatLE",
      "writeInt8",
      "writeInt16BE",
      "writeInt16LE",
      "writeInt32BE",
      "writeInt32LE",
      "writeIntBE",
      "writeIntLE",
      "writeUInt8",
      "writeUInt16BE",
      "writeUInt16LE",
      "writeUInt32BE",
      "writeUInt32LE",
      "writeUIntBE",
      "writeUIntLE",
    ).also { it.sort() }
  }
}
