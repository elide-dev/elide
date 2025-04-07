package elide.runtime.gvm.internals.intrinsics.js.webstreams.readable

import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.gvm.internals.intrinsics.js.webstreams.readable.ReadableStream.ReaderMode.BYOB
import elide.runtime.gvm.internals.intrinsics.js.webstreams.readable.ReadableStream.ReaderMode.Default
import elide.runtime.intrinsics.js.JsPromise
import elide.vm.annotations.Polyglot

/**
 * Interface for stream controller implementations, providing the shared methods specified in the
 * [WHATWG standard](https://streams.spec.whatwg.org).
 */
public sealed interface ReadableStreamController

/**
 * Interface for stream reader implementations, providing the shared methods specified in the
 * [WHATWG standard](https://streams.spec.whatwg.org/#generic-reader-mixin).
 */
public sealed interface ReadableStreamReader

/**
 * Base abstract class for the native `ReadableStream` standard implementation.
 *
 * Unlike in the WHATWG reference, the default and BYOB stream components are separated into specialized classes to
 * remove the need for casting and type checks on every operation.
 *
 * This class defines the shared public API for streams, according to the
 * [spec](https://streams.spec.whatwg.org/#rs-class-definition).
 */
public sealed class ReadableStream {
  /** Defines the type of streams implemented by the standard. */
  public enum class Type {
    /**
     * The default stream type, which uses a `ReadableStreamDefaultController` and `ReadableStreamDefaultReader`
     * instances.
     */
    Default,

    /**
     * A stream optimized for byte operations, using `ReadableStreamBYOBController and `ReadableStreamBYOBReader`
     * types, which allow consumers to provide their own buffers for sources to write the data in.
     */
    BYOB,
  }

  /**
   * When used as part of the [GetReaderOptions] in [ReadableStream.getReader], the returned reader instance will match
   * this setting. Note that using [BYOB] mode on a default stream will result in an error (but [Default] is always
   * allowed).
   */
  public enum class ReaderMode {
    /** Requests a default reader, which moves elements from the stream as they are. Available for all stream types. */
    Default,

    /**
     * Requests a BYOB reader, which allows passing a custom buffer for the data to be written into. Available only for
     * BYOB streams.
     */
    BYOB,
  }

  /** Options struct that can be optionally supplied as part of [ReadableStream.getReader]. */
  public data class GetReaderOptions(
    /** The type of reader that should be returned. */
    val mode: ReaderMode
  )

  /** Thread-safe state flag for the stream. */
  protected val streamState: AtomicInteger = AtomicInteger(STREAM_READABLE)

  /** The source this stream pulls data from. */
  protected abstract val source: ReadableStreamSource

  /** The queuing strategy used by the stream's controller to pull data from the [source]. */
  protected abstract val strategy: ReadableStreamQueuingStrategy

  /** Whether the stream is currently [locked](https://streams.spec.whatwg.org/#lock) to a [reader]. */
  @get:Polyglot public abstract val locked: Boolean

  /** Cancel the stream with an optional reason, releasing the underlying source. */
  @Polyglot public abstract fun cancel(reason: Any? = null): JsPromise<Unit>

  /**
   * Obtain a new [ReadableStreamReader] instance and lock the stream to it, preventing any new readers from being
   * acquired until it is released.
   *
   * If the stream is already [locked], an error will be thrown.
   */
  @Polyglot public abstract fun getReader(options: Any? = null): ReadableStreamReader

  internal companion object {
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
  }
}
