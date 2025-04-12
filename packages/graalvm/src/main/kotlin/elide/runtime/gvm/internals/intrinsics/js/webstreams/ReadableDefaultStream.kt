package elide.runtime.gvm.internals.intrinsics.js.webstreams

import com.google.common.util.concurrent.AtomicDouble
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.js.*
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.*
import elide.vm.annotations.Polyglot

/** Shorthand for a completable promise used to define read queues. */
private typealias DefaultReadRequest = CompletableJsPromise<ReadResult>

/**
 * Specialized implementation for a 'default' stream, i.e. a [ReadableStream] with a `ReadableStreamDefaultController`
 * and `ReadableStreamDefaultReader`.
 */
internal class ReadableDefaultStream internal constructor(
  override val source: ReadableStreamSource,
  override val strategy: QueueingStrategy = QueueingStrategy.Default,
) : AbstractReadableStream() {
  /** An arbitrary [chunk] value with an attached [size], as calculated by the stream's queueing strategy. */
  data class SizedChunk(val chunk: Any?, val size: Double)

  /**
   * [`ReadableStreamDefaultController`](https://streams.spec.whatwg.org/#rs-default-controller-class) spec
   * implementation, including all the exposed fields and methods in the spec.
   */
  private inner class ControllerImpl : ReadableStreamDefaultController {
    /** Atomic controller state flag. */
    private val controllerState = AtomicInteger(CONTROLLER_STARTED)

    override val desiredSize: Double?
      @Polyglot get() = when (streamState.get()) {
        STREAM_CLOSED -> 0.0
        STREAM_READABLE -> strategy.highWaterMark() - sourceChunksSize.get()
        else -> null
      }

    fun setup() {
      // set up the source
      runCatching { source.start(this) }
        .onFailure {
          error(it)
        }
        .onSuccess {
          controllerState.compareAndSet(CONTROLLER_UNINITIALIZED, CONTROLLER_STARTED)
          if (shouldPull()) pull()
        }
    }

    /** Whether the controller should request new chunks from the underlying [source]. */
    private fun shouldPull(): Boolean {
      val state = controllerState.get()
      return when {
        state == CONTROLLER_CLOSING || streamState.get() != STREAM_READABLE -> false
        state == CONTROLLER_UNINITIALIZED -> false
        locked && readQueue.isNotEmpty() -> true
        (desiredSize ?: 0.0) > 0.0 -> true
        else -> false
      }
    }

    /** Cancel the controller and the underlying source. Called by the stream during [ReadableDefaultStream.cancel]. */
    fun cancel(reason: Any? = null): JsPromise<Unit> {
      sourceChunks.clear()
      sourceChunksSize.set(0.0)
      return source.cancel(reason)
    }

    @Polyglot override fun close() {
      if (controllerState.getAndSet(CONTROLLER_CLOSING) == CONTROLLER_CLOSING) return
      // don't close the stream if there are undelivered elements
      if (sourceChunks.isEmpty()) this@ReadableDefaultStream.close()
    }

    @Polyglot override fun error(reason: Any?) {
      if (streamState.get() != STREAM_READABLE) return
      sourceChunks.clear()
      sourceChunksSize.set(0.0)
      this@ReadableDefaultStream.error(reason)
    }

    @Polyglot override fun enqueue(chunk: Any?) {
      // check the controller isn't closing or closed
      if (controllerState.get() == CONTROLLER_CLOSING || streamState.get() != STREAM_READABLE) {
        throw TypeError.create("Controller is closing or stream is not readable")
      }

      // consume queued reads if possible
      readQueue.poll()?.let {
        it.resolve(ReadResult(chunk, false))
        return
      }

      try {
        // enqueue the sized unread chunk
        val size = strategy.size(chunk)
        sourceChunks.add(SizedChunk(chunk, size))
        sourceChunksSize.addAndGet(size)
        if (shouldPull()) pull()
      } catch (cause: Throwable) {
        error(cause)
      }
    }

    /** Pull from the underlying source if the backpressure controls allow it. */
    fun pullIfNeeded() {
      if (shouldPull()) pull()
    }

    /**
     * Pull from the underlying source to obtain new chunks. If a pull is already in progress, a 'pull again' flag is
     * set to trigger a new pull after the current one.
     */
    fun pull() {
      // if pulling, flag for retry, the queued read will be fulfilled
      if (controllerState.compareAndSet(CONTROLLER_PULLING, CONTROLLER_PULL_AGAIN)) return

      // if idle, flag as pulling and pull from the source
      if (controllerState.compareAndSet(CONTROLLER_STARTED, CONTROLLER_PULLING)) {
        source.pull(this).then(
          onFulfilled = {
            if (controllerState.compareAndSet(CONTROLLER_PULL_AGAIN, CONTROLLER_PULLING)) pullIfNeeded()
            else controllerState.compareAndSet(CONTROLLER_PULLING, CONTROLLER_STARTED)
          },
          onCatch = ::error,
        )
        return
      }
    }

    /**
     * Poll the inbound queue for available chunks, returning `null` if the queue is empty.
     *
     * If the controller is currently closing and the queue is emptied as a result of this operation, the controller
     * stream will be closed.
     */
    fun poll(): SizedChunk? {
      val chunk = sourceChunks.poll()?.also { sourceChunksSize.addAndGet(-it.size) }

      // finish closing if we removed the last chunk from the queue
      if (sourceChunks.isEmpty() && controllerState.compareAndSet(CONTROLLER_CLOSING, CONTROLLER_CLOSED))
        this@ReadableDefaultStream.close()

      return chunk
    }
  }

  /**
   * [`ReadableStreamDefaultReader`](https://streams.spec.whatwg.org/#default-reader-class) spec
   * implementation, including all the exposed fields and methods in the spec.
   */
  private inner class ReadableStreamDefaultReaderImpl : ReadableStreamDefaultReader {
    @Polyglot override val closed: CompletableJsPromise<Unit> = JsPromise()

    fun setup() {
      // early closure
      when (streamState.get()) {
        STREAM_CLOSED -> closed.resolve(Unit)
        STREAM_ERRORED -> closed.reject(errorCause.get())
        else -> Unit
      }
    }

    @Polyglot override fun read(): JsPromise<ReadResult> {
      if (reader.get() !== this) throw TypeError.create("Reader is detached")
      return when (streamState.get()) {
        STREAM_READABLE -> {
          // consume queued chunks if available; if the stream was closed after polling, mark as 'done'
          controller.poll()?.let {
            return JsPromise.resolved(ReadResult(it.chunk, streamState.get() != STREAM_READABLE))
          }

          // enqueue read request and pull
          JsPromise<ReadResult>().also { request ->
            readQueue.add(request)
            controller.pullIfNeeded()
          }
        }

        STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
        else -> JsPromise.resolved(ReadResult(null, true))
      }
    }

    @Polyglot override fun releaseLock() {
      if (closed.isDone) throw TypeError.create("Reader has already been released")
      closed.reject(TypeError.create("Reader lock was released"))
      reader.set(null)
    }

    @Polyglot override fun cancel() {
      this@ReadableDefaultStream.cancel()
    }
  }

  /**
   * Chunks are pushed by the source using the controller, and pulled by consumers using the reader; the queue can be
   * skipped if there are pending reads when a new chunk is pushed.
   */
  private val sourceChunks: ConcurrentLinkedQueue<SizedChunk> = ConcurrentLinkedQueue()

  /** Total size of the chunks in [sourceChunks], as calculated by the stream's queueing [strategy]. */
  private val sourceChunksSize = AtomicDouble(0.0)

  /**
   * Reads are enqueued by consumers using the reader, and fulfilled by the source by pushing chunks with the
   * controller; the queue can be skipped if there are queued chunks when a read is requested.
   */
  private val readQueue: ConcurrentLinkedQueue<DefaultReadRequest> = ConcurrentLinkedQueue()

  /** Stored cause for the stream's failure. */
  private val errorCause = AtomicReference<Any>()

  /**
   * Handle used by consumers to read chunks; if not `null`, the stream is considered 'locked' and must be released
   * before a new reader can be acquired.
   */
  private val reader = AtomicReference<ReadableStreamDefaultReaderImpl>()

  /** Handle used by the [source] to push new chunks and control the stream. */
  private val controller = ControllerImpl().also { it.setup() }

  @get:Polyglot override val locked: Boolean get() = reader.get() != null

  /** Close the stream, preventing new chunks from being enqueued by the source or read by consumers. */
  private fun close() {
    if (!streamState.compareAndSet(STREAM_READABLE, STREAM_CLOSED)) throw TypeError.create("Stream is not readable")
    reader.getAndSet(null)?.closed?.resolve(Unit) ?: return

    // close unfulfilled requests
    while (true) readQueue.poll()?.resolve(ReadResult(null, true)) ?: break
  }

  /**
   * Cancel the stream with an error; if locked, the reader will also be closed with the same cause. All pending read
   * requests will be rejected with [reason].
   */
  private fun error(reason: Any? = null) {
    if (!streamState.compareAndSet(STREAM_READABLE, STREAM_ERRORED)) throw TypeError.create("Stream is not readable")
    errorCause.set(reason)

    reader.get()?.closed?.reject(reason) ?: return
    while (true) readQueue.poll()?.reject(reason) ?: break
  }

  @Polyglot override fun cancel(reason: Any?): JsPromise<Unit> {
    return when (streamState.get()) {
      STREAM_CLOSED -> JsPromise.resolved(Unit)
      STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
      else -> {
        close()
        controller.cancel(reason)
      }
    }
  }

  @Polyglot override fun getReader(options: Any?): ReadableStreamReader {
    if (reader.getAndUpdate { current ->
        current ?: ReadableStreamDefaultReaderImpl().also { it.setup() }
      } != null) throw TypeError.create("Stream is locked")
    return reader.get()
  }

  override fun pipeThrough(transform: TransformStream, options: Any?): ReadableStream = TODO("Not yet implemented")
  override fun pipeTo(destination: WritableStream, options: Any?): JsPromise<Unit> = TODO("Not yet implemented")
  override fun tee(): Array<ReadableStream> = TODO("Not yet implemented")
}
