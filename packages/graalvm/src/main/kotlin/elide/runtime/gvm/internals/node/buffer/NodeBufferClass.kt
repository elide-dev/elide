package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.Value
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.intrinsics.js.JsError.typeError
import elide.runtime.intrinsics.js.node.buffer.BufferClass
import elide.runtime.intrinsics.js.node.buffer.BufferInstance

@DelicateElideApi internal class NodeBufferClass : BufferClass {
  override var poolSize: Int = 8192

  private fun PolyglotValue.asBufferOrNull(): BufferInstance? {
    if (!isProxyObject) return null
    return runCatching { asProxyObject<NodeBufferInstance>() }.getOrNull()
  }

  override fun alloc(size: Int, fill: PolyglotValue?, encoding: String?): BufferInstance {
    val buffer = NodeHostBuffer.allocate(size)
    fill?.let { buffer.fill(it, encoding = encoding) }

    return buffer
  }

  override fun allocUnsafe(size: Int): BufferInstance {
    return NodeHostBuffer.allocate(size)
  }

  override fun allocUnsafeSlow(size: Int): BufferInstance {
    return NodeHostBuffer.allocate(size)
  }

  override fun byteLength(string: String, encoding: String?): Int {
    return NodeBufferEncoding.byteLength(string, encoding)
  }

  override fun compare(buf1: PolyglotValue, buf2: PolyglotValue): Int {
    // the first value is a buffer, delegate the comparison
    buf1.asBufferOrNull()?.let { return it.compare(buf2) }

    // the first value is an array and the second one is a buffer, delegate
    // the comparison but flip the result to obtain the correct value
    buf2.asBufferOrNull()?.let { return -(it.compare(buf1)) }

    // both values must be arrays, compare byte by byte
    val (bytes1, offset1, size1) = GuestBufferView.tryFrom(buf1)
      ?: throw typeError("Expected operand to be a Buffer or UInt8Array")

    val (bytes2, offset2, size2) = GuestBufferView.tryFrom(buf2)
      ?: throw typeError("Expected operand to be a Buffer or UInt8Array")

    var comp = 0
    for (i in 0 until minOf(size1, size2)) comp = bytes1[offset1 + i].compareTo(bytes2[offset2 + i])
    comp = if (comp != 0) comp else size1.compareTo(size2)

    return comp.coerceIn(-1, 1)
  }

  override fun concat(list: PolyglotValue, totalLength: Int?): BufferInstance {
    if (!list.hasArrayElements()) throw typeError("Expected an array of sources to concatenate")
    if (totalLength == 0) return NodeHostBuffer.allocate(0)

    // compute actual combined length
    var combinedSourceLength = 0
    for (i in 0 until list.arraySize) {
      val element = list.getArrayElement(i)

      combinedSourceLength += GuestBufferView.tryFrom(element)?.byteSize()
        ?: element.asBufferOrNull()?.length
                ?: throw typeError("Expected source elements to be Buffer or UInt8Array")
    }

    val bufferSize = totalLength?.coerceAtMost(combinedSourceLength) ?: combinedSourceLength
    val buffer = NodeHostBuffer.allocate(bufferSize)

    // fill the buffer
    var filled = 0
    for (i in 0 until list.arraySize) {
      val element = list.getArrayElement(i)
      val elementSize = GuestBufferView.tryFrom(element)?.byteSize()
        ?: element.asBufferOrNull()?.length
        ?: throw typeError("Expected source elements to be Buffer or UInt8Array")


      buffer.fill(element, filled, (filled + elementSize).coerceAtMost(bufferSize - filled))
      filled += elementSize
    }

    return buffer
  }

  override fun copyBytesFrom(view: PolyglotValue, offset: Int?, length: Int?): BufferInstance {
    val (bytes, sourceOffset, sourceLength) = GuestBufferView.tryFrom(view)
      ?: throw typeError("Expected source to be a TypedArray")

    val maxLength = sourceLength - (offset ?: 0)
    val bufferSize = length?.coerceAtMost(maxLength) ?: maxLength

    val buffer = NodeHostBuffer.allocate(bufferSize)
    buffer.fillWith(bytes, 0, bufferSize, offset ?: 0)

    return buffer
  }

  override fun from(source: PolyglotValue, offset: Int?, length: Int?, encoding: String?): BufferInstance {
    if (source.isString) {
      val bytes = NodeBufferEncoding.encode(source.asString(), encoding)
      return NodeHostBuffer.allocate(bytes.size).apply { fillWith(bytes, 0, bytes.size) }
    }

    source.asBufferOrNull()?.let { sourceBuffer ->
      val buffer = NodeHostBuffer.allocate(sourceBuffer.length)

      if (sourceBuffer is NodeHostBuffer) buffer.fillWith(buffer, 0, buffer.length)
      else buffer.fillWith(GuestBytes(sourceBuffer.buffer), 0, buffer.length, sourceBuffer.byteOffset)

      return buffer
    }

    GuestBufferView.tryFrom(source)?.let { (bytes, sourceOffset, sourceLength) ->
      val buffer = NodeHostBuffer.allocate(sourceLength)
      buffer.fillWith(bytes, 0, sourceLength, sourceOffset)

      return buffer
    }

    if (source.hasBufferElements()) {
      val buffer = NodeHostBuffer.allocate(source.bufferSize.toInt())
      buffer.fillWith(GuestBytes(source), 0, buffer.length, offset ?: 0)

      return buffer
    }

    if (source.hasArrayElements()) {
      val buffer = NodeHostBuffer.allocate(source.arraySize.toInt())
      for (i in 0 until buffer.length) buffer.byteBuffer.put(i, source.getArrayElement(i.toLong()).asLong().toByte())

      return buffer
    }

    throw typeError("Unexpected source type: expected string, int[], Buffer, UInt8Array, or ArrayBuffer")
  }

  override fun isBuffer(obj: PolyglotValue): Boolean {
    return obj.asBufferOrNull() != null
  }

  override fun isEncoding(encoding: String): Boolean {
    return NodeBufferEncoding.isSupported(encoding)
  }

  override fun newInstance(vararg arguments: Value?): Any {
    error("Buffer constructors are deprecated, use the static factory methods instead")
  }
}
