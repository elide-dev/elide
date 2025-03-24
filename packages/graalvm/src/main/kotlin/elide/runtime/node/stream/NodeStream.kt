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
package elide.runtime.node.stream

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.io.IOException
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.api.Intrinsic
import elide.runtime.gvm.internals.intrinsics.js.AbstractNodeBuiltinModule
import elide.runtime.gvm.js.JsError
import elide.runtime.gvm.js.JsSymbol.JsSymbols.asJsSymbol
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.GuestIntrinsic.MutableIntrinsicBindings
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.node.StreamAPI
import elide.runtime.intrinsics.js.node.buffer.BufferInstance
import elide.runtime.intrinsics.js.node.stream.*
import elide.runtime.lang.javascript.NodeModuleName
import elide.runtime.node.events.EventAware
import elide.runtime.node.events.StandardEventName

// Internal symbol where the Node built-in module is installed.
private const val STREAM_MODULE_SYMBOL = "node_${NodeModuleName.STREAM}"

// Installs the Node stream module into the intrinsic bindings.
@Intrinsic internal class NodeStreamModule : AbstractNodeBuiltinModule() {
  private val singleton by lazy { NodeStream.create() }

  override fun install(bindings: MutableIntrinsicBindings) {
    bindings[STREAM_MODULE_SYMBOL.asJsSymbol()] = ProxyExecutable { singleton }
    // @TODO: Cannot register natively because some stream types are polyfilled
    // ModuleRegistry.deferred(ModuleInfo.of(NodeModuleName.STREAM)) { singleton }
  }
}

/**
 * End-of-stream internal signal.
 */
internal class EndStreamException : Exception("End of Stream", null, false, false)

private object NodeEncodingNames {
  const val UTF8 = "utf8"
  const val UTF16 = "utf16"
  const val UTF16BE = "utf16be"
  const val UTF16LE = "utf16le"
  const val LATIN1 = "latin1"
  const val ASCII = "ascii"
}

internal object StreamEventName {
  const val CLOSE = "close"
  const val DATA = "data"
  const val END = "end"
  const val ERROR = "error"
  const val PAUSE = "pause"
  const val RESUME = "resume"
  const val DRAIN = "drain"
  const val FINISH = "finish"
  const val PIPE = "pipe"
  const val UNPIPE = "unpipe"
}

private fun encodingFromGuestValue(value: Value?): Charset? = when {
  value == null || value.isNull -> null
  value.isString -> stringToEncoding(value.asString())
  else -> throw IllegalArgumentException("Unsupported encoding value: $value")
}

private fun stringToEncoding(encoding: String): Charset {
  return when (encoding) {
    NodeEncodingNames.UTF8 -> StandardCharsets.UTF_8
    NodeEncodingNames.UTF16 -> StandardCharsets.UTF_16
    NodeEncodingNames.UTF16BE -> StandardCharsets.UTF_16BE
    NodeEncodingNames.UTF16LE -> StandardCharsets.UTF_16LE
    NodeEncodingNames.LATIN1 -> StandardCharsets.ISO_8859_1
    NodeEncodingNames.ASCII -> StandardCharsets.US_ASCII
    else -> throw IllegalArgumentException("Unsupported encoding: $encoding")
  }
}

private fun Charset.toNodeEncodingName(): String = when (this) {
  StandardCharsets.UTF_8 -> NodeEncodingNames.UTF8
  StandardCharsets.UTF_16 -> NodeEncodingNames.UTF16
  StandardCharsets.UTF_16BE -> NodeEncodingNames.UTF16BE
  StandardCharsets.UTF_16LE -> NodeEncodingNames.UTF16LE
  StandardCharsets.ISO_8859_1 -> NodeEncodingNames.LATIN1
  StandardCharsets.US_ASCII -> NodeEncodingNames.ASCII
  else -> throw IllegalArgumentException("Unsupported charset: $this")
}

