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
import org.graalvm.polyglot.Value.asValue
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.ByteBuffer
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType.Uint8Array
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.ReadableStream.Type
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.stream.*
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@OptIn(DelicateElideApi::class)
@TestCase internal class ReadableStreamTest : AbstractJsIntrinsicTest<ReadableStreamIntrinsic>() {
  @Inject lateinit var executionProvider: GuestExecutorProvider

  private fun options(vararg pairs: Pair<String, Any>): Value {
    return asValue(ProxyObject.fromMap(mapOf(*pairs)))
  }

  override fun provide(): ReadableStreamIntrinsic {
    return ReadableStreamIntrinsic()
  }

  override fun testInjectable() {
    // noop
  }

  private inline fun pushSource(crossinline onPull: (ReadableStreamDefaultController) -> Unit): ReadableStreamSource {
    return object : ReadableStreamSource {
      override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
        onPull(controller as ReadableStreamDefaultController)
        return JsPromise.resolved(Unit)
      }
    }
  }

  private inline fun byteSource(crossinline onPull: (ReadableByteStreamController) -> Unit): ReadableStreamSource {
    return object : ReadableStreamSource {
      override val type: Type = Type.BYOB
      override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
        onPull(controller as ReadableByteStreamController)
        return JsPromise.resolved(Unit)
      }
    }
  }

  private inline fun <R> ReadableStream.useReader(block: (ReadableStreamDefaultReader) -> R): R {
    val reader = getReader() as ReadableStreamDefaultReader
    return runCatching { block(reader) }
      .also { reader.releaseLock() }
      .getOrThrow()
  }

  private fun ReadableStreamDefaultReader.readAsync(): Deferred<ReadableStream.ReadResult> = read().asDeferred()
  private suspend fun ReadableStreamDefaultReader.awaitRead(): ReadableStream.ReadResult = readAsync().await()

  @Test fun `should save chunks in queue when no pending reads`() = runTest {
    val source = pushSource { it.enqueue(asValue(1)) }
    val stream = ReadableDefaultStream(source, QueueingStrategy.DefaultReadStrategy, executionProvider.executor())

    val read = stream.useReader { reader -> reader.awaitRead() }
    assertEquals(1, (read.value as Value).asInt(), "expected chunk to have enqueued value")
    assertFalse(read.done, "expected chunk not to be the last")
  }

  @Test fun `should handle data from host push source`() = runTest {
    // simple host source (guest support TBD)
    val source = pushSource { controller ->
      // push-only, no backpressure control (for simplicity), last chunk should be marked
      // as final and close the stream when read
      controller.enqueue(asValue(1))
      controller.enqueue(asValue(2))
      controller.enqueue(asValue(3))
      controller.close()
    }

    // cast is needed on the host to access the APIs (for now, will be easier later)
    val stream = ReadableDefaultStream(source, QueueingStrategy.DefaultReadStrategy, executionProvider.executor())
    val reader = stream.getReader() as ReadableStreamDefaultReader

    // read from the stream sequentially until a chunk marked as 'done' is found
    val chunks = flow {
      do {
        val chunk = reader.read().asDeferred().await(); emit(chunk)
      } while (!chunk.done)
    }.toList()

    // inspect results
    assertEquals(3, chunks.size, "expected 3 values")
    repeat(chunks.size) { assertEquals(it + 1, (chunks[it].value as Value).asInt()) }
  }

  @Test fun `should handle BYOB sources`() = runTest {
    val read = withContext {
      enter()

      val source = byteSource { controller ->
        // direct write
        val request = controller.byobRequest
        if (request != null) {
          val view = checkNotNull(request.view) { "expected view to not be null" }

          val viewBuffer = ArrayBufferViews.getBackingBuffer(view)
          val viewOffset = ArrayBufferViews.getOffset(view)

          repeat(5) { viewBuffer.writeBufferByte(viewOffset + it, it.toByte()) }
          request.respond(5)
        }

        // indirect write (enqueue)
        val firstChunk = ByteBuffer.allocate(5)
        repeat(5) { firstChunk.put(it, (5 + it).toByte()) }
        controller.enqueue(ArrayBufferViews.newView(Uint8Array, firstChunk))
      }

      val stream = ReadableStream.create(source, QueueingStrategy.DefaultReadStrategy)
      val reader = stream.getReader(options("mode" to "byob")) as ReadableStreamBYOBReader

      val viewBuffer = ByteBuffer.allocate(12)
      val view = ArrayBufferViews.newView(Uint8Array, viewBuffer, offset = 2, length = 10)

      reader.read(view, options("min" to 10))
    }.asDeferred().await()

    val result = read.value
    assertIs<Value>(result, "expected result to be a polyglot value")

    val buffer = assertDoesNotThrow { ArrayBufferViews.getBackingBuffer(result) }
    val bytes = ByteArray(12)
    buffer.readBuffer(0, bytes, 0, 12)

    repeat(2) { assertEquals(0, bytes[it], "expected first two bytes to be 0") }
    repeat(10) { assertEquals(it.toByte(), bytes[2 + it], "expected byte at ${2 + it} to be $it") }
  }
}
