package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamSource

/**
 * Base class for [ReadableStream] implementations, containing shared definitions for internal queue types as well as
 * methods used by other components. This contract allows readers and controllers to be decoupled from the internal
 * state of the stream itself.
 */
internal abstract class ReadableStreamBase : ReadableStream {
  /**
   * A reentrant lock used to synchronize update or transfer operations between the stream's internal queues, as well
   * as changes to the queue size.
   *
   * It is important that *all* write operations related to the queues are performed while holding the lock, since
   * there is no other method to guarantee the atomicity of operations that mutate both collections.
   */
  protected val queueLock = ReentrantLock()

  /**
   * Pending reads queue, used to store unfulfilled read requests from the stream's [reader]. When a read is requested
   * and there is no buffered data to complete it immediately, a promise encapsulating the request will be added to
   * this queue.
   *
   * Modifying this queue is strictly forbidden unless the [queueLock] is held by the current thread. This is
   * required to avoid race conditions when transferring a chunk between queues.
   */
  protected val readQueue = LinkedList<CompletableJsPromise<ReadResult>>()

  /**
   * A reference to the current (if any) reader this stream is locked to. Default streams only support 'default'
   * readers.
   */
  protected open val lockedReader = AtomicReference<ReadableStreamReaderTokenBase>()

  /** Current state of the stream, safe for concurrent access and modification. */
  protected val streamState = AtomicInteger(STREAM_READABLE)

  /** Current state of the stream's source, semantically equivalent to the WHATWG spec controller's state. */
  protected val sourceState = AtomicInteger(SOURCE_UNINITIALIZED)

  /** Optional reason for the stream's errored state. */
  protected val errorCause = AtomicReference<Any>()

  /**
   * Initialize the underlying [source] of the stream, using a [controller] to expose its driving APIs. Subclasses
   * should always call this method exactly once immediately after construction.
   */
  protected fun setupSource(source: ReadableStreamSource, controller: ReadableStreamController) {
    try {
      source.start(controller)
      sourceState.compareAndSet(SOURCE_UNINITIALIZED, SOURCE_READY)
      maybePull()
    } catch (reason: Throwable) {
      error(reason)
    }
  }

  /** Whether the stream should pull from the underlying source, given its current state and the size of the queue. */
  protected fun shouldPull(): Boolean {
    val streamState = streamState.get()
    val sourceState = sourceState.get()
    val reader = lockedReader.get()

    return when {
      streamState != STREAM_READABLE -> false
      sourceState >= SOURCE_CLOSING -> false
      sourceState <= SOURCE_UNINITIALIZED -> false
      reader != null && readQueue.isNotEmpty() -> true
      checkNotNull(desiredSize()) > 0 -> true
      else -> false
    }
  }

  protected fun pull(source: ReadableStreamSource, controller: ReadableStreamController) {
    // if pulling, flag for retry; if idle, flag as pulling and pull from the source
    if (sourceState.compareAndSet(SOURCE_PULLING, SOURCE_PULL_AGAIN)) return
    if (!sourceState.compareAndSet(SOURCE_READY, SOURCE_PULLING)) return

    source.pull(controller).then(
      onFulfilled = {
        if (sourceState.compareAndSet(SOURCE_PULL_AGAIN, SOURCE_PULLING)) maybePull()
        else sourceState.compareAndSet(SOURCE_PULLING, SOURCE_READY)
      },
      onCatch = ::error,
    )
  }

  /** Pull from the underlying source if needed. */
  protected abstract fun maybePull()

  /** Return the desired size for the stream's internal queue, or `null` if no further data will be required. */
  internal abstract fun desiredSize(): Double?

  /** Fulfill a new read from available chunks, or enqueue a new read request and return the promise handle for it. */
  internal abstract fun readOrEnqueue(): JsPromise<ReadResult>

  /** Fulfill a pending read using a chunk, or add the chunk to the queue for future use. */
  internal abstract fun fulfillOrEnqueue(chunk: Value)

  /**
   * Close the stream in response to an error, with an optional reason. This will discard all undelivered chunks and
   * reject pending read requests.
   */
  internal abstract fun error(reason: Any? = null)

  /**
   * Close the stream normally, preventing further reads or chunks from being enqueued. The stream may not close
   * immediately, for example, if there are undelivered chunks, the stream will be marked as "closing" to prevent new
   * reads but full closure will be deferred until the queue is empty.
   */
  internal abstract fun close()

  /** Release the current reader, if any, allowing a new reader to be locked to the stream. */
  internal abstract fun release()

  internal companion object {
    // stream state
    internal const val STREAM_READABLE = 0
    internal const val STREAM_CLOSED = 1
    internal const val STREAM_ERRORED = 2

    // source (controller) state
    internal const val SOURCE_UNINITIALIZED = 0
    internal const val SOURCE_READY = 1
    internal const val SOURCE_PULLING = 2
    internal const val SOURCE_PULL_AGAIN = 3
    internal const val SOURCE_CLOSING = 4
    internal const val SOURCE_CLOSED = 5
  }
}

