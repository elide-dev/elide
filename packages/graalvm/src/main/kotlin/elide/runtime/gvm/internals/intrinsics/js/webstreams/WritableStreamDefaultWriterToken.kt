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

import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_CLOSED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_ERRORED
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultWriter
import elide.vm.annotations.Polyglot

/**
 * A light implementation of a writable stream writer that delegates most of the logic to the [stream] instance owning
 * it.
 */
internal class WritableStreamDefaultWriterToken(
  private val stream: WritableDefaultStream
) : WritableStreamDefaultWriter {
  /** Whether this writer is detached, i.e. released by the [stream] and no longer valid. */
  private val detached = AtomicBoolean()

  /** A promise used to communicate changes in backpressure to data producers. */
  private val mutableReadyPromise = AtomicReference<CompletableJsPromise<Unit>>()

  /** A promise used to signal the closing of the stream to data producers. */
  private val mutableClosePromise = AtomicReference<CompletableJsPromise<Unit>>()

  @get:Polyglot override val ready: CompletableJsPromise<Unit> get() = mutableReadyPromise.get()
  @get:Polyglot override val closed: CompletableJsPromise<Unit> get() = mutableClosePromise.get()
  @get:Polyglot override val desiredSize: Double?
    get() = if (detached.get()) throw TypeError.create("Writer has been released")
    else stream.desiredSize()

  /** Update the writer's [ready] promise, setting its value to [newPromise], and returning the new value. */
  internal fun refreshReadyPromise(newPromise: CompletableJsPromise<Unit> = JsPromise()): CompletableJsPromise<Unit> {
    return newPromise.also(mutableReadyPromise::set)
  }

  /** Update the writer's [close] promise, setting its value to [newPromise], and returning the new value. */
  internal fun refreshClosePromise(newPromise: CompletableJsPromise<Unit> = JsPromise()): CompletableJsPromise<Unit> {
    return newPromise.also(mutableClosePromise::set)
  }

  /**
   * Ensures that the writer's [ready] promise is rejected, by replacing it with a new rejected promise if it the
   * current already.instance is already settled, or rejecting it if it is pending.
   */
  internal fun ensureReadyPromiseRejected(reason: Any?) {
    val readyPromise = ready
    if (!readyPromise.isDone) readyPromise.reject(reason)
    else mutableReadyPromise.getAndSet(JsPromise<Unit>().also { it.reject(reason) })
  }

  /**
   * Ensures that the writer's [closed] promise is rejected, by replacing it with a new rejected promise if it the
   * current already.instance is already settled, or rejecting it if it is pending.
   */
  internal fun ensureClosedPromiseRejected(reason: Any?) {
    val closePromise = ready
    if (!closePromise.isDone) closePromise.reject(reason)
    else mutableClosePromise.getAndSet(JsPromise<Unit>().also { it.reject(reason) })
  }

  internal fun closeWithErrorPropagation(): JsPromise<Unit> {
    check(!detached.get())
    val streamState = stream.streamState

    return when {
      stream.closeQueuedOrInflight || streamState == WRITABLE_STREAM_CLOSED -> JsPromise.resolved(Unit)
      streamState == WRITABLE_STREAM_ERRORED -> JsPromise.rejected(stream.errorCause)
      else -> close()
    }
  }

  @Polyglot override fun write(chunk: Value): JsPromise<Unit> {
    return if (detached.get()) JsPromise.rejected(TypeError.create("Writer has been released"))
    else stream.writeChunk(chunk)
  }

  @Polyglot override fun releaseLock() {
    if (!detached.compareAndSet(false, true)) return
    stream.releaseWriter(this)
  }

  @Polyglot override fun abort(reason: Any?): JsPromise<Unit> {
    return if (detached.get()) JsPromise.rejected(TypeError.create("Writer has been released"))
    else stream.abortStream(reason)
  }

  @Polyglot override fun close(): JsPromise<Unit> = when {
    detached.get() -> JsPromise.rejected(TypeError.create("Writer has been released"))
    stream.closeQueuedOrInflight -> JsPromise.rejected(TypeError.create("Stream is already closing"))
    else -> stream.closeStream()
  }
}