// Basic stream state shared across readable/writable streams.
internal abstract class AbstractStream<T>(
  initialEncoding: Charset? = null,
  protected val events: EventAware = EventAware.create(),
) : EventAware by events, StatefulStream where T : AbstractStream<T> {
  protected val open: AtomicBoolean = AtomicBoolean(true)
  protected val isDestroyed: AtomicBoolean = AtomicBoolean(false)
  protected val didError: AtomicBoolean = AtomicBoolean(false)
  protected val didAbort: AtomicBoolean = AtomicBoolean(false)
  protected val didEnd: AtomicBoolean = AtomicBoolean(false)
  protected val objectModeActive: AtomicBoolean = AtomicBoolean(false)
  protected val encoding: AtomicReference<Charset> = AtomicReference(initialEncoding)
  protected val streamErr: AtomicReference<Any> = AtomicReference(null)
  protected val highWaterMark: AtomicInteger = AtomicInteger(0)
  protected val bufferSize: AtomicInteger = AtomicInteger(0)
  private val linkedQueue: ConcurrentLinkedQueue<Any?> = ConcurrentLinkedQueue()

  override val closed: Boolean get() = !open.get()
  override val destroyed: Boolean get() = isDestroyed.get()

  init {
    events.addEventListener(StreamEventName.CLOSE) {
      open.set(false)
      interrupt()
    }
    events.addEventListener(StreamEventName.END) {
      didEnd.set(true)
      interrupt()
    }
    events.addEventListener(StreamEventName.ERROR) {
      didError.set(true)
      didAbort.set(!didEnd.get())
      interrupt()
    }
  }

  @Suppress("UNCHECKED_CAST")
  protected inline fun <reified R> withActive(block: T.() -> R): R {
    require(open.get()) { "Stream is closed" }
    require(!isDestroyed.get()) { "Stream is destroyed" }
    return block(this as T)
  }

  override fun destroy() {
    open.set(false)
    isDestroyed.set(true)
    interrupt()
    close()
  }

  override fun destroy(error: Value) {
    streamErr.set(error)
    didError.set(true)
    destroy()
  }

  override fun destroy(error: Throwable) {
    streamErr.set(error)
    didError.set(true)
    destroy()
  }

  protected open fun interrupt() {
    // not implemented
  }

  protected open fun notifyData() {
    // not implemented
  }

  override fun close() {
    bufferSize.set(0)
    highWaterMark.set(0)
    linkedQueue.clear()
    events.close()
    emit(StreamEventName.CLOSE)
  }

  /**
   * Implementation entrypoint to retrieve the current buffer size.
   *
   * @return The current buffer size.
   */
  internal abstract fun size(): Int
}

private val readableEventNames = listOf(
  StreamEventName.CLOSE,
  StreamEventName.DATA,
  StreamEventName.END,
  StreamEventName.ERROR,
  StreamEventName.PAUSE,
  StreamEventName.RESUME,
)

