package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_ERRORED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_READABLE
import elide.runtime.intrinsics.js.*
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamReader
import elide.runtime.intrinsics.js.stream.ReadableStreamSource

/**
 * Implementation of a [ReadableStream] as defined by the [WHATWG Web Streams spec](https://streams.spec.whatwg.org).
 *
 * This class supports all known types of streams (default and byte streams), with its controller type being selected
 * according to the type of the [source].
 */
internal class ReadableStreamImpl(source: ReadableStreamSource, strategy: QueueingStrategy) : ReadableStream {
  /**
   * A snapshot of the current state of the stream, matching one of the well-known constants: [STREAM_READABLE],
   * [STREAM_CLOSED], or [STREAM_ERRORED].
   */
  internal val state: Int get() = mutableState.get()
  private val mutableState = AtomicInteger(STREAM_READABLE)

  /** The reader this stream is currently locked to, if any. */
  internal val reader: ReadableStreamReader get() = mutableReader.get()
  private val mutableReader = AtomicReference<ReadableStreamReader>(null)

  override val locked: Boolean get() = mutableReader.get() != null

  /** The controller used by the stream's [source] to manage the stream and push data. */
  internal val controller: ReadableStreamController = when (source.type) {
    ReadableStream.Type.Default -> ReadableStreamDefaultControllerImpl(this, source, strategy)
    ReadableStream.Type.BYOB -> TODO("Byte streams are not supported yet")
  }

  /** Mutable cause for the stream's failure, should only be set when [state] is [STREAM_ERRORED]. */
  private val errorCause = AtomicReference<Any>()

  /** The cause for the stream's current error state, if [state] is [STREAM_ERRORED], or `null` otherwise. */
  internal val error: Any? get() = errorCause.get()

  /** Close the stream, preventing new chunks from being enqueued by the source or read by consumers. */
  internal fun close() {
    if (!mutableState.compareAndSet(STREAM_READABLE, STREAM_CLOSED))
      throw TypeError.create("Stream is not readable")

    // close unfulfilled requests
    when (val oldReader = mutableReader.getAndSet(null)) {
      is ReadableStreamDefaultReaderImpl -> {
        (oldReader.closed as? CompletableJsPromise<Unit>?)?.resolve(Unit)
        while (true) oldReader.poll()?.resolve(ReadResult(null, true)) ?: break
      }

      else -> Unit // no-op
    }
  }

  /**
   * Cancel the stream with an error; if locked, the reader will also be closed with the same cause. All pending read
   * requests will be rejected with [reason].
   */
  internal fun error(reason: Any? = null) {
    if (!mutableState.compareAndSet(STREAM_READABLE, STREAM_ERRORED))
      throw TypeError.create("Stream is not readable")

    errorCause.set(reason)

    when (val oldReader = mutableReader.getAndSet(null)) {
      is ReadableStreamDefaultReaderImpl -> {
        (oldReader.closed as? CompletableJsPromise<Unit>?)?.reject(Unit)
        while (true) oldReader.poll()?.reject(reason) ?: break
      }

      else -> Unit // no-op
    }
  }

  /** Clear the current reader instance, unlocking the stream. */
  internal fun releaseReader() {
    mutableReader.getAndSet(null)?.detach()
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    return when (state) {
      STREAM_CLOSED -> JsPromise.resolved(Unit)
      STREAM_ERRORED -> JsPromise.rejected(TypeError.create(errorCause.get().toString()))
      else -> {
        close()
        controller.cancel(reason)
      }
    }
  }

  override fun getReader(options: Any?): ReadableStreamReader {
    val previous = mutableReader.getAndUpdate { current -> current ?: ReadableStreamDefaultReaderImpl(this) }

    if (previous != null) throw TypeError.create("Stream is locked")
    return reader
  }

  override fun pipeThrough(transform: TransformStream, options: Any?): ReadableStream = TODO("Not yet implemented")
  override fun pipeTo(destination: WritableStream, options: Any?): JsPromise<Unit> = TODO("Not yet implemented")
  override fun tee(): Array<ReadableStream> = TODO("Not yet implemented")

  internal companion object {
    /** The stream is readable and fully functional. */
    internal const val STREAM_READABLE = 0

    /** The stream has been closed normally without an exception. */
    internal const val STREAM_CLOSED = 1

    /** The stream has been closed exceptionally. */
    internal const val STREAM_ERRORED = 2
  }
}
