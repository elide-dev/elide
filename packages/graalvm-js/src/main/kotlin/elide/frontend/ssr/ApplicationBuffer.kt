package elide.frontend.ssr

import elide.runtime.ssr.ServerResponse
import react.ReactElement
import web.streams.ReadableStreamDefaultReadValueResult
import web.streams.ReadableStreamDefaultReader
import kotlin.js.Promise
import react.dom.server.renderToReadableStream as renderSSRStreaming

/**
 *
 */
public class ApplicationBuffer constructor (private val app: ReactElement<*>) {
  // Whether we have finished streaming.
  private var fin: Boolean = false

  // Call the `emitter` with a well-formed `ServerResponse`.
  private fun finishStream(status: Int, emitter: (ServerResponse) -> Unit) {
    emitter.invoke(object: ServerResponse {
      override val status: Int get() = status
      override val fin: Boolean get() = true
    })
  }

  private fun pump(reader: ReadableStreamDefaultReader<ByteArray>, emitter: (ServerResponse) -> Unit) {
    reader.read().then { value ->
      val chunk = value.unsafeCast<ReadableStreamDefaultReadValueResult<ByteArray?>>()

      val content = chunk.value
      var resolved = false
      if (content != null && content.isNotEmpty()) {
        resolved = true  // we're handling a content chunk
        val decoded = content.decodeToString()
        val emit = object: ServerResponse {
          override val content: String get() = decoded
          override val fin: Boolean get() = false
          override val hasContent: Boolean get() = decoded.isNotBlank()
        }
        emitter.invoke(emit)
      }

      if (chunk.done) {
        resolved = true  // we're done with the stream
        fin = true
        finishStream(200, emitter)
      } else {
        pump(reader, emitter)
      }

      // error catch: if we haven't handled either a content chunk or a finish chunk, then we have an error case of some
      // kind, or some unhandled case in general.
      if (!resolved) {
        console.error(
          "Failed to read chunk from stream: got `null` or empty content. Got value: ",
          JSON.stringify(value)
        )
        finishStream(500, emitter)
      }
    }
  }

  /**
   * TBD
   */
  private fun render(callback: (ServerResponse) -> Unit): Promise<*> {
    return renderSSRStreaming(app).then { stream ->
      val reader = stream.getReader()
      pump(reader, callback)
    }
  }

  /** @return Whether the buffer is still rendering. */
  public fun isExecuting(): Boolean = !fin

  /**
   * TBD
   */
  public fun execute(callback: (ServerResponse) -> Unit): Promise<*> {
    return render(callback)
  }
}
