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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.withLock
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferValue
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType.Uint8Array
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableByteStream.PullDescriptor.ReaderType
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableByteStream.PullDescriptor.ReaderType.None
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.err.RangeError
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.*
import elide.vm.annotations.Polyglot

/**
 * A specialized [ReadableStream] implementation matching the behavior of a WHATWG readable stream with a BYOB
 * controller. The type of the underlying [source] _must_ be "BYOB", otherwise the behavior of the stream is
 * unspecified.
 */
internal class ReadableByteStream(
  /** Underlying source for this stream. */
  private val source: ReadableStreamSource,
  /** Queueing strategy used to manage backpressure. */
  private val strategy: QueueingStrategy,
) : ReadableStreamBase() {
  /**
   * A chunk wrapping a [buffer] to be written into by the stream's source; only the region of [buffer] between
   * [offset] and [offset] + [length] should be written into or read.
   */
  private data class ByobChunk(val buffer: ArrayBufferValue, var offset: Long, var length: Long) {
    constructor(buffer: ByteBuffer, offset: Long, length: Long) : this(Value.asValue(buffer), offset, length)
  }

  /**
   * Represents a [pull-into descriptor](https://streams.spec.whatwg.org/#pull-into-descriptor) used when a BYOB reader
   * provides a buffer to read data into, or when a default reader requests data while an auto-allocation policy is
   * active.
   *
   * Pull descriptors are managed through the [pullQueue], the head of which is exposed via the stream controller's
   * BYOB [request]. The underlying [source] can use the BYOB request API to fill the buffer, notifying completion
   * through [ReadableStreamBYOBRequest.respond] and [ReadableStreamBYOBRequest.respondWithNewView].
   *
   * Once filled up to or beyond the [minimumFill] threshold, a pull descriptor will be used to either fulfill the read
   * request that originated it, or, in the case of detached descriptors, as a loose chunk in the [chunkQueue] that can
   * be used to further fill new pull requests.
   */
  private class PullDescriptor(
    /**
     * The buffer into which data should be written; only the region starting at [offset] + [filled], and ending at
     * [offset] + [length] should be written to.
     */
    var buffer: ArrayBufferValue,
    /** Offset within [buffer] at which to start writing data (not accounting for the [filled] bytes). */
    val offset: Long,
    /** Max length within [buffer] to be written in total (not accounting for the current [filled] bytes). */
    val length: Long,
    /** Minimum number of bytes that should be [filled] before the descriptor is committed or enqueued as a chunk. */
    val minimumFill: Long,
    /** Size of the elements in the typed array this descriptor will be transformed into when committed. */
    val elementSize: Int,
    /** The type of the reader that originated the pull request. */
    val readerType: ReaderType,
    /** The type of array to be created when this descriptor is committed. */
    val viewType: ArrayBufferViewType
  ) {
    /**
     * The type of the reader that originated the pull request; [None] indicates a detached descriptor that originated
     * from a now-released reader.
     */
    enum class ReaderType {
      None,
      Default,
      BYOB,
    }

    /** Number of bytes filled in this descriptor. */
    var filled: Long = 0
  }

  /**
   * An implementation of the [ReadableStreamBYOBRequest] interface that delegates to stream methods. The stream should
   * [invalidate] the request when the head of the [pullQueue] changes to avoid illegal access to pull descriptors.
   */
  private class RequestDelegate(
    @get:Polyglot override var view: Value?,
    private var stream: ReadableByteStream?,
  ) : ReadableStreamBYOBRequest {
    /**
     * Invalidate this request, disallowing any future [respond] and [respondWithNewView] calls. The [view] will be set
     * to `null` to avoid further references to the head of the pull queue.
     */
    fun invalidate() {
      view = null
      stream = null
    }

    @Polyglot override fun respond(bytesWritten: Long) {
      stream?.respondWithSize(bytesWritten) ?: throw TypeError.create("")
    }

    @Polyglot override fun respondWithNewView(view: Value) {
      stream?.respondWithView(view) ?: throw TypeError.create("")
    }
  }

  /**
   * A queue of pull-into descriptors, representing BYOB reads to be satisfied. Pull descriptors may be transformed
   * into [chunks][chunkQueue] under certain conditions (see [fulfillOrEnqueue]) or used directly to satisfy incoming
   * BYOB reads.
   *
   * Descriptors may become "detached" if the originating reader is released, in which case its
   * [PullDescriptor.readerType] will be set to [PullDescriptor.ReaderType.None]; however, only the head of the pull
   * queue can be detached (and only when it has been filled partially), as all empty descriptors are discarded when a
   * reader is released.
   *
   * Modifying this queue is strictly forbidden unless the [queueLock] is held by the current thread. This is
   * required to avoid race conditions when transferring a chunk to the [readQueue].
   */
  private val pullQueue = LinkedList<PullDescriptor>()

  /**
   * A queue used to buffer undelivered data enqueued by the underlying [source].
   *
   * Unlike with default streams, the data in this queue might not always be delivered directly during reads; instead
   * it is mostly used to fulfill descriptors in the [pullQueue] in the order they appear. Default readers may still
   * cause chunks to be delivered directly.
   *
   * Modifying this queue is strictly forbidden unless the [queueLock] is held by the current thread. This is
   * required to avoid race conditions when transferring a chunk to the [readQueue].
   */
  private val chunkQueue = LinkedList<ByobChunk>()

  /**
   * The total size (in bytes) of the chunks in the [chunkQueue]. The length of each chunk is measured by the length of
   * its backing buffer, as opposed to using the queueing [strategy].
   */
  private val queueSize = AtomicLong()

  /**
   * Reference to a BYOB request wrapping the head of the [pullQueue]. The underlying [source] may use this value to
   * write directly into the head of the queue (as an alternative to enqueuing a chunk via [fulfillOrEnqueue]).
   *
   * The request is invalidated and unset when the source [responds][respondWithSize] to it, or as a side effect of
   * other operations.
   */
  private val request = AtomicReference<RequestDelegate>()

  /** Controller interface passed to the underlying [source], allowing it to control the stream and enqueue chunks. */
  private val controller = ReadableStreamByteControllerToken(this)

  @get:Polyglot override val locked: Boolean get() = lockedReader.get() != null

  init {
    // initialize the source
    setupSource(source, controller)
  }

  /**
   * Transform a pull [descriptor] into a [ByobChunk] and add it to the [chunkQueue]. The [queueLock] *must* be held
   * by the calling thread, otherwise an exception will be thrown.
   */
  private fun enqueueAsChunk(descriptor: PullDescriptor) {
    assert(queueLock.isHeldByCurrentThread)
    chunkQueue.add(ByobChunk(descriptor.buffer, descriptor.offset, descriptor.filled))
    queueSize.addAndGet(descriptor.filled)
  }

  /**
   * Wrap a buffer [view] as a [ByobChunk] and add it to the [chunkQueue]. The [queueLock] *must* be held by the
   * calling thread, otherwise an exception will be thrown.
   */
  private fun enqueueAsChunk(view: Value) {
    assert(queueLock.isHeldByCurrentThread)

    val length = ArrayBufferViews.getLength(view)
    val offset = ArrayBufferViews.getOffset(view)
    val buffer = ArrayBufferViews.getBackingBuffer(view)

    chunkQueue.add(ByobChunk(buffer, offset, length))
    queueSize.addAndGet(length)
  }

  /**
   * Fulfill requests in the [readQueue] using values from the [chunkQueue], until either queue is empty. The returned
   * value indicates whether all requests were satisfied in this way.
   *
   * This operation must only be used when the current [lockedReader] is a default reader, otherwise the resulting
   * desync of the queues will break the stream's behavior. Additionally, the [queueLock] *must* be held by the
   * calling thread, otherwise an exception will be thrown.
   */
  private fun fulfillDefaultReadsWithQueue(): Boolean {
    assert(queueLock.isHeldByCurrentThread)

    // fulfill all possible read requests in the queue using the chunk queue; returns whether all reads were fulfilled
    while (chunkQueue.isNotEmpty() && readQueue.isNotEmpty()) {
      val chunk = chunkQueue.poll()
      queueSize.addAndGet(-chunk.length)

      // create a new UInt8Array instance wrapping the buffer
      val view = ArrayBufferViews.newView(Uint8Array, chunk.buffer, chunk.offset, chunk.length)
      readQueue.poll().resolve(ReadResult(view, done = false))
    }

    return readQueue.isEmpty()
  }

  /**
   * Commit the [descriptor], delivering it as a [ReadResult] to the head of the [readQueue]. The [queueLock] must
   * be held by the current thread and the [readQueue] must not be empty, otherwise an exception will be thrown.
   */
  private fun commitPullDescriptor(descriptor: PullDescriptor) {
    assert(queueLock.isHeldByCurrentThread)
    assert(readQueue.isNotEmpty())

    val state = streamState.get()
    if (state == STREAM_CLOSED) check(descriptor.filled % descriptor.elementSize == 0L)

    // create a new buffer view to be delivered in a read requests with the appropriate element size
    val view = ArrayBufferViews.newView(
      type = descriptor.viewType,
      buffer = descriptor.buffer,
      offset = descriptor.offset,
      length = descriptor.filled,
    )

    readQueue.poll().resolve(ReadResult(view, done = state == STREAM_CLOSED))
  }

  /**
   *  Fulfill as many descriptors from the [pullQueue] as possible, using the data from the [chunkQueue], and commit
   *  all descriptors filled in this way. The returned value indicates whether all descriptors in the queue were
   *  committed as a result.s
   */
  private fun fulfillPullsWithQueueAndCommit(): Boolean {
    // fulfill all possible pull requests using the chunk queue; returns whether all requests were fulfilled
    var fulfilled = true

    while (pullQueue.isNotEmpty()) {
      if (!fulfillPullWithQueue(pullQueue.peek())) {
        // the chunk queue wasn't enough to fulfill all pulls, stop
        fulfilled = false
        break
      }

      commitPullDescriptor(pullQueue.poll())
    }

    return fulfilled
  }

  /**
   * Fulfill a pull [descriptor] using the data from the [chunkQueue], up to the maximum length specified by it. Chunks
   * that are not exhausted will remain at the head of the queue, with an updated offset and length, so they can be
   * used to fulfill more descriptors.
   */
  private fun fulfillPullWithQueue(descriptor: PullDescriptor): Boolean {
    var remaining = (descriptor.length - descriptor.filled).coerceAtMost(queueSize.toLong())
    val ready = descriptor.filled + remaining >= descriptor.minimumFill

    while (remaining > 0) {
      val chunk = chunkQueue.peek()

      val length = chunk.length.coerceAtMost(remaining)
      val dstOffset = descriptor.offset + descriptor.filled
      val srcOffset = chunk.offset

      // Note: it would be great to use a bulk operation here, but we can't assume any of the operands are actually
      // ByteBuffer instances, so we're forced to use polyglot operations; there must be a better way
      for (i in 0 until length)
        descriptor.buffer.writeBufferByte(dstOffset + i, chunk.buffer.readBufferByte(srcOffset + i))

      remaining -= length
      descriptor.filled += length

      if (length == chunk.length) {
        // only remove the chunk from the queue if it's spent
        chunkQueue.poll()
        queueSize.addAndGet(-chunk.length)
      } else {
        // otherwise update it so it gets consumed by the next pull
        chunk.offset += length
        chunk.length -= length
      }
    }

    return ready
  }

  /** Clear and invalidate the current [request], preventing the [source] from using it. */
  private fun invalidateRequest() {
    // invalidate the public request exposed via the controller
    request.getAndSet(null)?.invalidate()
  }

  /** Called whenever the [queueSize] decreases, e.g. allowing the stream to consider whether pulling is needed. */
  private fun handleDrain() {
    if (queueSize.get() == 0L && sourceState.get() == SOURCE_CLOSING) cleanup()
    else maybePull()
  }

  /**
   *  Finalize this stream, setting its state to "closed", and releasing the locked [reader]. Pending descriptors in
   *  the [pullQueue] will be discarded, and pending reads in the [readQueue] will be completed with a `null` value.
   */
  private fun cleanup() {
    queueLock.withLock {
      while (pullQueue.isNotEmpty()) {
        val head = pullQueue.poll()
        if (head.filled % head.elementSize == 0L) continue

        val err = TypeError.create("")
        error(err)
        throw err
      }

      chunkQueue.clear()

      if (lockedReader.get() is ReadableStreamDefaultReader) {
        while (readQueue.isNotEmpty()) readQueue.poll().resolve(ReadResult(null, true))
      }
    }

    streamState.set(STREAM_CLOSED)
    sourceState.set(SOURCE_CLOSED)
    lockedReader.getAndSet(null)?.close()
  }

  /**
   * Respond to the stream's current [request] with the given amount of [bytesWritten]. This method is meant to be used
   * by the [RequestDelegate] implementation to notify the stream about a response from the source.
   */
  private fun respondWithSize(bytesWritten: Long) {
    queueLock.withLock {
      val head = checkNotNull(pullQueue.peek())
      val state = streamState.get()

      if (state == STREAM_CLOSED) {
        if (bytesWritten != 0L) throw TypeError.create("")
      } else {
        check(state == STREAM_READABLE)
        if (bytesWritten == 0L) throw TypeError.create("")
        if (head.filled + bytesWritten > head.length) throw RangeError.create("")
      }

      finalizeResponse(bytesWritten)
    }
  }

  /**
   * Respond to the stream's current [request] with a new view, resulting in the view's underlying buffer being
   * transferred to the head descriptor of the pull queue and used to fulfill reads.
   *
   * This method is meant to be used by the [RequestDelegate] implementation to notify the stream about a response
   * from the source.
   */
  @Suppress("ThrowsCount")
  private fun respondWithView(view: Value) {
    queueLock.withLock {
      val head = checkNotNull(pullQueue.peek())
      val state = streamState.get()

      val byteLength = ArrayBufferViews.getLength(view)
      val byteOffset = ArrayBufferViews.getOffset(view)
      val arrayBuffer = ArrayBufferViews.getBackingBuffer(view)

      if (state == STREAM_CLOSED) {
        if (byteLength != 0L) throw TypeError.create("")
      } else {
        check(state == STREAM_READABLE)
        if (byteLength == 0L) throw TypeError.create("")
      }

      if (head.offset + head.filled != byteOffset) throw RangeError.create("")
      if (head.buffer.bufferSize != byteLength) throw RangeError.create("")
      if (head.filled + byteLength > head.length) throw RangeError.create("")

      head.buffer = arrayBuffer
      finalizeResponse(byteLength)
    }
  }

  /**
   * Finalize a response from the [source], notified via [respondWithSize] or [respondWithView], by updating the bytes
   * written in the head pull descriptor and attempting to fulfill pending requests with the new data, as applicable.
   *
   * Calling this method invalidates the current [request].
   */
  private fun finalizeResponse(bytesWritten: Long) {
    check(queueLock.isHeldByCurrentThread)
    invalidateRequest()

    val head = checkNotNull(pullQueue.peek())

    when (streamState.get()) {
      STREAM_CLOSED -> {
        check(head.filled % head.elementSize == 0L)
        if (head.readerType == None) pullQueue.poll()

        if (lockedReader.get() is ReadableStreamBYOBReader) {
          while (pullQueue.isNotEmpty()) commitPullDescriptor(pullQueue.poll())
        }
      }

      else -> {
        head.filled += bytesWritten
        if (head.readerType == None) {
          pullQueue.poll()
          enqueueAsChunk(head)
          fulfillPullsWithQueueAndCommit()
        } else if (head.filled >= head.minimumFill) {
          pullQueue.poll()

          // trim non-aligned bytes and enqueue them as a chunk
          val excess = head.filled % head.elementSize
          if (excess > 0) {
            val end = head.offset + head.filled
            val buffer = ByteBuffer.allocate(excess.toInt())
            val srcOffset = end - excess

            // Note: this is another instance where bulk writes would be nice, but it's not as bad since it's at
            // most copying 7 bytes (max element size is 8 and this is a remainder)
            for (i in 0 until excess) buffer.put(i.toInt(), head.buffer.readBufferByte(srcOffset + i))

            chunkQueue.offer(ByobChunk(buffer, 0, excess))
            queueSize.addAndGet(excess)
          }

          head.filled -= excess

          commitPullDescriptor(head)
          fulfillPullsWithQueueAndCommit()
        }
      }
    }

    maybePull()
  }

  /** Return the request wrapping the head of this stream's pull queue, if it is not empty. */
  internal fun getRequest(): ReadableStreamBYOBRequest? {
    request.get()?.let { return it }

    val newRequest = queueLock.withLock {
      pullQueue.peek()?.let { pull ->
        // create a new UInt8Array instance wrapping the buffer
        val view = ArrayBufferViews.newView(
          Uint8Array,
          pull.buffer,
          pull.offset + pull.filled,
          pull.length - pull.filled,
        )
        RequestDelegate(view, this)
      }.also {
        request.set(it)
      }
    }

    return newRequest
  }

  /**
   * Read at least [min] bytes into the given [view], drawing from buffered chunks in the stream's queue, or enqueueing
   * the read internally if not enough data is available.
   */
  @Suppress("ReturnCount")
  internal fun readOrEnqueue(view: Value, min: Long): JsPromise<ReadResult> {
    val state = streamState.get()

    // disallow reading from an errored stream
    if (state == STREAM_ERRORED) return JsPromise.rejected(errorCause.get())

    // make a new pull request using this view and the read options
    val elementSize = ArrayBufferViews.getElementSize(view)
    val arrayType = ArrayBufferViews.getViewType(view)
    val byteLength = ArrayBufferViews.getLength(view)

    val minimumFill = min * elementSize
    check(minimumFill >= 0 && minimumFill <= byteLength)
    check(minimumFill % elementSize == 0L)

    val arrayBuffer = ArrayBufferViews.getBackingBuffer(view)
    val byteOffset = ArrayBufferViews.getOffset(view)

    val pull = PullDescriptor(
      buffer = arrayBuffer,
      length = byteLength,
      offset = byteOffset,
      minimumFill = minimumFill,
      elementSize = elementSize,
      readerType = ReaderType.BYOB,
      viewType = arrayType,
    )

    val promise = queueLock.withLock {
      // if there are pull requests backed up, we can't fulfill the new one immediately
      if (pullQueue.isNotEmpty()) {
        pullQueue.offer(pull)
        return JsPromise<ReadResult>().also { readQueue.add(it) }
      }

      // if the stream is closed, respond with an empty view
      if (state == STREAM_CLOSED) return JsPromise.resolved(
        ReadResult(
          value = ArrayBufferViews.newView(arrayType, ByteBuffer.allocate(0), 0, 0),
          done = true,
        ),
      )

      // if there are available chunks, attempt to fulfill the new request
      if (queueSize.get() > 0) {
        if (fulfillPullWithQueue(pull)) {
          handleDrain()
          // create a new buffer view to be delivered in a read requests with the appropriate element size
          val resultView = ArrayBufferViews.newView(
            type = pull.viewType,
            buffer = pull.buffer,
            offset = pull.offset,
            length = pull.filled,
          )

          return JsPromise.resolved(ReadResult(resultView, done = false))
        }

        // we couldn't fulfill the request, and no new chunks will arrive, so we error
        if (sourceState.get() >= SOURCE_CLOSING) {
          val reason = TypeError.create("")
          error(reason)
          return JsPromise.rejected(reason)
        }
      }

      // if we reach here, the request is partially filled at best, enqueue it
      pullQueue.offer(pull)
      JsPromise<ReadResult>().also { readQueue.add(it) }
    }

    maybePull()
    return promise
  }

  override fun maybePull() {
    if (shouldPull()) pull(source, controller)
  }

  override fun desiredSize(): Double? = when (streamState.get()) {
    STREAM_READABLE -> strategy.highWaterMark() - queueSize.get()
    STREAM_CLOSED -> 0.0
    else -> null
  }

  override fun readOrEnqueue(): JsPromise<ReadResult> {
    return when (streamState.get()) {
      STREAM_CLOSED -> JsPromise.resolved(ReadResult(null, done = true))
      STREAM_ERRORED -> JsPromise.rejected(errorCause.get())
      else -> {
        val readHandle = queueLock.withLock {
          // if there is a queued chunk, use it
          chunkQueue.poll()?.let {
            queueSize.addAndGet(-it.length)
            handleDrain()

            val view = ArrayBufferViews.newView(Uint8Array, it.buffer, it.offset, it.length)
            return@withLock JsPromise.resolved(ReadResult(view, false))
          }

          // if there is an auto-allocate policy in effect, use it to create a pull request for this read
          if (source.autoAllocateChunkSize > 0) {
            val buffer = ByteBuffer.allocate(source.autoAllocateChunkSize.toInt())
            val request = PullDescriptor(
              buffer = Value.asValue(buffer),
              offset = 0,
              length = buffer.limit().toLong(),
              minimumFill = 1,
              elementSize = 1,
              readerType = ReaderType.Default,
              viewType = Uint8Array,
            )

            pullQueue.offer(request)
          }

          // create the read promise and enqueue it
          JsPromise<ReadResult>().also { readQueue.add(it) }
        }

        maybePull()
        readHandle
      }
    }
  }

  override fun fulfillOrEnqueue(chunk: Value) {
    queueLock.withLock {
      // check for a detached pull request at the head of the queue, if it is partially filled, transfer it to the
      // chunk queue, otherwise discard it
      pullQueue.peek()?.also { invalidateRequest() }?.takeIf { it.readerType == None }?.let {
        if (it.filled > 0) enqueueAsChunk(it)
        pullQueue.poll()
      }

      when (lockedReader.get()) {
        // if the stream is locked to a default reader, and there are pending read requests, fulfill as many as
        // possible with the queue. If all reads are satisfied in this way, add the new chunk to the queue, otherwise
        // use the chunk to satisfy the next pending read
        is ReadableStreamDefaultReaderToken -> {
          fulfillDefaultReadsWithQueue()
          readQueue.poll()?.resolve(
            ReadResult(
              ArrayBufferViews.newView(
                type = Uint8Array,
                buffer = ArrayBufferViews.getBackingBuffer(chunk),
                offset = ArrayBufferViews.getOffset(chunk),
                length = ArrayBufferViews.getLength(chunk),
              ),
              false,
            ),
          ) ?: enqueueAsChunk(chunk)
        }

        // if the stream is locked to a BYOB reader, add the new chunk to the queue, and fulfill as many pull requests
        // as possible
        is ReadableStreamByobReaderToken -> {
          enqueueAsChunk(chunk)
          fulfillPullsWithQueueAndCommit()
        }

        // if the stream is not locked, enqueue the chunk
        else -> enqueueAsChunk(chunk)
      }
    }

    maybePull()
  }

  override fun error(reason: Any?) {
    if (!streamState.compareAndSet(STREAM_READABLE, STREAM_ERRORED)) return
    errorCause.set(reason)

    invalidateRequest()
    lockedReader.getAndSet(null)?.error(reason)

    queueLock.withLock {
      while (readQueue.isNotEmpty()) readQueue.poll()?.reject(reason)
      chunkQueue.clear()
      queueSize.set(0L)
    }
  }

  override fun close() {
    if (sourceState.getAndSet(SOURCE_CLOSING) >= SOURCE_CLOSING)
      throw TypeError.create("Stream is already closing or closed")

    if (streamState.get() != STREAM_READABLE)
      throw TypeError.create("Failed to close: stream is not readable")

    // only fully close if there are no undelivered chunks
    if (queueSize.get() == 0L) cleanup()
  }

  override fun release() {
    lockedReader.getAndSet(null)
  }

  @Polyglot override fun cancel(reason: Any?): JsPromise<Unit> {
    return when (streamState.get()) {
      STREAM_CLOSED -> JsPromise.resolved(Unit)
      STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
      else -> {
        close()

        queueLock.withLock {
          pullQueue.clear()
          queueSize.set(0L)
          chunkQueue.clear()
          source.cancel(reason)
        }
      }
    }
  }

  @Polyglot override fun getReader(options: Any?): ReadableStreamReader {
    val mode = (options as? Value)?.takeIf { it.hasMember("mode") }?.let { opts ->
      opts.getMember("mode").takeIf { it.isString }?.asString()
    }

    val reader = if (mode == "byob") ReadableStreamByobReaderToken(this)
    else ReadableStreamDefaultReaderToken(this)

    if (!lockedReader.compareAndSet(null, reader)) throw TypeError.create("Stream is already locked to a reader")

    // if the stream is not readable, close the reader before returning it
    when (streamState.get()) {
      STREAM_ERRORED -> reader.error(errorCause.get())
      STREAM_CLOSED -> reader.close()
    }

    return reader
  }

  @Polyglot override fun pipeThrough(transform: TransformStream, options: Any?): ReadableStream {
    TODO("Not yet implemented")
  }

  @Polyglot override fun pipeTo(destination: WritableStream, options: Any?): JsPromise<Unit> {
    TODO("Not yet implemented")
  }

  @Polyglot override fun tee(): Array<ReadableStream> {
    TODO("Not yet implemented")
  }
}
