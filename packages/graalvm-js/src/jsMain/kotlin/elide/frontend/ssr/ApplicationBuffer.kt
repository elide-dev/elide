/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.frontend.ssr

import js.objects.jso
import react.ReactElement
import web.streams.ReadableStreamDefaultReader
import web.streams.ReadableStreamReadValueResult
import kotlinx.coroutines.await
import kotlin.js.Promise
import react.dom.server.rawRenderToString as renderSSRString
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
public class ApplicationBuffer (private val app: ReactElement<*>, private val stream: Boolean = true) {
  // Whether we have finished streaming.
  private var fin: Boolean = false

  // Call the `emitter` with a well-formed `ResponseChunk`.
  private fun finishStream(statusCode: Int, emitter: (ResponseChunk) -> Unit) {
    emitter.invoke(jso {
      status = statusCode
      fin = true
    })
  }

  private suspend fun pump(reader: ReadableStreamDefaultReader<ByteArray>, emitter: (ResponseChunk) -> Unit) {
    reader.read().let { value ->
      val chunk: dynamic = value.unsafeCast<ReadableStreamReadValueResult<ByteArray?>>()

      val rawContent: ByteArray? = chunk.value as? ByteArray
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

      val done = chunk.done as? Boolean ?: false
      if (done) {
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

  private suspend fun renderStream(callback: (ResponseChunk) -> Unit): Promise<*> {
    return renderSSRStreaming(app).await().let { stream ->
      pump(stream.getReader(), callback)
      Promise { accept, _ ->
        accept(stream)
      }
    }
  }

  /**
   * TBD
   */
  private suspend fun render(callback: (ResponseChunk) -> Unit): Promise<*> {
    return if (stream) renderStream(callback) else Promise { accept, reject ->
      try {
        accept(renderSSRString(app))
      } catch (err: Throwable) {
        reject(err)
      }
    }
  }

  /** @return Whether the buffer is still rendering. */
  public fun isExecuting(): Boolean = !fin

  /**
   * TBD
   */
  public suspend fun execute(callback: (ResponseChunk) -> Unit): Promise<*> {
    return render(callback)
  }
}