internal abstract class AbstractReadable<T>(charset: Charset?) : AbstractStream<T>(charset), Readable
        where T : AbstractReadable<T> {
  private val didRead: AtomicBoolean = AtomicBoolean(false)
  private val flowing: AtomicReference<Boolean> = AtomicReference(null)
  private val isPaused: AtomicBoolean = AtomicBoolean(false)
  private val pipes: ConcurrentLinkedQueue<Pair<Writable, ReadablePipeOptions?>> = ConcurrentLinkedQueue()
  private val hasConsumer: AtomicBoolean = AtomicBoolean(false)

  init {
    // if a `data` listener is added and there is no current data consumer, mark consumption as having begun and start
    // the stream flowing.
    events.addEventListener(StandardEventName.NEW_LISTENER) {
      val event = it.getOrNull(0) as? String ?: return@addEventListener
      if (event == StreamEventName.DATA && !hasConsumer.get()) {
        hasConsumer.set(true)
        notifyData()
      }
    }
  }

  // Perform a recursive pump cycle of bytes from the backing stream into any piped destinations; delivery of `data`
  // events occurs here before piped streams are dispatched.
  private fun pump(untilHaltedOrExhausted: Boolean = true) {
    var seenRead = didRead.get()
    while (true) {
      when {
        isPaused.get() || didEnd.get() || isDestroyed.get() -> return

        else -> try {
          readData().let { chunk ->
            if (!seenRead) {
              seenRead = true
              didRead.set(true)
            }
            emit(StreamEventName.DATA, chunk)
            pipes.forEach { (destination, _) ->
              try {
                destination.write(chunk)
              } catch (e: IOException) {
                emit(StreamEventName.ERROR, e)
              }
            }
          }
        } catch (_: EndStreamException) {
          didEnd.set(true)
          emit(StreamEventName.END)
          return
        }
      }
      if (!untilHaltedOrExhausted) {
        return  // if we are asked to perform once cycle, bail early
      }
    }
  }

  override fun notifyData() {
    when {
      // if the readable is not currently flowing, and a consumer of some kind has been added, we need to move into the
      // `flowing` state and start delivering `data` events.
      readableFlowing != true && hasConsumer.get() -> {
        flowing.set(true)
        pump()
      }
    }
  }

  override val readable: Boolean get() = !isDestroyed.get() && !didError.get() && !didEnd.get()

  override val readableAborted: Boolean get() = didAbort.get()
  override val readableDidRead: Boolean get() = didRead.get()
  override val readableEncoding: String? get() = encoding.get()?.toNodeEncodingName()
  override val readableEnded: Boolean get() = didEnd.get()
  override val errored: Any? get() = if (didError.get()) streamErr.get() else null
  override val readableFlowing: Boolean? get() = flowing.get()
  override val readableHighWaterMark: Int get() = highWaterMark.get()
  override val readableLength: Int get() = bufferSize.get()
  override val readableObjectMode: Boolean get() = objectModeActive.get()
  override fun eventNames(): List<String> = readableEventNames
  override fun isPaused(): Boolean = isPaused.get()

  override fun pause(): Readable = apply {
    isPaused.set(true).also {
      interrupt()
    }
  }

  override fun pipe(destination: Writable) {
    pipes.add(destination to null)
    notifyData()
  }

  override fun pipe(destination: Writable, options: Value) {
    pipes.add(destination to ReadablePipeOptions.fromGuest(options))
    notifyData()
  }

  override fun pipe(destination: Writable, options: ReadablePipeOptions) {
    pipes.add(destination to options)
    notifyData()
  }

  override fun read(): StringOrBufferOrAny = readData()
  override fun read(size: Value): StringOrBufferOrAny = readData(size.asInt())
  override fun read(size: Int): StringOrBufferOrAny = readData(size)

  override fun resume(): Readable {
    TODO("Not yet implemented")
  }

  override fun setEncoding(encoding: String) {
    this.encoding.set(stringToEncoding(encoding))
  }

  override fun unpipe() {
    if (pipes.isNotEmpty()) {
      pipes.poll()  // discard
      interrupt()
    }
  }

  override fun unpipe(destination: Writable) {
    pipes.dropWhile {
      it.first === destination
    }.also {
      interrupt()
    }
  }

  override fun unshift(chunk: Value) {
    TODO("Not yet implemented")
  }

  override fun unshift(chunk: StreamChunk) {
    TODO("Not yet implemented")
  }

  override fun unshift(chunk: StreamChunk, encoding: String) {
    TODO("Not yet implemented")
  }

  override fun wrap(stream: Stream) {
    TODO("Not yet implemented")
  }

  override fun compose(stream: Stream): Duplex {
    TODO("Not yet implemented")
  }

  override fun compose(stream: Stream, options: Value): Duplex {
    TODO("Not yet implemented")
  }

  override fun compose(stream: Stream, options: ReadableComposeOptions): Duplex {
    TODO("Not yet implemented")
  }

  override fun iterator() {
    TODO("Not yet implemented")
  }

  override fun iterator(options: Value) {
    TODO("Not yet implemented")
  }

  override fun iterator(options: ReadableIteratorOptions) {
    TODO("Not yet implemented")
  }

  override fun map(cbk: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun map(cbk: Value, options: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun map(cbk: (Value) -> Value): Readable {
    TODO("Not yet implemented")
  }

  override fun map(options: ReadableMapOptions, cbk: (Any) -> Value): Readable {
    TODO("Not yet implemented")
  }

  override fun filter(cbk: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun filter(cbk: Value, options: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun filter(cbk: (Value) -> Boolean): Readable {
    TODO("Not yet implemented")
  }

  override fun filter(options: ReadableMapOptions, cbk: (Value) -> Boolean): Readable {
    TODO("Not yet implemented")
  }

  override fun forEach(cbk: Value): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  override fun forEach(cbk: Value, options: Value): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  override fun forEach(cbk: (Value) -> Unit): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  override fun forEach(options: ReadableForEachOptions, cbk: (Value) -> Unit): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  override fun toArray(): JsPromise<Array<Value>> {
    TODO("Not yet implemented")
  }

  override fun toArray(options: Value): JsPromise<Array<Value>> {
    TODO("Not yet implemented")
  }

  override fun toArray(options: ReadableToArrayOptions): JsPromise<Array<Value>> {
    TODO("Not yet implemented")
  }

  override fun some(cbk: Value): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun some(cbk: Value, options: Value): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun some(cbk: () -> Boolean?): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun some(options: ReadableSomeOptions, cbk: () -> Boolean?): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun find(cbk: Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun find(cbk: Value, options: Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun find(cbk: () -> Boolean?): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun find(options: ReadableFindOptions, cbk: () -> Boolean?): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun every(cbk: Value): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun every(cbk: Value, options: Value): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun every(cbk: () -> Boolean?): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun every(options: ReadableEveryOptions, cbk: () -> Boolean?): JsPromise<Boolean> {
    TODO("Not yet implemented")
  }

  override fun flatMap(cbk: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun flatMap(cbk: Value, options: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun flatMap(cbk: () -> Iterable<Value>): Readable {
    TODO("Not yet implemented")
  }

  override fun flatMap(options: ReadableFlatMapOptions, cbk: () -> Iterable<Value>): Readable {
    TODO("Not yet implemented")
  }

  override fun drop(limit: Int): Readable {
    TODO("Not yet implemented")
  }

  override fun drop(limit: Int, options: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun drop(limit: Int, options: ReadableDropOptions): Readable {
    TODO("Not yet implemented")
  }

  override fun take(limit: Int): Readable {
    TODO("Not yet implemented")
  }

  override fun take(limit: Int, options: Value): Readable {
    TODO("Not yet implemented")
  }

  override fun take(limit: Int, options: ReadableDropOptions): Readable {
    TODO("Not yet implemented")
  }

  override fun reduce(op: Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun reduce(op: Value, initial: Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun reduce(op: Value, initial: Value, options: Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun reduce(op: () -> Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun reduce(initial: Value, op: () -> Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun reduce(initial: Value, options: ReadableReduceOptions, op: () -> Value): JsPromise<Value> {
    TODO("Not yet implemented")
  }

  override fun getIterator(): Any {
    TODO("Not yet implemented")
  }

  override fun getMember(key: String): Any? = null

  /**
   * Implementation entrypoint for a stream read operation.
   *
   * @param size The amount of data to read; defaults to `0`, meaning "all data"
   * @return The data read from the stream.
   */
  @Throws(EndStreamException::class)
  internal abstract fun readData(size: Int = 0): StringOrBufferOrAny
}

private val writableEventNames = listOf(
  StreamEventName.CLOSE,
  StreamEventName.DRAIN,
  StreamEventName.ERROR,
  StreamEventName.FINISH,
  StreamEventName.PIPE,
  StreamEventName.UNPIPE,
)

internal abstract class AbstractWritable<T> : AbstractStream<T>(), Writable where T : AbstractWritable<T> {
  private val didFinish: AtomicBoolean = AtomicBoolean(false)
  private val needsDrain: AtomicBoolean = AtomicBoolean(false)
  private val corks: AtomicInteger = AtomicInteger(0)
  @Suppress("unused") private val pipes: ConcurrentLinkedQueue<Readable> = ConcurrentLinkedQueue()

  override val writable: Boolean get() = !isDestroyed.get() && !didError.get() && !didEnd.get()
  override val writableAborted: Boolean get() = !didAbort.get()
  override val writableEnded: Boolean get() = didEnd.get()
  override val writableCorked: Int get() = corks.get()
  override val errored: Any? get() = if (didError.get()) streamErr.get() else null
  override val writableFinished: Boolean get() = didFinish.get()
  override val writableHighWaterMark: Int get() = highWaterMark.get()
  override val writableLength: Int get() = bufferSize.get()
  override val writableNeedDrain: Boolean get() = needsDrain.get()
  override val writableObjectMode: Boolean get() = objectModeActive.get()
  override fun eventNames(): List<String> = writableEventNames

  override fun cork() {
    corks.incrementAndGet().also {
      interrupt()
    }
  }

  override fun write(chunk: Any?) = doWrite(chunk)
  override fun write(chunk: StringOrBufferOrAny?, encoding: Value?) = write(chunk, encoding, null)

  override fun write(chunk: StringOrBufferOrAny?, encoding: Value?, callback: Value?) {
    require(callback == null || callback.canExecute()) {
      "Callback must be a function if provided"
    }
    if (callback == null) doWrite(chunk, encodingFromGuestValue(encoding), null) else write(
      chunk,
      encoding,
    ) {
      callback.executeVoid()
    }
  }

  override fun write(chunk: StringOrBufferOrAny?, encoding: Value?, callback: () -> Unit) =
    doWrite(chunk, encodingFromGuestValue(encoding), callback)

  override fun end(): Unit = doEnd(null)
  override fun end(chunk: StringOrBufferOrAny?, encoding: Value?) = end(chunk, encoding, null)
  override fun end(chunk: StringOrBufferOrAny?, encoding: Value?, callback: Value?) {
    require(callback == null || callback.canExecute()) {
      "Callback must be a function if provided"
    }
    if (callback == null) doEnd(chunk, encodingFromGuestValue(encoding), null) else end(
      chunk,
      encoding,
    ) {
      callback.executeVoid()
    }
  }

  override fun end(chunk: StringOrBufferOrAny?, encoding: Value?, callback: () -> Unit): Unit =
    doEnd(chunk, encodingFromGuestValue(encoding), callback)

  /**
   * Implementation entrypoint for a chunked write operation.
   *
   * @param chunk The chunk to write.
   * @param encoding The encoding to use; if unspecified, [chunk] is written as binary data (and is expected to be
   *   binary data in this case), or if [objectModeActive] is `true`, the [chunk] is written as a serialized object,
   *   using JSON encoding.
   * @param callback The callback to invoke.
   */
  internal abstract fun doWrite(chunk: StringOrBufferOrAny?, encoding: Charset? = null, callback: (() -> Unit)? = null)

  /**
   * Implementation entrypoint for a writable stream end operation.
   *
   * @param chunk The chunk to write.
   * @param encoding The encoding to use; if unspecified, [chunk] is written as binary data (and is expected to be
   *   binary data in this case), or if [objectModeActive] is `true`, the [chunk] is written as a serialized object,
   *   using JSON encoding.
   * @param callback The callback to invoke.
   */
  internal abstract fun doEnd(chunk: StringOrBufferOrAny?, encoding: Charset? = null, callback: (() -> Unit)? = null)
}

// Implements a cold input stream, which can accept data from any source.
internal class ColdInputStream : InputStream() {
  companion object {
    @JvmStatic fun create(): ColdInputStream = ColdInputStream()
  }

  private val dirty: AtomicBoolean = AtomicBoolean(false)
  private val buffer: ConcurrentLinkedQueue<Byte> = ConcurrentLinkedQueue()

  fun write(chunk: ByteArray) {
    dirty.set(true)
    buffer.addAll(chunk.toList())
  }

  override fun read(): Int {
    require(dirty.get()) { "Stream is not yet dirty" }
    return buffer.poll()?.toInt() ?: -1
  }

  override fun readNBytes(len: Int): ByteArray {
    require(dirty.get()) { "Stream is not yet dirty" }
    val arr = ByteArray(len)
    for (i in 0 until len) {
      arr[i] = buffer.poll() ?: break
    }
    return arr
  }

  override fun read(b: ByteArray): Int {
    require(dirty.get()) { "Stream is not yet dirty" }
    val len = b.size
    for (i in 0 until len) {
      b[i] = buffer.poll() ?: break
    }
    return len
  }

  override fun readAllBytes(): ByteArray {
    require(dirty.get()) { "Stream is not yet dirty" }
    val arr = ByteArray(buffer.size)
    for (i in arr.indices) {
      arr[i] = buffer.poll() ?: break
    }
    return arr
  }

  override fun read(b: ByteArray, off: Int, len: Int): Int {
    require(dirty.get()) { "Stream is not yet dirty" }
    for (i in off until off + len) {
      b[i] = buffer.poll() ?: break
    }
    return len
  }

  override fun available(): Int = buffer.size
}

// Implementation of a wrapped/proxied `InputStream` as a Node `Readable`.
internal class WrappedInputStream private constructor(
  private val backing: InputStream,
  initialEncoding: Charset?,
) : Readable, AbstractReadable<WrappedInputStream>(initialEncoding) {
  companion object {
    @JvmStatic fun wrap(stream: InputStream, encoding: Charset?): WrappedInputStream =
      WrappedInputStream(stream, encoding)
  }

  private val reader by lazy {
    backing.bufferedReader(encoding.get() ?: StandardCharsets.UTF_8)
  }

  override fun size(): Int = withActive { backing.available() }

  @Throws(EndStreamException::class)
  override fun readData(size: Int): StringOrBufferOrAny = withActive {
    if (backing.available() == 0) {
      throw EndStreamException()
    }
    when (encoding.get()) {
      null -> if (size == 0) {
        backing.readAllBytes()
      } else {
        backing.readNBytes(size)
      }

      else -> if (size == 0) {
        reader.readText()
      } else {
        val buf = CharBuffer.allocate(size)
        reader.read(buf)
        buf.flip()
        buf.toString()
      }
    }
  }
}

// Implementation of a wrapped/proxied `OutputStream` as a Node `Writable`.
@OptIn(DelicateElideApi::class)
internal class WrappedOutputStream private constructor(backing: OutputStream) :
  Writable,
  AbstractWritable<WrappedOutputStream>() {
  companion object {
    @JvmStatic fun wrap(stream: OutputStream): WrappedOutputStream = WrappedOutputStream(stream)
  }

  // Buffer for output stream.
  private val rawBuffer: BufferedOutputStream = BufferedOutputStream(backing)

  // Callbacks on next flush.
  private val enqueuedCallbacks: ConcurrentLinkedQueue<() -> Unit> = ConcurrentLinkedQueue()

  internal fun flush() {
    notifyData()
  }

  override fun notifyData() {
    rawBuffer.flush()
    bufferSize.set(0)
    while (bufferSize.get() > 0) {
      enqueuedCallbacks.poll()?.invoke()
    }
  }

  private fun bufferChunkForWrite(chunk: StringOrBufferOrAny?): Pair<ByteArray, Int> {
    return when (chunk) {
      is ByteArray -> chunk to chunk.size
      is String -> chunk.toByteArray(encoding.get() ?: StandardCharsets.UTF_8) to chunk.length
      is BufferInstance -> TODO("Node buffers are not supported for writes to other buffers yet")

      is Value -> when {
        chunk.isString -> bufferChunkForWrite(chunk.asString())
        chunk.isNull -> TODO("writing null to a buffer")
        chunk.isBoolean -> TODO("writing a boolean to a buffer")
        chunk.isNumber -> TODO("numbers written to buffer")

        chunk.hasBufferElements() -> {
          val sizeL = chunk.bufferSize
          if (sizeL > Int.MAX_VALUE) error("Cannot allocate buffer of size $sizeL")
          val size = sizeL.toInt()
          val arr = ByteArray(size)
          chunk.readBuffer(0, arr, 0, size)
          arr to size
        }

        else -> throw JsError.valueError("Unrecognized type for buffer write")
      }

      else -> throw IllegalArgumentException("Unsupported chunk type")
    }
  }

  override fun size(): Int = bufferSize.get()

  override fun doWrite(chunk: StringOrBufferOrAny?, encoding: Charset?, callback: (() -> Unit)?) = withActive {
    try {
      bufferChunkForWrite(chunk).let { (chunk, size) ->
        rawBuffer.write(chunk).also {
          val high = highWaterMark.get()
          bufferSize.addAndGet(size)
          if (size > high) highWaterMark.set(size)
          callback?.let { enqueuedCallbacks.add(it) }
        }
      }
    } finally {
      notifyData()
    }
  }

  override fun doEnd(chunk: StringOrBufferOrAny?, encoding: Charset?, callback: (() -> Unit)?) = withActive {
    require(!didEnd.get()) { "Cannot close/end a stream twice" }
    if (chunk != null) {
      doWrite(chunk, encoding, callback)
      notifyData()
    }
    didEnd.compareAndSet(false, true)
    emit(StreamEventName.END)
    close()
  }
}

/**
 * # Node API: Stream
 */
internal class NodeStream private constructor () : ReadOnlyProxyObject, StreamAPI {
  //

  internal companion object {
    @JvmStatic fun create(): NodeStream = NodeStream()
  }

  // @TODO not yet implemented

  override fun getMemberKeys(): Array<String> = emptyArray()
  override fun getMember(key: String?): Any? = null
}
