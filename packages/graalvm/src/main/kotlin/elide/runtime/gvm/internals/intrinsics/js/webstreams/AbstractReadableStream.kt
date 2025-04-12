package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.ReadableStreamSource


/**
 * Base abstract class for the native `ReadableStream` standard implementation.
 *
 * Unlike in the WHATWG reference, the default and BYOB stream components are separated into specialized classes to
 * remove the need for casting and type checks on every operation.
 *
 * This class defines the shared public API for streams, according to the
 * [spec](https://streams.spec.whatwg.org/#rs-class-definition).
 */
public sealed class AbstractReadableStream : ReadableStream {
  /** Thread-safe state flag for the stream. */
  protected val streamState: AtomicInteger = AtomicInteger(STREAM_READABLE)

  /** The source this stream pulls data from. */
  protected abstract val source: ReadableStreamSource

  /** The queuing strategy used by the stream's controller to pull data from the [source]. */
  protected abstract val strategy: QueueingStrategy

  public companion object {
    // Stream state values

    /** The stream is readable and fully functional. */
    internal const val STREAM_READABLE = 0

    /** The stream has been closed normally without an exception. */
    internal const val STREAM_CLOSED = 1

    /** The stream has been closed exceptionally. */
    internal const val STREAM_ERRORED = 2

    // Controller state values

    /** The underlying source has not been initialized yet.*/
    internal const val CONTROLLER_UNINITIALIZED = 0

    /** The controller is ready, the source has completed startup. */
    internal const val CONTROLLER_STARTED = 1

    /** The controller is pulling a new value from the source. */
    internal const val CONTROLLER_PULLING = 2

    /** The controller is pulling and will pull again when the current pull completes. */
    internal const val CONTROLLER_PULL_AGAIN = 3

    /** The controller is being closed, but must finish other operations before finishing. */
    internal const val CONTROLLER_CLOSING = 4

    /** The controller is closed and can no longer be used. */
    internal const val CONTROLLER_CLOSED = 5

    public fun from(
      source: ReadableStreamSource,
      strategy: QueueingStrategy = QueueingStrategy.Default
    ): ReadableStream = when (source.type) {
      ReadableStream.Type.Default -> ReadableDefaultStream(source, strategy)
      ReadableStream.Type.BYOB -> TODO("BYOB streams are not yet supported")
    }
  }
}

