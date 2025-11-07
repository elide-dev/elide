/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
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
package elide.runtime.http.server.js.worker

import io.netty.buffer.ByteBuf
import java.nio.ByteBuffer
import elide.runtime.exec.ContextAwareExecutor
import elide.runtime.exec.PinnedContext
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.http.server.HttpRequestConsumer
import elide.runtime.http.server.HttpRequestBody
import elide.runtime.http.server.HttpResponseBody
import elide.runtime.http.server.common.WorkerResponseContent
import elide.runtime.http.server.js.worker.JsWorkerStreams.consumeResponseStream
import elide.runtime.http.server.source
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.runtime.intrinsics.js.stream.ReadableStreamSource

/**
 * Provides utilities to map between Web Streams and request/response body streams in Workers-style applications.
 *
 * Use [JsWorkerStreams.forRequest] to create a [ReadableStream] from a request body stream, and
 * [consumeResponseStream] to write the contents of a [ReadableStream] to a response body stream.
 */
internal object JsWorkerStreams {
  /**
   * Returns a [ReadableStream] that pulls from the [requestBody] using the given [executor] for context management.
   * Failures and cancellation events are propagated between the streams, so they are properly synchronized.
   */
  fun forRequest(requestBody: HttpRequestBody, executor: ContextAwareExecutor): ReadableStream {
    val pinnedContext = PinnedContext.current()
    val source = object : ReadableStreamSource, HttpRequestConsumer {
      @Volatile private var streamController: ReadableStreamDefaultController? = null
      @Volatile private var streamReader: HttpRequestBody.Reader? = null

      override fun start(controller: ReadableStreamController) {
        streamController = controller as ReadableStreamDefaultController
      }

      override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
        return if (streamReader?.pull() != null) JsPromise.resolved(Unit)
        else JsPromise.rejected("Stream source is detached")
      }

      override fun cancel(reason: Any?): JsPromise<Unit> {
        streamController = null
        streamReader?.release()

        return JsPromise.resolved(Unit)
      }

      override fun onAttached(reader: HttpRequestBody.Reader) {
        streamReader = reader
      }

      override fun onRead(content: ByteBuf) {
        content.retain()
        executor.execute(pinnedContext) {
          try {
            val buffer = ByteBuffer.allocate(content.readableBytes())
            content.readBytes(buffer)

            streamController?.enqueue(ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, buffer))
          } finally {
            content.release()
          }
        }
      }

      override fun onClose(failure: Throwable?) {
        if (failure == null) streamController?.close()
        else streamController?.error(failure)

        streamReader = null
      }
    }

    requestBody.consume(source)
    return ReadableStream.create(source)
  }

  /**
   * Use a web [sourceStream] as the source for the given [callStream], using [executor] for context management.
   * Errors are propagated between the streams, and close event are properly synchronized.
   */
  fun consumeResponseStream(
    sourceStream: ReadableStream,
    callStream: HttpResponseBody,
    executor: ContextAwareExecutor
  ) {
    val reader = sourceStream.getReader() as ReadableStreamDefaultReader
    val pinnedContext = PinnedContext.current()

    callStream.source(onClose = { if (!reader.closed.isDone) reader.releaseLock() }) { writer ->
      executor.execute(pinnedContext) {
        if (reader.closed.isDone) writer.end() else reader.read().then(
          onFulfilled = {
            if (it.value != null) writer.write(WorkerResponseContent.mapElement(it.value))
            if (it.done) writer.end()
          },
          onCatch = {
            writer.end(IllegalStateException("Failed to read response body: $it"))
          },
        )
      }
    }
  }
}
