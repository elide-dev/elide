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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.atomicfu.locks.withLock
import elide.runtime.exec.GuestExecutor
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueuingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamController
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
  private val strategy: QueuingStrategy,
  override val executor: GuestExecutor
) : ReadableStreamBase() {
  /** Simple chunk type allowing an arbitrary untyped value to be enqueued with a computed size. */
  private data class SizedChunk(val chunk: Value?, val size: Double)

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
    streamState.set(READABLE_STREAM_CLOSED)
    lockedReader.getAndSet(null)?.close()
    queueLock.withLock {
      while (readQueue.isNotEmpty()) readQueue.poll()?.resolve(ReadResult(null, done = true))
    }
  }

  internal fun hasBackpressure(): Boolean = !shouldPull()

  internal fun canCloseOrEnqueue(): Boolean {
    return streamState.get() == READABLE_STREAM_READABLE && sourceState.get() < SOURCE_CLOSING
  }

  override fun desiredSize(): Double? = when (streamState.get()) {
    READABLE_STREAM_READABLE -> strategy.highWaterMark() - queueSize.get()
    READABLE_STREAM_CLOSED -> 0.0
    else -> null
  }

  override fun readOrEnqueue(request: CompletableJsPromise<ReadResult>?): JsPromise<ReadResult> {
    return when (streamState.get()) {
      READABLE_STREAM_CLOSED -> {
        val result = ReadResult(null, done = true)
        request?.apply { resolve(result) } ?: JsPromise.resolved(result)
      }

      READABLE_STREAM_ERRORED -> {
        request?.apply { reject(errorCause.get()) } ?: JsPromise.rejected(errorCause.get())
      }

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
            val result = ReadResult(cached.chunk, done = streamState.get() != READABLE_STREAM_READABLE)
            request?.apply { resolve(result) } ?: JsPromise.resolved(result)
          } else {
            // create a new promise for the read and enqueue it
            (request ?: JsPromise()).also(readQueue::offer)
          }
        }

        // queue might be empty, pull if needed
        maybePull()

        result
      }
    }
  }

  override fun fulfillOrEnqueue(chunk: Value) {
    // disallow writes when closing or errored
    if (streamState.get() != READABLE_STREAM_READABLE || sourceState.get() >= SOURCE_CLOSING)
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
    if (!streamState.compareAndSet(READABLE_STREAM_READABLE, READABLE_STREAM_ERRORED)) return
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

    if (streamState.get() != READABLE_STREAM_READABLE)
      throw TypeError.create("Failed to close: stream is not readable")

    // only fully close if there are no undelivered chunks
    if (chunkQueue.isEmpty()) cleanup()
  }

  override fun releaseReader() {
    lockedReader.getAndSet(null)
  }

  @Polyglot override fun getReader(options: Value?): ReadableStreamReader {
    // creating the reader prematurely is not an issue: if it can't be used, we're throwing anyway
    val reader = ReadableStreamDefaultReaderToken(this)
    if (!lockedReader.compareAndSet(null, reader)) throw TypeError.create("Stream is already locked to a reader")

    // if the stream is not readable, close the reader before returning it
    when (streamState.get()) {
      READABLE_STREAM_ERRORED -> reader.error(errorCause.get())
      READABLE_STREAM_CLOSED -> reader.close()
    }

    return reader
  }

  @Polyglot override fun cancel(reason: Any?): JsPromise<Unit> {
    return when (streamState.get()) {
      READABLE_STREAM_CLOSED -> JsPromise.resolved(Unit)
      READABLE_STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
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

  @Polyglot override fun tee(): Array<ReadableStream> {
    val reader = getReader()

    val reading = AtomicBoolean(false)
    val readAgain = AtomicBoolean(false)
    val canceled1 = AtomicBoolean(false)
    val canceled2 = AtomicBoolean(false)
    val reason1 = AtomicReference<Any?>(null)
    val reason2 = AtomicReference<Any?>(null)

    val branch1 = AtomicReference<ReadableDefaultStream>()
    val branch2 = AtomicReference<ReadableDefaultStream>()

    val cancelPromise = JsPromise<Unit>()

    fun pull(): JsPromise<Unit> {
      if (reading.getAndSet(true)) {
        readAgain.set(true)
        return JsPromise.resolved(Unit)
      }

      val readRequest = JsPromise<ReadResult>()
      readRequest.then(
        onFulfilled = {
          if (!it.done) {
            check(it.value != null)
            readAgain.set(false)

            if (!canceled1.get()) branch1.get().fulfillOrEnqueue(it.value)
            if (!canceled2.get()) branch2.get().fulfillOrEnqueue(it.value)

            reading.set(false)
            if (readAgain.get()) pull()
          } else {
            reading.set(false)
            if (!canceled1.get()) branch1.get().close()
            if (!canceled2.get()) branch2.get().close()
            if (!canceled1.get() || !canceled2.get()) cancelPromise.resolve(Unit)
          }
        },
        onCatch = {
          reading.set(false)
        },
      )

      readOrEnqueue(readRequest)
      return JsPromise.resolved(Unit)
    }

    fun cancel1(reason: Any?): JsPromise<Unit> {
      canceled1.set(true)
      reason1.set(reason)

      if (canceled2.get()) {
        val compositeReason = arrayOf(reason, reason2.get())
        cancel(compositeReason).then(
          onFulfilled = { cancelPromise.resolve(Unit) },
          onCatch = { cancelPromise.reject(it) },
        )
      }

      return cancelPromise
    }

    fun cancel2(reason: Any?): JsPromise<Unit> {
      canceled2.set(true)
      reason2.set(reason)

      if (canceled1.get()) {
        val compositeReason = arrayOf(reason1.get(), reason)
        cancel(compositeReason).then(
          onFulfilled = { cancelPromise.resolve(Unit) },
          onCatch = { cancelPromise.reject(it) },
        )
      }

      return cancelPromise
    }

    val source1 = object : ReadableStreamSource {
      override fun pull(controller: ReadableStreamController): JsPromise<Unit> = pull()
      override fun cancel(reason: Any?): JsPromise<Unit> = cancel1(reason)
    }

    val source2 = object : ReadableStreamSource {
      override fun pull(controller: ReadableStreamController): JsPromise<Unit> = pull()
      override fun cancel(reason: Any?): JsPromise<Unit> = cancel2(reason)
    }

    branch1.set(ReadableDefaultStream(source1, QueuingStrategy.DefaultReadStrategy, executor))
    branch2.set(ReadableDefaultStream(source2, QueuingStrategy.DefaultReadStrategy, executor))

    reader.closed.catch {
      branch1.get().error(it)
      branch2.get().error(it)
      if (!canceled1.get() || !canceled2.get()) cancelPromise.resolve(Unit)
    }

    return arrayOf(branch1.get(), branch2.get())
  }
}
