package elide.runtime.intrinsics.js.stream

import elide.vm.annotations.Polyglot

/**
 * Interface for stream controller implementations, providing the shared methods specified in the
 * [WHATWG standard](https://streams.spec.whatwg.org).
 */
public sealed interface ReadableStreamController {
  /** Desired total size for the inbound queue, used for backpressure control. */
  @get:Polyglot public val desiredSize: Double?
}
