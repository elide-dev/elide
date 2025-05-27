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
import elide.runtime.exec.GuestExecutor
import elide.runtime.gvm.internals.intrinsics.js.webstreams.ReadableStreamBase.Companion.READABLE_STREAM_ERRORED
import elide.runtime.gvm.internals.intrinsics.js.webstreams.WritableDefaultStream.Companion.WRITABLE_STREAM_ERRORED
import elide.runtime.interop.ReadOnlyProxyObject
import elide.runtime.intrinsics.js.CompletableJsPromise
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.*
import elide.vm.annotations.Polyglot

internal class TransformDefaultStream(
  val transformer: TransformStreamTransformer,
  executor: GuestExecutor,
  writableStrategy: QueuingStrategy = QueuingStrategy.DefaultReadStrategy,
  readableStrategy: QueuingStrategy = QueuingStrategy.DefaultReadStrategy,
) : TransformStream, ReadOnlyProxyObject {
  @JvmInline private value class DelegatingSource(private val stream: TransformDefaultStream) : ReadableStreamSource {
    override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
      check(stream.backpressure.get())
      return stream.setBackpressure(false)
    }

    override fun cancel(reason: Any?): JsPromise<Unit> {
      stream.finishPromise.get()?.let { return it }

      val finishPromise = JsPromise<Unit>()
      stream.finishPromise.set(finishPromise)

      stream.transformer.cancel(reason).then(
        onFulfilled = {
          if (stream.writable.streamState == WRITABLE_STREAM_ERRORED) finishPromise.reject(stream.writable.errorCause)
          else {
            stream.writable.errorIfNeeded(reason)
            stream.unblockWrite()
            finishPromise.resolve(Unit)
          }
        },
        onCatch = {
          stream.writable.errorIfNeeded(it)
          stream.unblockWrite()
          finishPromise.reject(it)
        },
      )

      return finishPromise
    }
  }

  @JvmInline private value class DelegatingSink(private val stream: TransformDefaultStream) : WritableStreamSink {
    override fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
      return if (!stream.backpressure.get()) stream.performTransform(chunk)
      else checkNotNull(stream.backpressurePromise.get()).then(
        onFulfilled = {
          val state = stream.writable.streamState
          if (state == WritableDefaultStream.WRITABLE_STREAM_ERRORING)
            throw TypeError.create(stream.writable.errorCause.toString())

          check(state == WritableDefaultStream.WRITABLE_STREAM_WRITABLE)
          stream.performTransform(chunk)
        },
      )
    }

    override fun close(): JsPromise<Unit> {
      stream.finishPromise.get()?.let { return it }

      val finishPromise = JsPromise<Unit>()
      stream.finishPromise.set(finishPromise)

      stream.transformer.flush(stream.controller).then(
        onFulfilled = {
          if (stream.readable.state == READABLE_STREAM_ERRORED) finishPromise.reject(stream.readable.storedError)
          else {
            stream.readable.close()
            finishPromise.resolve(Unit)
          }
        },
        onCatch = {
          stream.readable.error(it)
          finishPromise.reject(it)
        },
      )

      return finishPromise
    }

    override fun abort(reason: Any?): JsPromise<Unit> {
      stream.finishPromise.get()?.let { return it }

      val finishPromise = JsPromise<Unit>()
      stream.finishPromise.set(finishPromise)

      stream.transformer.cancel(reason).then(
        onFulfilled = {
          if (stream.readable.state == READABLE_STREAM_ERRORED) finishPromise.reject(stream.readable.storedError)
          else {
            stream.readable.error(stream.readable.storedError)
            finishPromise.resolve(Unit)
          }
        },
        onCatch = {
          stream.readable.error(it)
          finishPromise.reject(it)
        },
      )

      return finishPromise
    }
  }

  @JvmInline private value class TransformStreamDefaultControllerToken(
    private val stream: TransformDefaultStream
  ) : TransformStreamDefaultController {
    override val desiredSize: Double?
      get() = stream.readable.desiredSize()

    @Polyglot override fun enqueue(chunk: Value) {
      if (!stream.readable.canCloseOrEnqueue()) throw TypeError.create("Readable stream is closed")
      runCatching { stream.readable.fulfillOrEnqueue(chunk) }.onFailure { cause ->
        stream.errorWritableAndUnblockWrite(cause)
        throw TypeError.create(stream.readable.storedError.toString())
      }

      if (stream.readable.hasBackpressure() != stream.backpressure.get()) {
        stream.setBackpressure(true)
      }
    }

    @Polyglot override fun error(reason: Value) {
      stream.errorStream(reason)
    }

    @Polyglot override fun terminate() {
      stream.readable.close()
      stream.errorWritableAndUnblockWrite(TypeError.create("Stream has been terminated"))
    }
  }

  private val backpressure = AtomicBoolean(true)
  private val backpressurePromise = AtomicReference<CompletableJsPromise<Unit>>(JsPromise())

  private val finishPromise = AtomicReference<CompletableJsPromise<Unit>>()

  private val controller = TransformStreamDefaultControllerToken(this)

  @Polyglot override val readable: ReadableDefaultStream = ReadableDefaultStream(
    source = DelegatingSource(this),
    strategy = readableStrategy,
    executor,
  )

  @Polyglot override val writable: WritableDefaultStream = WritableDefaultStream(
    sink = DelegatingSink(this),
    strategy = writableStrategy,
  )

  init {
    transformer.start(controller)
  }

  private fun performTransform(chunk: Value): JsPromise<Unit> {
    return transformer.transform(chunk, controller).catch { reason ->
      errorStream(reason)
      throw TypeError.create(reason.toString())
    }
  }

  private fun unblockWrite() {
    if (backpressure.get()) setBackpressure(false)
  }

  private fun errorStream(reason: Any?) {
    readable.error(reason)
  }

  private fun errorWritableAndUnblockWrite(reason: Any?) {
    writable.errorIfNeeded(reason)
    unblockWrite()
  }

  private fun setBackpressure(value: Boolean): JsPromise<Unit> {
    check(backpressure.compareAndSet(!value, value))

    val promise = JsPromise<Unit>()
    backpressurePromise.getAndSet(promise)?.resolve(Unit)

    return promise
  }

  override fun getMemberKeys(): Array<String> = MEMBERS
  override fun getMember(key: String?): Any? = when (key) {
    MEMBER_READABLE -> readable
    MEMBER_WRITABLE -> writable
    else -> null
  }

  private companion object {
    private const val MEMBER_READABLE = "readable"
    private const val MEMBER_WRITABLE = "writable"
    private val MEMBERS = arrayOf(MEMBER_READABLE, MEMBER_WRITABLE)
  }
}
