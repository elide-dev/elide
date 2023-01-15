package elide.frontend.ssr

import react.ReactElement
import web.streams.ReadableStreamDefaultReadValueResult
import web.streams.ReadableStreamDefaultReader
import kotlin.js.Promise
import js.core.jso
import react.dom.server.renderToReadableStream as renderSSRStreaming

/**
 * TBD
 */
public external interface ResponseChunk {
  /** */
  public var status: Int?

  /** */
  public var headers: Map<String, String>?

  /** */
  public var content: String?

  /** */
  public var css: String?

  /** */
  public var hasContent: Boolean

  /** */
  public var fin: Boolean
}

/**
 * TBD
 */
public typealias RenderCallback = (ResponseChunk) -> Unit

/**
 *
 */
public class ApplicationBuffer constructor (private val app: ReactElement<*>) {
  // Whether we have finished streaming.
  private var fin: Boolean = false

  // Call the `emitter` with a well-formed `ResponseChunk`.
  private fun finishStream(statusCode: Int, emitter: (ResponseChunk) -> Unit) {
    emitter.invoke(jso {
      status = statusCode
      fin = true
    })
  }

  private fun pump(reader: ReadableStreamDefaultReader<ByteArray>, emitter: (ResponseChunk) -> Unit) {
    reader.read().then { value ->
      val chunk = value.unsafeCast<ReadableStreamDefaultReadValueResult<ByteArray?>>()

      val rawContent = chunk.value
      var resolved = false
      if (rawContent != null && rawContent.isNotEmpty()) {
        resolved = true  // we're handling a content chunk
        val decoded = rawContent.decodeToString()
        emitter.invoke(jso {
          content = decoded
          fin = false
          hasContent = decoded.isNotBlank()
        })
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
  private fun render(callback: (ResponseChunk) -> Unit): Promise<*> {
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
  public fun execute(callback: (ResponseChunk) -> Unit): Promise<*> {
    return render(callback)
  }
}
