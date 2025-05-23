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

import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
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

    if (desiredBytes > 0) {
      val length = desiredBytes.toInt().coerceAtMost(wrapped.remaining())
      val view = ArrayBufferViews.newView(
        type = ArrayBufferViewType.Uint8Array,
        buffer = wrapped,
        offset = offset.getAndAdd(length).toLong(),
        length = remaining.getAndAdd(-length).toLong(),
      )

      controller.enqueue(view)
    }

    if (remaining.get() == 0) controller.close()
    return super.pull(controller)
  }
}
