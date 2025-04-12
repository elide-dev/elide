package elide.runtime.intrinsics.js.stream

import elide.vm.annotations.Polyglot

/** A controller used by streams with a 'default' source, allowing arbitrary chunks to be enqueued. */
public interface ReadableStreamDefaultController : ReadableStreamController {
  /**
   * Close the controller and the associated stream. If there are undelivered chunks, the stream will not be closed
   * until they are claimed.
   */
  @Polyglot public fun close()

  /**
   * Shut down the controller and associated stream with the given error [reason]. Unlike [close], this method does
   * not wait for undelivered elements to be claimed, all unread data will be lost.
   */
  @Polyglot public fun error(reason: Any? = null)

  /**
   * Enqueue a new chunk, making it available immediately for readers. If any pending read requests are found, the
   * chunk will be delivered directly without using the queue.
   */
  @Polyglot public fun enqueue(chunk: Any? = null)
}
