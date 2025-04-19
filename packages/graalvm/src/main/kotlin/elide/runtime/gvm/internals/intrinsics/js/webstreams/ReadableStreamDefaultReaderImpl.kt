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
package elide.runtime.gvm.internals.intrinsics.js.webstreams

import java.util.concurrent.ConcurrentLinkedQueue
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_ERRORED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamImpl.Companion.STREAM_READABLE
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream.ReadResult
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultReader
import elide.vm.annotations.Polyglot

/** Shorthand for a completable promise used to define read queues. */
internal typealias DefaultReadRequest = CompletableJsPromise<ReadResult>

/** Implementation of a default stream reader. */
internal class ReadableStreamDefaultReaderImpl(private val stream: ReadableStreamImpl) : ReadableStreamDefaultReader {
  /** Queue used for read requests that cannot be fulfilled immediately. */
  private val queue = ConcurrentLinkedQueue<DefaultReadRequest>()

  @Polyglot override val closed = when (stream.state) {
    STREAM_CLOSED -> JsPromise.resolved(Unit)
    STREAM_ERRORED -> JsPromise.rejected(stream.error)
    else -> JsPromise()
  }

  /** Read from the given default [controller] instance. */
  private fun read(controller: ReadableStreamDefaultControllerImpl): JsPromise<ReadResult> {
    // consume queued chunks if available; if the stream was closed after polling, mark as 'done'
    controller.poll()?.let {
      return JsPromise.Companion.resolved(ReadResult(it.chunk, stream.state != STREAM_READABLE))
    }

    // enqueue read request and pull
    return JsPromise<ReadResult>().also { request ->
      queue.add(request)
      controller.pullIfNeeded()
    }
  }

  /** Remove the next entry in the read queue, if any exist. */
  internal fun poll(): DefaultReadRequest? {
    return queue.poll()
  }

  /** Mark this reader as detached from its stream, making all operations invalid. */
  internal fun detach() {
    (closed as? CompletableJsPromise<Unit>)?.resolve(Unit)
  }

  @Polyglot override fun read(): JsPromise<ReadResult> {
    if (closed.isDone) throw TypeError.Companion.create("Reader has already been released")
    return when (stream.state) {
      STREAM_READABLE -> when (val controller = stream.controller) {
        is ReadableStreamDefaultControllerImpl -> read(controller)
        else -> throw TypeError.Companion.create("Unsupported or unknown controller type")
      }

      STREAM_ERRORED -> JsPromise.Companion.rejected(TypeError.Companion.create(stream.error.toString()))
      else -> JsPromise.Companion.resolved(ReadResult(null, true))
    }
  }

  @Polyglot override fun releaseLock() {
    if (closed.isDone) throw TypeError.Companion.create("Reader has already been released")
    (closed as? CompletableJsPromise<Unit>)?.reject(TypeError.Companion.create("Reader lock was released"))
    stream.releaseReader()
  }

  @Polyglot override fun cancel() {
    stream.cancel()
  }
}
