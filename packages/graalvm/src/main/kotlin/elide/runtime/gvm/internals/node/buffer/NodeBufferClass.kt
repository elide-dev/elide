@file:Suppress("MagicNumber")

package elide.runtime.gvm.internals.node.buffer

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
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

  override fun byteLength(string: PolyglotValue, encoding: String?): Int {
    // in a twisted joke about API design, the argument *explicitly* named 'string', in the method
    // used *to calculate the byte size for encoded text*, can also be a non-string type; brilliant
    if (string.isString) return NodeBufferEncoding.byteLength(string.asString(), encoding)

    // accepted non-string types simply return their own byte length (buffers and typed arrays)
    if (string.hasMembers()) string.getMember("byteLength")?.asInt()?.let { return it }

    // unsupported type
    throw typeError("Unknown type for argument 'string', expected string, Buffer, ArrayBuffer, or TypedArray")
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
    for (i in 0 until minOf(size1, size2)) {
      comp = bytes1[offset1 + i].compareTo(bytes2[offset2 + i])
      if (comp != 0) break
    }

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

      combinedSourceLength += element.asBufferOrNull()?.length
        ?: GuestBufferView.tryFrom(element)?.byteSize()
        ?: throw typeError("Expected source elements to be Buffer or UInt8Array")
    }

    val bufferSize = totalLength?.coerceAtMost(combinedSourceLength) ?: combinedSourceLength
    val buffer = NodeHostBuffer.allocate(bufferSize)

    // fill the buffer
    var filled = 0
    for (i in 0 until list.arraySize) {
      val element = list.getArrayElement(i)
      val elementSize = element.asBufferOrNull()?.length
        ?: GuestBufferView.tryFrom(element)?.byteSize()
        ?: throw typeError("Expected source elements to be Buffer or UInt8Array")

      buffer.fill(element, filled, filled + elementSize.coerceAtMost(bufferSize - filled))
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
    buffer.fillWith(bytes, 0, bufferSize, sourceOffset + (offset ?: 0))

    return buffer
  }

  override fun from(source: PolyglotValue, offset: Int?, length: Int?, encoding: String?): BufferInstance {
    source.asBufferOrNull()?.let { sourceBuffer ->
      val buffer = NodeHostBuffer.allocate(sourceBuffer.length)

      if (sourceBuffer is NodeHostBuffer) buffer.fillWith(sourceBuffer, 0, buffer.length)
      else buffer.fillWith(GuestBytes(sourceBuffer.buffer), 0, buffer.length, sourceBuffer.byteOffset)

      return buffer
    }

    GuestBufferView.tryFrom(source)?.let { (bytes, sourceOffset, sourceLength) ->
      val buffer = NodeHostBuffer.allocate(length ?: (sourceLength - (offset ?: 0)))
      buffer.fillWith(bytes, 0, buffer.length, sourceOffset + (offset ?: 0))

      return buffer
    }

    return when {
      source.isString -> {
        val bytes = NodeBufferEncoding.encode(source.asString(), encoding)
        NodeHostBuffer.allocate(bytes.size).apply { fillWith(bytes, 0, bytes.size) }
      }

      source.hasBufferElements() -> {
        val buffer = NodeHostBuffer.allocate(length ?: (source.bufferSize.toInt() - (offset ?: 0)))
        buffer.fillWith(GuestBytes(source), 0, buffer.length, offset ?: 0)
        buffer
      }

      source.hasArrayElements() -> {
        val buffer = NodeHostBuffer.allocate(source.arraySize.toInt())
        for (i in 0 until buffer.length) buffer.byteBuffer.put(i, source.getArrayElement(i.toLong()).asLong().toByte())
        buffer
      }

      else -> throw typeError("Unexpected source type: expected string, int[], Buffer, UInt8Array, or ArrayBuffer")
    }
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

  override fun getMemberKeys(): Any {
    return staticMembers
  }

  override fun hasMember(key: String?): Boolean {
    if (key == null) return false
    return staticMembers.binarySearch(key) >= 0
  }

  @Suppress("CyclomaticComplexMethod")
  override fun getMember(key: String?): Any = when (key) {
    "alloc" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.alloc takes 1 to 3 arguments, but received none")
      alloc(size = args[0].asInt(), fill = args.getOrNull(1), encoding = args.getOrNull(2)?.asString())
    }

    "allocUnsafe" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.allocUnsafe takes 1 argument, but received none")
      allocUnsafe(args[0].asInt())
    }

    "allocUnsafeSlow" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.allocUnsafeSlow takes 1 argument, but received none")
      allocUnsafe(args[0].asInt())
    }

    "byteLength" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.byteLength takes 1 or 2 arguments, but received none")
      byteLength(string = args[0], encoding = args.getOrNull(1)?.asString())
    }

    "compare" -> ProxyExecutable { args ->
      if (args.size < 2) error("Buffer.compare takes 2 arguments, but received ${args.size}")
      compare(args[0], args[1])
    }

    "concat" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.byteLength takes 1 or 2 arguments, but received none")
      concat(list = args[0], totalLength = args.getOrNull(1)?.asInt())
    }

    "copyBytesFrom" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.copyBytesFrom takes 1 to 3 arguments, but received none")
      copyBytesFrom(view = args[0], offset = args.getOrNull(1)?.asInt(), length = args.getOrNull(2)?.asInt())
    }

    "from" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.byteLength takes 1 to 3 arguments, but received none")

      // Buffer.from has 4 different overloads with very different semantics, but at most they
      // have 3 arguments (source + offset + length); in two of those overloads the second argument
      // can be an encoding (a string value), and one of them also allows it to be an offset instead,
      // so we disambiguate based on the type manually
      val offsetOrEncoding = args.getOrNull(1)

      from(
        source = args[0],
        offset = offsetOrEncoding?.takeIf { it.isNumber }?.asInt(),
        encoding = offsetOrEncoding?.takeIf { it.isString }?.asString(),
        length = args.getOrNull(2)?.asInt(),
      )
    }

    "isBuffer" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.copyBytesFrom takes 1 argument, but received none")
      isBuffer(args[0])
    }

    "isEncoding" -> ProxyExecutable { args ->
      if (args.isEmpty()) error("Buffer.isEncoding takes 1 argument, but received none")
      isEncoding(args[0].asString())
    }

    "poolSize" -> poolSize

    null -> error("Cannot retrieve member with null key")
    else -> error("Unknown member 'Buffer.$key'")
  }

  override fun putMember(key: String?, value: Value?) {
    error("Modifying the Buffer prototype is not allowed")
  }

  override fun removeMember(key: String?): Boolean {
    error("Modifying the Buffer prototype is not allowed")
  }

  private companion object {
    private val staticMembers = arrayOf(
      "alloc",
      "allocUnsafe",
      "allocUnsafeSlow",
      "byteLength",
      "compare",
      "concat",
      "copyBytesFrom",
      "from",
      "isBuffer",
      "isEncoding",
      "poolSize",
    ).also { it.sort() }
  }
}
