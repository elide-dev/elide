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

import com.google.common.util.concurrent.AtomicDouble
import org.graalvm.polyglot.Value
import java.util.*
import kotlinx.atomicfu.locks.withLock
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamReader
import elide.runtime.intrinsics.js.stream.ReadableStreamSource
import elide.vm.annotations.Polyglot

/**
 * A specialized [ReadableStream] implementation matching the behavior of a WHATWG readable stream with a default
 * controller. The type of the underlying [source] _must_ be "default", otherwise the behavior of the stream is
 * unspecified.
 */
internal class ReadableDefaultStream(
  /** Underlying source for this stream. */
  private val source: ReadableStreamSource,
  /** Queueing strategy used to manage backpressure. */
  private val strategy: QueueingStrategy,
) : ReadableStreamBase() {
  /** Simple chunk type allowing an arbitrary untyped value to be enqueued with a computed size. */
  private data class SizedChunk(val chunk: Any?, val size: Double)

  /**
   * Incoming chunk queue, used to store undelivered data enqueued by the stream's [source]. Note that, if there are
   * pending reads when a new chunk is enqueued, this queue will not be modified, and instead the data will be consumed
   * directly.
   *
   * Modifying this queue is strictly forbidden unless the [queueLock] is held by the current thread. This is
   * required to avoid race conditions when transferring a chunk to the [readQueue].
   */
  private val chunkQueue = LinkedList<SizedChunk>()

  /**
   * The sum of the sizes of all elements in the [chunkQueue], as measured by the stream's queueing [strategy]. This
   * size is used to compute the [desiredSize] of the stream's queue, which may be used by the [source] to support
   * backpressure.
   *
   * Reading this value is always safe, even concurrently from multiple threads, but writing to it is not, since it
   * _must_ match the current state of the [chunkQueue]. Modifying this value is strictly forbidden unless the current
   * thread holds the [queueLock].
   */
  private val queueSize = AtomicDouble()

  /** Controller interface passed to the underlying [source], allowing it to control the stream and enqueue chunks. */
  private val controller = ReadableStreamDefaultControllerToken(this)

  @get:Polyglot override val locked: Boolean get() = lockedReader.get() != null

  init {
    // initialize the source
    setupSource(source, controller)
  }

  override fun maybePull() {
    if (shouldPull()) pull(source, controller)
  }

  /** Finalize this stream, setting its state to "closed", and releasing the locked [reader]. */
  private fun cleanup() {
    streamState.set(STREAM_CLOSED)
    lockedReader.getAndSet(null)?.close()
  }

  override fun desiredSize(): Double? = when (streamState.get()) {
    STREAM_READABLE -> strategy.highWaterMark() - queueSize.get()
    STREAM_CLOSED -> 0.0
    else -> null
  }

  override fun readOrEnqueue(): JsPromise<ReadResult> = when (streamState.get()) {
    STREAM_CLOSED -> JsPromise.resolved(ReadResult(null, done = true))
    STREAM_ERRORED -> JsPromise.rejected(errorCause.get())
    else -> {
      val result = queueLock.withLock {
        val cached = chunkQueue.poll()

        if (cached != null) {
          // resolve directly using a cached chunk, this may finish closing the stream, so other pending
          // reads will be aborted, and new reads will fail
          if (chunkQueue.isEmpty() && sourceState.compareAndSet(SOURCE_CLOSING, SOURCE_CLOSED)) {
            cleanup()
          }

          queueSize.addAndGet(-cached.size)
          JsPromise.resolved(ReadResult(cached.chunk, done = streamState.get() != STREAM_READABLE))
        } else {
          // create a new promise for the read and enqueue it
          JsPromise<ReadResult>().also(readQueue::offer)
        }
      }

      // queue might be empty, pull if needed
      maybePull()

      result
    }
  }

  override fun fulfillOrEnqueue(chunk: Value) {
    // disallow writes when closing or errored
    if (streamState.get() != STREAM_READABLE || sourceState.get() >= SOURCE_CLOSING)
      throw TypeError.create("Stream is not readable")

    queueLock.withLock {
      // directly fulfill a pending request if possible
      readQueue.poll()?.let {
        it.resolve(ReadResult(chunk, false))
        return@withLock
      }

      // enqueue chunk for future use
      val size = strategy.size(chunk)
      chunkQueue.offer(SizedChunk(chunk, size))
      queueSize.addAndGet(size)
    }

    maybePull()
  }

  override fun error(reason: Any?) {
    if (!streamState.compareAndSet(STREAM_READABLE, STREAM_ERRORED)) return
    errorCause.set(reason)

    lockedReader.getAndSet(null)?.error(reason)

    queueLock.withLock {
      while (readQueue.isNotEmpty()) readQueue.poll()?.reject(reason)
      chunkQueue.clear()
      queueSize.set(0.0)
    }
  }

  override fun close() {
    if (sourceState.getAndSet(SOURCE_CLOSING) >= SOURCE_CLOSING)
      throw TypeError.create("Stream is already closing or closed")

    if (streamState.get() != STREAM_READABLE)
      throw TypeError.create("Failed to close: stream is not readable")

    // only fully close if there are no undelivered chunks
    if (chunkQueue.isEmpty()) cleanup()
  }

  override fun release() {
    lockedReader.getAndSet(null)
  }

  @Polyglot override fun getReader(options: Any?): ReadableStreamReader {
    // creating the reader prematurely is not an issue: if it can't be used, we're throwing anyway
    val reader = ReadableStreamDefaultReaderToken(this)
    if (!lockedReader.compareAndSet(null, reader)) throw TypeError.create("Stream is already locked to a reader")

    // if the stream is not readable, close the reader before returning it
    when (streamState.get()) {
      STREAM_ERRORED -> reader.error(errorCause.get())
      STREAM_CLOSED -> reader.close()
    }

    return reader
  }

  @Polyglot override fun cancel(reason: Any?): JsPromise<Unit> {
    return when (streamState.get()) {
      STREAM_CLOSED -> JsPromise.resolved(Unit)
      STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
      else -> {
        close()

        queueLock.withLock {
          queueSize.set(0.0)
          chunkQueue.clear()
          source.cancel(reason)
        }
      }
    }
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
