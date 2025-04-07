package elide.runtime.gvm.internals.intrinsics.js.webstreams

import org.graalvm.polyglot.Value
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.vm.annotations.Polyglot

/**
 * A lightweight, delegating implementation of the [ReadableStreamDefaultController] interface, wrapping the controlled
 * stream instance.
 */
@JvmInline internal value class ReadableStreamDefaultControllerToken(
  private val stream: ReadableDefaultStream
) : ReadableStreamDefaultController {
  @get:Polyglot override val desiredSize: Double? get() = stream.desiredSize()

  @Polyglot override fun enqueue(chunk: Value?) {
    if (chunk == null) throw TypeError.create("Cannot enqueue null or undefined chunk")
    stream.fulfillOrEnqueue(chunk)
  }

  @Polyglot override fun error(reason: Any?) = stream.error(reason)
  @Polyglot override fun close() = stream.close()
}
