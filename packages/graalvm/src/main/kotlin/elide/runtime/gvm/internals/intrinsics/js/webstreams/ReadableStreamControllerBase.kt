package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_CLOSING
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_PULLING
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_PULL_AGAIN
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_STARTED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamControllerBase.Companion.CONTROLLER_UNINITIALIZED
import elide.runtime.intrinsics.js.JsPromise

/** Base class used by readable stream controller implementations. */
internal abstract class ReadableStreamControllerBase {
  /**
   * Represents a chunk of data that can be enqueued into a controller by a source. Default controllers use simple
   * [SizedChunk] values, whereas byte controllers use [ByobChunk] instances that keep track of additional state to
   * ensure buffers are filled correctly.
   */
  internal sealed interface QueueChunk

  /** Simple chunk type allowing an arbitrary untyped value to be enqueued with a computed size. */
  internal data class SizedChunk(val chunk: Any?, val size: Double) : QueueChunk

  /** A chunk wrapping a buffer to be written into by the stream's source. */
  internal interface ByobChunk : QueueChunk

  /**
   * Mutable state flag, corresponding to the current state of the controller: [CONTROLLER_UNINITIALIZED],
   * [CONTROLLER_STARTED], [CONTROLLER_PULLING], [CONTROLLER_PULL_AGAIN], [CONTROLLER_CLOSING], or [CONTROLLER_CLOSED].
   */
  protected val mutableState = AtomicInteger(CONTROLLER_UNINITIALIZED)

  /**
   * Current state of the controller: [CONTROLLER_UNINITIALIZED], [CONTROLLER_STARTED], [CONTROLLER_PULLING],
   * [CONTROLLER_PULL_AGAIN], [CONTROLLER_CLOSING], or [CONTROLLER_CLOSED].
   */
  internal val state: Int get() = mutableState.get()

  /**
   * Cancel the controller stream with an optional [reason]. The returned promise encapsulates the cancellation of the
   * underlying source of the stream.
   */
  internal abstract fun cancel(reason: Any? = null): JsPromise<Unit>

  internal companion object {
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
