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
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortController
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.QueueElement.Chunk
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.QueueElement.CloseToken
import elide.runtime.intrinsics.js.AbortSignal
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultController
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultWriter
import elide.runtime.intrinsics.js.stream.WritableStreamSink
import elide.vm.annotations.Polyglot

/**
 * Implementation of the default [WritableStream] WHATWG spec. This class merges most operations described in the spec,
 * including those performed by the controller and writer.
 */
internal class WritableDefaultStream(
  /** Underlying sink this stream writes to. */
  private val sink: WritableStreamSink,
  /** Queueing strategy used to control backpressure. */
  private val strategy: QueueingStrategy = QueueingStrategy.DefaultWriteStrategy,
) : WritableStream {
  /** Inline wrapper for the stream providing the controller API by delegation. */
  @JvmInline private value class ControllerToken(val stream: WritableDefaultStream) : WritableStreamDefaultController {
    override val signal: AbortSignal
      get() = stream.abortController.signal

    override fun error(e: Any?) {
      if (stream.state.get() != WRITABLE_STREAM_WRITABLE) return
      stream.startErroring(e)
    }
  }

  /**
   * Represents an element sent through the [writeQueue]; this can be either a data [Chunk], which is a sized value,
   * or a [CloseToken], which signals the closing of the stream.
   */
  private sealed interface QueueElement {
    /** A sized data chunk that should be written to the underlying [sink]. */
    data class Chunk(val chunk: Value, val size: Double) : QueueElement

    /** A sentinel value used to indicate the end of the stream; no more values will be enqueued after this token. */
    data object CloseToken : QueueElement
  }

  /**
   * A request to abort the stream, placed asynchronously. The [promise] should be resolved once the abort operation is
   * complete. The [alreadyErroring] flag indicates whether the stream was already in the process of failing when it
   * was aborted.
   */
  private data class AbortRequest(
    val promise: CompletableJsPromise<Unit>,
    val reason: Any?,
    val alreadyErroring: Boolean,
  )

  /** Abort controller that can be used to abort the current ongoing write operation. */
  private val abortController = AbortController()

  /**
   * Reference to an in-flight write operation, set by [markFirstWriteRequestInFlight], and unset, then completed by
   * [finishInFlightWrite] or [finishInFlightWriteWithError].
   */
  private val pendingWrite = AtomicReference<CompletableJsPromise<Unit>>()

  /**
   * Reference to an in-flight stream close operation, set by [markCloseRequestInFlight], and unset, then completed by
   * [finishInFlightClose] or [finishInFlightCloseWithError].
   */
  private val pendingClose = AtomicReference<CompletableJsPromise<Unit>>()

  /**
   * A pending request to abort the stream, used to asynchronously instruct the stream to abort; the promise in the
   * request is resolved once the abort operation is complete.
   */
  private val pendingAbort = AtomicReference<AbortRequest>()

  /**
   * Whether the stream is currently applying backpressure. This value is monitored by producers through the
   * [lockedWriter]'s [ready][WritableStreamDefaultWriter.ready] promise.
   */
  private val backpressureApplied = AtomicBoolean(false)

  /** Whether the underlying [sink] has been initialized and is ready to write. */
  private val initialized = AtomicBoolean(false)

  /** Current state of the stream. */
  private val state = AtomicInteger(WRITABLE_STREAM_WRITABLE)
  internal val streamState: Int get() = state.get()

  /** An optional stored cause for the stream's error state. */
  private val storedError = AtomicReference<Any?>(null)
  internal val errorCause: Any? get() = storedError.get()

  /** A queue holding the promises corresponding to pending write operations in the [writeQueue]. */
  private val writeRequests = ConcurrentLinkedQueue<CompletableJsPromise<Unit>>()

  /**
   * A queue holding the chunks of data to be written to the underlying [sink]. When the stream is terminated (by
   * closing, erroring, or aborting), a special [close token][QueueElement.CloseToken] is added to this queue,
   * indicating that no more chunks will be added.
   */
  private val writeQueue = ConcurrentLinkedQueue<QueueElement>()

  /** The total size of the elements in the [writeQueue], calculated using the stream's queueing [strategy]. */
  private val writeQueueSize = AtomicDouble(0.0)

  /**
   * A promise representing a stream close operation, monitored by producers that request closure through the
   * [lockedWriter].
   */
  private val closeRequest = AtomicReference<CompletableJsPromise<Unit>>()

  /** A [WritableStreamDefaultController] instance used by the [sink] to interface with the stream. */
  private val controller = ControllerToken(this)

  /** Reference to a writer currently locked to this stream, if any. */
  private val lockedWriter = AtomicReference<WritableStreamDefaultWriterToken>()

  /** Compute whether the stream currently needs to apply backpressure, according to its [desiredSize]. */
  private val needsBackpressure: Boolean get() = desiredSize()?.let { it <= 0 } == true

  /** Whether there is an ongoing [write][pendingWrite] or [close][pendingClose] operation. */
  internal val pendingInflight get() = pendingWrite.get() != null || pendingClose.get() != null

  /** Whether the stream is currently closing or closure is queued. */
  internal val closeQueuedOrInflight get() = closeRequest.get() != null || pendingClose.get() != null

  @get:Polyglot override val locked: Boolean get() = lockedWriter.get() != null

  init {
    setupSink()
  }

  /** Initialize the underlying [sink] of the stream, using the [controller] to expose its driving APIs. */
  private fun setupSink() {
    runCatching { sink.start(controller) }
      .onFailure {
        initialized.set(true)
        dealWithRejection(it)
      }
      .onSuccess {
        initialized.set(true)
        advanceQueueIfNeeded()
      }
  }

  /** Compute the desired [writeQueueSize] for the stream, depending on its [state] and [strategy]. */
  internal fun desiredSize(): Double? = when (state.get()) {
    WRITABLE_STREAM_ERRORED, WRITABLE_STREAM_ERRORING -> null
    WRITABLE_STREAM_CLOSED -> 0.0
    else -> strategy.highWaterMark() - writeQueueSize.get()
  }

  /**
   * Write a [chunk] of data to the underlying sink. The write operation is enqueued and a promise encapsulating it is
   * returned. Depending on the current state of the stream, this method may throw a [TypeError] or return a rejected
   * promise.
   *
   * This method is intended for use by [WritableStreamDefaultWriter] implementations when a producer requests to write
   * a chunk.
   */
  internal fun writeChunk(chunk: Value): JsPromise<Unit> {
    val size = strategy.size(chunk)
    val currentState = state.get()

    return when {
      currentState == WRITABLE_STREAM_ERRORED -> JsPromise.rejected(storedError.get())
      currentState == WRITABLE_STREAM_CLOSED || closeQueuedOrInflight -> {
        JsPromise.rejected(TypeError.create("Stream is closing or closed"))
      }

      currentState == WRITABLE_STREAM_ERRORING -> JsPromise.rejected(storedError.get())

      else -> {
        val promise = JsPromise<Unit>()

        writeRequests.offer(promise)
        commitWrite(chunk, size)

        promise
      }
    }
  }

  internal fun errorIfNeeded(cause: Any?) {
    if (state.get() != WRITABLE_STREAM_WRITABLE) return
    startErroring(cause)
  }

  /** Enqueue an [element] to the [writeQueue] and increment the [writeQueueSize] accordingly. */
  private fun enqueueWithSize(element: QueueElement) {
    if (element is Chunk) writeQueueSize.addAndGet(element.size)
    writeQueue.offer(element)
  }

  /** Retrieve and remove the element at the head of the [writeQueue] and decrease the [writeQueueSize] accordingly. */
  private fun dequeue(): QueueElement {
    return writeQueue.poll().also { if (it is Chunk) writeQueueSize.addAndGet(-it.size) }
  }

  /**
   * Finalize an immediate [write][writeChunk] operation, enqueuing the [chunk] with the given [size], updating the
   * stream's backpressure, and advancing the write queue.
   */
  private fun commitWrite(chunk: Value, size: Double) {
    enqueueWithSize(Chunk(chunk, size))
    if (!closeQueuedOrInflight && state.get() == WRITABLE_STREAM_WRITABLE) updateBackpressure(needsBackpressure)
    advanceQueueIfNeeded()
  }

  /**
   * Process the next entry in the [writeQueue] if applicable. If the stream is currently closing and the head element
   * in the queue is a [CloseToken], the stream will finalize closing.
   */
  private fun advanceQueueIfNeeded() {
    if (!initialized.get() || pendingWrite.get() != null) return

    if (state.get() == WRITABLE_STREAM_ERRORING) {
      finishErroring()
      return
    }

    if (writeQueue.isEmpty()) return

    when (val element = writeQueue.peek()) {
      // stream is closing, and we've reached the end of the queue, we can clean up
      is CloseToken -> {
        markCloseRequestInFlight()

        dequeue()
        check(writeQueue.isEmpty())

        sink.close().then(
          onFulfilled = { finishInFlightClose() },
          onCatch = ::finishInFlightCloseWithError,
        )
      }

      // stream is writable (or erroring), and we have a chunk enqueued, write to the sink
      is Chunk -> {
        markFirstWriteRequestInFlight()
        sink.write(element.chunk, controller).then(
          onFulfilled = {
            finishInFlightWrite()

            val currentState = state.get()
            check(currentState == WRITABLE_STREAM_WRITABLE || currentState == WRITABLE_STREAM_ERRORING)

            dequeue()
            if (!closeQueuedOrInflight && currentState == WRITABLE_STREAM_WRITABLE)
              updateBackpressure(needsBackpressure)

            advanceQueueIfNeeded()
          },
          onCatch = ::finishInFlightWriteWithError,
        )
      }
    }
  }

  /** Set the [closeRequest] as the [pendingClose] operation, indicating that the stream has started closing. */
  private fun markCloseRequestInFlight() {
    val request = checkNotNull(closeRequest.getAndSet(null))
    check(pendingClose.compareAndSet(null, request))
  }

  /** Resolve and clear the current [pendingClose] promise, and clean up the stream's state to finalize closure. */
  private fun finishInFlightClose() {
    checkNotNull(pendingClose.getAndSet(null)).resolve(Unit)

    val currentState = state.get()
    if (currentState == WRITABLE_STREAM_ERRORING) {
      storedError.set(null)
      pendingAbort.getAndSet(null)?.promise?.resolve(Unit)
    }

    state.set(WRITABLE_STREAM_CLOSED)
    lockedWriter.get()?.closed?.resolve(Unit)

    check(pendingAbort.get() == null)
    check(storedError.get() == null)
  }

  /** Finalize an ongoing closure of the stream with an error, rather than cleanly closing. */
  private fun finishInFlightCloseWithError(reason: Any?) {
    checkNotNull(pendingClose.getAndSet(null)).reject(reason)

    pendingAbort.getAndSet(null)?.promise?.reject(reason)
    dealWithRejection(reason)
  }

  /** Remove the head of the [writeRequests] queue and set it as the [pendingWrite]. */
  private fun markFirstWriteRequestInFlight() {
    val request = checkNotNull(writeRequests.poll())
    check(pendingWrite.compareAndSet(null, request))
  }

  /** Resolve and clear the current [pendingWrite] promise. */
  private fun finishInFlightWrite() {
    checkNotNull(pendingWrite.getAndSet(null)).resolve(Unit)
  }

  /** Reject and clear the current [pendingWrite] promise, and handle the error state. */
  private fun finishInFlightWriteWithError(reason: Any?) {
    checkNotNull(pendingWrite.getAndSet(null)).reject(reason)
    dealWithRejection(reason)
  }

  /**
   * Update the stream's backpressure flag to the given [value], adjusting the [lockedWriter]'s ready promise
   * accordingly.
   */
  private fun updateBackpressure(value: Boolean) {
    check(state.get() == WRITABLE_STREAM_WRITABLE)
    check(!closeQueuedOrInflight)

    lockedWriter.get()?.takeIf { value != backpressureApplied.get() }?.let {
      if (value) it.refreshReadyPromise()
      else it.ready.resolve(Unit)
    }

    backpressureApplied.set(value)
  }

  /** Handle a failure during a stream operation, and either [startErroring] or [finishErroring]. */
  private fun dealWithRejection(reason: Any?) {
    val currentState = state.get()
    if (currentState == WRITABLE_STREAM_WRITABLE) {
      startErroring(reason)
      return
    }

    check(currentState == WRITABLE_STREAM_ERRORING)
    finishErroring()
  }

  /** Begin closing the stream with an error state using the given [reason]. */
  private fun startErroring(reason: Any?) {
    check(state.compareAndSet(WRITABLE_STREAM_WRITABLE, WRITABLE_STREAM_ERRORING))
    check(storedError.compareAndSet(null, reason))

    lockedWriter.get()?.ensureReadyPromiseRejected(reason)
    if (!pendingInflight && initialized.get()) finishErroring()
  }

  /** Finalize a stream closure caused by an error. */
  private fun finishErroring() {
    check(state.compareAndSet(WRITABLE_STREAM_ERRORING, WRITABLE_STREAM_ERRORED))
    check(!pendingInflight)

    writeQueueSize.set(0.0)
    writeQueue.clear()

    // clear pending requests
    val reason = storedError.get()
    while (writeRequests.isNotEmpty()) writeRequests.poll().reject(reason)

    val abort = pendingAbort.getAndSet(null)
    if (abort == null) {
      rejectCloseAndClosedPromise()
      return
    }

    if (abort.alreadyErroring) {
      abort.promise.reject(reason)
      rejectCloseAndClosedPromise()
      return
    }

    sink.abort(abort.reason).then(
      onFulfilled = {
        abort.promise.resolve(Unit)
        rejectCloseAndClosedPromise()
      },
      onCatch = {
        abort.promise.reject(it)
        rejectCloseAndClosedPromise()
      },
    )
  }

  /**
   * Reject the stream's [closeRequest] and the [lockedWriter]'s [WritableStreamDefaultWriter.closed] promises with
   * the stored error, if applicable.
   */
  private fun rejectCloseAndClosedPromise() {
    val reason = storedError.get()

    closeRequest.getAndSet(null)?.let { request ->
      check(pendingClose.get() == null)
      request.reject(reason)
    }

    lockedWriter.get()?.closed?.reject(reason)
  }

  /** Abruptly close the stream using an error with the given [reason]. */
  @Suppress("ReturnCount")
  internal fun abortStream(reason: Any?): JsPromise<Unit> {
    var currentState = state.get()
    if (currentState == WRITABLE_STREAM_CLOSED || currentState == WRITABLE_STREAM_ERRORED) return JsPromise.resolved(
      Unit,
    )

    abortController.abort(reason)

    // note: the condition is not guaranteed since aborting the controller may result in a state change
    // see https://streams.spec.whatwg.org/#writable-stream-abort
    currentState = state.get()
    if (currentState == WRITABLE_STREAM_CLOSED || currentState == WRITABLE_STREAM_ERRORED) return JsPromise.resolved(
      Unit,
    )

    pendingAbort.get()?.let {
      return it.promise
    }

    val finalReason = if (currentState != WRITABLE_STREAM_ERRORING) reason else null
    val alreadyErroring = currentState == WRITABLE_STREAM_ERRORING

    val abortPromise = JsPromise<Unit>()
    pendingAbort.set(
      AbortRequest(
        promise = abortPromise,
        alreadyErroring = alreadyErroring,
        reason = finalReason,
      ),
    )

    if (!alreadyErroring) startErroring(finalReason)
    return abortPromise
  }

  /** Close the stream normally if applicable. */
  internal fun closeStream(): JsPromise<Unit> {
    val currentState = state.get()
    if (currentState == WRITABLE_STREAM_CLOSED || currentState == WRITABLE_STREAM_ERRORED)
      return JsPromise.rejected(TypeError.create("Stream is already closed or errored"))

    check(!closeQueuedOrInflight)

    val closePromise = JsPromise<Unit>()
    closeRequest.set(closePromise)

    lockedWriter.get()
      ?.takeIf { backpressureApplied.get() && currentState == WRITABLE_STREAM_WRITABLE }?.ready?.resolve(Unit)

    // signal closure via the queue
    enqueueWithSize(CloseToken)
    advanceQueueIfNeeded()

    return closePromise
  }

  /** Clear and release the current [lockedWriter]. */
  internal fun releaseWriter(writer: WritableStreamDefaultWriterToken) {
    check(lockedWriter.compareAndSet(writer, null))
    val reason = TypeError.create("Writer has been released")
    writer.ensureReadyPromiseRejected(reason)
    writer.ensureClosedPromiseRejected(reason)
  }

  @Polyglot override fun getWriter(): WritableStreamDefaultWriter {
    val writer = WritableStreamDefaultWriterToken(this)

    if (!lockedWriter.compareAndSet(null, writer))
      throw TypeError.create("Stream is already locked to a writer")

    when (state.get()) {
      WRITABLE_STREAM_WRITABLE -> {
        if (!closeQueuedOrInflight && backpressureApplied.get()) writer.refreshReadyPromise()
        else writer.refreshClosePromise(JsPromise<Unit>().also { it.resolve(Unit) })

        writer.refreshClosePromise()
      }

      WRITABLE_STREAM_CLOSED -> {
        writer.refreshReadyPromise(JsPromise<Unit>().also { it.resolve(Unit) })
        writer.refreshClosePromise(JsPromise<Unit>().also { it.resolve(Unit) })
      }

      WRITABLE_STREAM_ERRORING -> {
        writer.refreshReadyPromise(JsPromise<Unit>().also { it.reject(storedError.get()) })
        writer.refreshClosePromise()
      }

      else -> {
        val reason = storedError.get()
        writer.refreshReadyPromise(JsPromise<Unit>().also { it.reject(reason) })
        writer.refreshClosePromise(JsPromise<Unit>().also { it.reject(reason) })
      }
    }

    return writer
  }

  @Polyglot override fun abort(reason: Any?): JsPromise<Unit> {
    return if (locked) JsPromise.rejected(TypeError.create("Cannot abort a locked stream"))
    else abortStream(reason)
  }

  @Polyglot override fun close(): JsPromise<Unit> {
    return if (locked) JsPromise.rejected(TypeError.create("Cannot close a locked stream"))
    else if (closeQueuedOrInflight) JsPromise.rejected(TypeError.create("Stream is already closing or queued"))
    else closeStream()
  }

  internal companion object {
    internal const val WRITABLE_STREAM_WRITABLE: Int = 0
    internal const val WRITABLE_STREAM_CLOSED: Int = 1
    internal const val WRITABLE_STREAM_ERRORING: Int = 2
    internal const val WRITABLE_STREAM_ERRORED: Int = 3
  }
}
