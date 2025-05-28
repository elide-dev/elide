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
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import elide.runtime.exec.GuestExecutor
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamBase.Companion.READABLE_STREAM_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamBase.Companion.READABLE_STREAM_ERRORED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamBase.Companion.READABLE_STREAM_READABLE
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_ERRORED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_WRITABLE
import elide.runtime.intrinsics.js.AbortSignal
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.events.EventListener

internal class StreamPipe private constructor(
  private val scope: CoroutineScope,
  private val source: ReadableStreamBase,
  private val dest: WritableDefaultStream,
  private val preventClose: Boolean,
  private val preventAbort: Boolean,
  private val preventCancel: Boolean,
  private val signal: AbortSignal?,
) {
  // TODO(@darvld): use a BYOB reader when the source is a byte stream for better performance
  val reader = source.getReader() as ReadableStreamDefaultReaderToken
  val writer = dest.getWriter() as WritableStreamDefaultWriterToken

  private val shuttingDown = AtomicBoolean(false)
  private val monitorPromise = JsPromise<Unit>()

  private val abortListener by lazy { EventListener { abortPipe() } }
  private val pendingWrites = ConcurrentLinkedQueue<JsPromise<Unit>>()

  private fun abortPipe() {
    val abortReason = signal!!.reason
    val actions = mutableListOf<() -> Job?>()

    if (!preventAbort) actions.add {
      if (dest.streamState == WRITABLE_STREAM_WRITABLE) dest.abortStream(abortReason).asDeferred()
      else null
    }

    if (!preventCancel) actions.add {
      if (source.state == READABLE_STREAM_READABLE) source.cancel(abortReason).asDeferred()
      else null
    }

    scope.launch {
      shutdown(abortReason) {
        // wait for all shutdown hooks
        actions.mapNotNull { it() }.joinAll()
      }
    }
  }

  private suspend fun transfer() {
    // note: according to the spec, the public API methods of the reader and writer should not be used,
    // and the streams should instead be manipulated directly
    while (currentCoroutineContext().isActive) {
      // enforce backpressure (technically this uses a public API, but it is only a getter)
      if (dest.desiredSize()!! <= 0) writer.ready.asDeferred().await()

      // wait for reads but not for writes, enqueue the tracking promises
      val chunk = source.readOrEnqueue().asDeferred().await()
      chunk.value?.let { pendingWrites.offer(dest.writeChunk(it)) }

      var done = chunk.done
      if (source.state == READABLE_STREAM_ERRORED) {
        if (preventAbort) shutdown(source.storedError)
        else shutdown(source.storedError) { dest.abortStream(source.storedError) }
        done = true
      }

      if (dest.streamState == WRITABLE_STREAM_ERRORED) {
        if (preventCancel) shutdown(dest.errorCause)
        else shutdown(dest.errorCause) { source.cancel(dest.errorCause) }
        done = true
      }

      if (source.state == READABLE_STREAM_CLOSED) {
        if (preventClose) shutdown()
        else shutdown { writer.closeWithErrorPropagation() }
        done = true
      }

      if (dest.streamState == WRITABLE_STREAM_CLOSED || dest.closeQueuedOrInflight) {
        val destClosed = TypeError.create("Pipe destination is closed")
        if (preventCancel) shutdown(destClosed)
        else shutdown(destClosed) { source.cancel(destClosed) }
        done = true
      }

      if (done) break
    }
  }

  private suspend fun shutdown(error: Any? = null, action: (suspend () -> Unit)? = null) {
    if (!shuttingDown.compareAndSet(false, true)) return

    if (dest.streamState == WRITABLE_STREAM_WRITABLE && !dest.closeQueuedOrInflight) {
      // wait for all pending writes
      while (pendingWrites.isNotEmpty()) pendingWrites.poll()?.asDeferred()?.await()
    }

    if (action == null) {
      finalize(error)
      return
    }

    runCatching { action() }
      .onSuccess { finalize(error) }
      .onFailure { finalize(it) }
  }

  private fun finalize(cause: Any? = null) {
    dest.releaseWriter(writer)
    source.releaseReader()

    signal?.removeEventListener("aborted", abortListener)

    if (cause != null) monitorPromise.reject(cause)
    else monitorPromise.resolve(Unit)

    scope.cancel()
  }

  internal companion object {
    internal fun pipe(
      executor: GuestExecutor,
      source: ReadableStreamBase,
      dest: WritableDefaultStream,
      preventClose: Boolean,
      preventAbort: Boolean,
      preventCancel: Boolean,
      signal: AbortSignal?,
    ): JsPromise<Unit> {
      val scope = CoroutineScope(SupervisorJob() + executor)
      val pipe = StreamPipe(scope, source, dest, preventClose, preventAbort, preventCancel, signal)

      signal?.let {
        // early return if aborted before starting
        if (it.aborted) {
          pipe.abortListener.handleEvent()
          return pipe.monitorPromise
        }

        it.addEventListener("aborted", pipe.abortListener)
      }

      scope.launch {
        pipe.transfer()
      }
      return pipe.monitorPromise
    }
  }
}
