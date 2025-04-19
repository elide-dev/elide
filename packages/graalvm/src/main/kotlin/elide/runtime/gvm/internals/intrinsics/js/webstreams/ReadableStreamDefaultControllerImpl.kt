package elide.runtime.gvm.internals.intrinsics.js.webstreams

import com.google.common.util.concurrent.AtomicDouble
import java.util.concurrent.ConcurrentLinkedQueue
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.runtime.intrinsics.js.stream.ReadableStreamSource
import elide.vm.annotations.Polyglot

/** Implementation of the default readable stream controller class. */
internal class ReadableStreamDefaultControllerImpl(
  private val stream: ReadableStreamImpl,
  private val source: ReadableStreamSource,
  private val strategy: QueueingStrategy,
) : ReadableStreamControllerBase(), ReadableStreamDefaultController {
  /** Queue used for chunks provided by the source that cannot be delivered immediately. */
  private val queue = ConcurrentLinkedQueue<SizedChunk>()

  /** Total size of all chunks currently in the controller's queue. */
  private val queueSize = AtomicDouble(0.0)

  init {
    // set up the source
    runCatching { source.start(this) }
      .onFailure { error(it) }
      .onSuccess {
        mutableState.compareAndSet(CONTROLLER_UNINITIALIZED, CONTROLLER_STARTED)
        if (shouldPull()) pull()
      }
  }

  override val desiredSize: Double?
    @Polyglot get() = when (stream.state) {
      ReadableStreamImpl.Companion.STREAM_READABLE -> strategy.highWaterMark() - queueSize.get()
      ReadableStreamImpl.Companion.STREAM_CLOSED -> 0.0
      else -> null
    }

  /** Whether the controller should request new chunks from the underlying [source]. */
  private fun shouldPull(): Boolean {
    val state = mutableState.get()
    return when {
      state == CONTROLLER_CLOSING || stream.state != ReadableStreamImpl.Companion.STREAM_READABLE -> false
      state == CONTROLLER_UNINITIALIZED -> false
      stream.locked && queue.isNotEmpty() -> true
      (desiredSize ?: 0.0) > 0.0 -> true
      else -> false
    }
  }

  /**
   * Pull from the underlying source, or flag for retry if already pulling. This function only has an effect if the
   * current [state] is 'started' or 'pulling'.
   */
  private fun pull() {
    // if pulling, flag for retry, the queued read will be fulfilled
    if (mutableState.compareAndSet(CONTROLLER_PULLING, CONTROLLER_PULL_AGAIN)) return

    // if idle, flag as pulling and pull from the source
    if (mutableState.compareAndSet(CONTROLLER_STARTED, CONTROLLER_PULLING)) {
      source.pull(this).then(
        onFulfilled = {
          if (mutableState.compareAndSet(CONTROLLER_PULL_AGAIN, CONTROLLER_PULLING)) pullIfNeeded()
          else mutableState.compareAndSet(CONTROLLER_PULLING, CONTROLLER_STARTED)
        },
        onCatch = ::error,
      )
      return
    }
  }

  /** If the controller [shouldPull], [pull] from the underlying source. */
  internal fun pullIfNeeded(): Boolean = shouldPull().also { if (it) pull() }

  /**
   * Poll the inbound queue for available chunks, returning `null` if the queue is empty.
   *
   * If the controller is currently closing and the queue is emptied as a result of this operation, the controller
   * stream will be closed.
   */
  internal fun poll(): SizedChunk? {
    val chunk = queue.poll() ?: return null
    queueSize.addAndGet(-chunk.size)

    // finish closing if we removed the last chunk from the queue
    if (queue.isEmpty() && mutableState.compareAndSet(CONTROLLER_CLOSING, CONTROLLER_CLOSED))
      stream.close()

    return chunk
  }

  @Polyglot override fun close() {
    if (mutableState.getAndSet(CONTROLLER_CLOSING) == CONTROLLER_CLOSING) return
    // don't close the stream if there are undelivered elements
    if (queue.isEmpty()) stream.close()
  }

  @Polyglot override fun error(reason: Any?) {
    if (stream.state != ReadableStreamImpl.Companion.STREAM_READABLE) return
    queue.clear()
    queueSize.set(0.0)
    stream.error(reason)
  }

  @Polyglot override fun enqueue(chunk: Any?) {
    // check the controller isn't closing or closed
    if (state == CONTROLLER_CLOSING || stream.state != ReadableStreamImpl.Companion.STREAM_READABLE) {
      throw TypeError.Companion.create("Controller is closing or stream is not readable")
    }

    // consume queued reads if possible
    stream.reader.assertDefault().poll()?.let {
      it.resolve(ReadableStream.ReadResult(chunk, false))
      return
    }

    try {
      // enqueue the sized unread chunk
      val size = strategy.size(chunk)
      queue.add(SizedChunk(chunk, size))
      queueSize.addAndGet(size)
      pullIfNeeded()
    } catch (cause: Throwable) {
      error(cause)
    }
  }

  /** Cancel the controller and the underlying source. Called when the stream is cancelled. */
  @Polyglot override fun cancel(reason: Any?): JsPromise<Unit> {
    queue.clear()
    queueSize.set(0.0)
    return source.cancel(reason)
  }
}
