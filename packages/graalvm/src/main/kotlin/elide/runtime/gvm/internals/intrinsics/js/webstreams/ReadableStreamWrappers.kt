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
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.ReadableByteStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.runtime.intrinsics.js.stream.ReadableStreamSource

internal object ReadableStreamEmptySource : ReadableStreamSource {
  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    (controller as ReadableStreamDefaultController).close()
    return super.pull(controller)
  }
}

@JvmInline internal value class ReadableStreamInputStreamSource(
  private val wrapped: InputStream
) : ReadableStreamSource {
  override val type: ReadableStream.Type get() = ReadableStream.Type.BYOB

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    val byteController = controller as ReadableByteStreamController
    val desiredBytes = byteController.desiredSize?.toLong() ?: 0

    if (desiredBytes > 0) {
      val length = desiredBytes.toInt().coerceAtMost(wrapped.available())
      if (length > 0) {
        val buffer = ByteBuffer.wrap(wrapped.readNBytes(length))
        byteController.enqueue(ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, buffer))
      }
    }

    return super.pull(controller)
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    wrapped.close()
    return super.cancel(reason)
  }
}

@JvmInline internal value class ReadableStreamReaderSource(private val wrapped: Reader) : ReadableStreamSource {
  override val type: ReadableStream.Type get() = ReadableStream.Type.BYOB

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    val byteController = controller as ReadableByteStreamController
    val desiredBytes = byteController.desiredSize?.toLong() ?: 0

    if (desiredBytes > 0 && wrapped.ready()) {
      val length = desiredBytes.toInt()

      if (length > 0) {
        val buffer = ByteBuffer.allocate(length / 2)
        wrapped.read(buffer.asCharBuffer())

        byteController.enqueue(ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, buffer))
      }
    }

    return super.pull(controller)
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    wrapped.close()
    return super.cancel(reason)
  }
}

internal class ReadableStreamBufferSource(private val wrapped: ByteBuffer) : ReadableStreamSource {
  private val offset = AtomicInteger(wrapped.position())
  private val remaining = AtomicInteger(wrapped.remaining())

  override val type: ReadableStream.Type get() = ReadableStream.Type.BYOB

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    val byteController = controller as ReadableByteStreamController
    val desiredBytes = byteController.desiredSize?.toLong() ?: 0

    if (desiredBytes > 0 && remaining.get() > 0) {
      val length = desiredBytes.toInt().coerceAtMost(wrapped.remaining())
      val view = ArrayBufferViews.newView(
        type = ArrayBufferViewType.Uint8Array,
        buffer = wrapped,
        offset = offset.getAndAdd(length).toLong(),
        length = remaining.getAndAdd(-length).toLong(),
      )

      controller.enqueue(view)
    } else {
      controller.close()
    }
    return super.pull(controller)
  }
}

internal class ReadableStreamAsyncIteratorSource(private val wrapped: Value) : ReadableStreamSource {
  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    check(controller is ReadableStreamDefaultController)
    val nextPromise = JsPromise<Unit>()

    val next = runCatching { wrapped.invokeMember("next") }.getOrElse {
      return JsPromise.rejected(it)
    }


    (JsPromise.wrapOrNull(next, { it }) ?: JsPromise.resolved(next)).then(
      onFulfilled = {
        if (!it.hasMembers()) throw TypeError.create("Iterator returned a non-object value")
        if (it.getMember("done").asBoolean()) controller.close()
        else controller.enqueue(it.getMember("value"))
        nextPromise.resolve(Unit)
      },
      onCatch = nextPromise::reject,
    )
    return nextPromise
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    val iterator = wrapped.getMember("iterator")

    runCatching { iterator.invokeMember("return") }.getOrElse {
      return JsPromise.rejected(it)
    }

    return JsPromise.resolved(Unit)
  }
}

internal class ReadableStreamHostIteratorSource(private val wrapped: Iterator<Any>) : ReadableStreamSource {
  internal constructor(iterable: Iterable<Any>) : this(iterable.iterator())

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    controller as ReadableStreamDefaultController

    if (wrapped.hasNext()) controller.enqueue(Value.asValue(wrapped.next()))
    else controller.close()

    return JsPromise.resolved(Unit)
  }
}
