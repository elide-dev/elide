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
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.graalvm.polyglot.proxy.ProxyObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.ByteBuffer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType.Uint8Array
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.TransformStream
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.*
import elide.testing.annotations.TestCase
import elide.runtime.plugins.js.JavaScript as JS

@OptIn(DelicateElideApi::class)
@TestCase internal class ReadableStreamTest : AbstractJsIntrinsicTest<ReadableStreamIntrinsic>() {
  @Inject lateinit var intrinsic: ReadableStreamIntrinsic

  override fun provide(): ReadableStreamIntrinsic = intrinsic
  override fun testInjectable() {
    assertNotNull(intrinsic)
  }

  @Test fun `should allow creating readable streams`() = runTest {
    val defaultSource = object : ReadableStreamSource {
      override val type: ReadableStream.Type = ReadableStream.Type.Default
    }

    val byobSource = object : ReadableStreamSource {
      override val type: ReadableStream.Type = ReadableStream.Type.BYOB
    }

    // host-side construction (default)
    assertDoesNotThrow("expected default stream to be created from host call") {
      ReadableStream.create(defaultSource)
    }.also { stream -> assertIs<ReadableDefaultStream>(stream) }

    // host-side construction (byob)
    assertDoesNotThrow("expected byte stream to be created from host call") {
      ReadableStream.create(byobSource)
    }.also { stream -> assertIs<ReadableByteStream>(stream) }

    // guest-side construction (default)
    executeGuest {
      bindings(JS).putMember("TestSource", defaultSource)
      "new ReadableStream(TestSource)"
    }.thenAssert {
      it.doesNotFail()

      val stream = assertNotNull(it.returnValue())
      assertDoesNotThrow("expected a default readable stream") { stream.asProxyObject<ReadableDefaultStream>() }
    }

    // guest-side construction (byob)
    executeGuest {
      bindings(JS).putMember("TestSource", byobSource)
      "new ReadableStream(TestSource)"
    }.thenAssert {
      it.doesNotFail()

      val stream = assertNotNull(it.returnValue())
      assertDoesNotThrow("expected a readable byte stream") { stream.asProxyObject<ReadableByteStream>() }
    }
  }

  @Test fun `should manage default reader acquisition`() = runTest {
    // default source
    executeGuest { "(stream) => new ReadableStreamDefaultReader(stream)" }.thenAssert { test ->
      val stream = ReadableStream.create(ReadableStreamSource.Empty)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // direct construction
      val directReader = assertDoesNotThrow("expected direct reader constructor to succeed") {
        guestCall.execute(stream).asProxyObject<ReadableStreamDefaultReader>()
      }

      // illegal construction due to lock
      assertFails("expected reader constructor to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal construction after release
      directReader.releaseLock()
      assertDoesNotThrow("expected reader constructor to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<ReadableStreamDefaultReader>()
      }
    }

    executeGuest { "(stream) => stream.getReader()" }.thenAssert { test ->
      val stream = ReadableStream.create(ReadableStreamSource.Empty)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // indirect acquisition
      val acquiredReader = assertDoesNotThrow("expected reader acquisition to succeed") {
        guestCall.execute(stream).asProxyObject<ReadableStreamDefaultReader>()
      }

      // illegal acquisition due to lock
      assertFails("expected reader acquisition to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal acquisition after release
      acquiredReader.releaseLock()
      assertDoesNotThrow("expected reader acquisition to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<ReadableStreamDefaultReader>()
      }
    }
  }

  @Test fun `should manage byob reader acquisition`() = runTest {
    val byobSource = object : ReadableStreamSource {
      override val type: ReadableStream.Type get() = ReadableStream.Type.BYOB
    }

    executeGuest { "(stream) => new ReadableStreamBYOBReader(stream, { mode: 'byob' })" }.thenAssert { test ->
      val stream = ReadableStream.create(byobSource)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // direct construction
      val directReader = assertDoesNotThrow("expected direct reader constructor to succeed") {
        guestCall.execute(stream).asProxyObject<ReadableStreamBYOBReader>()
      }

      // illegal construction due to lock
      assertFails("expected reader constructor to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal construction after release
      directReader.releaseLock()
      assertDoesNotThrow("expected reader constructor to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<ReadableStreamBYOBReader>()
      }
    }

    executeGuest { "(stream) => stream.getReader({ mode: 'byob' })" }.thenAssert { test ->
      val stream = ReadableStream.create(byobSource)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // indirect acquisition
      val acquiredReader = assertDoesNotThrow("expected reader acquisition to succeed") {
        guestCall.execute(stream).asProxyObject<ReadableStreamBYOBReader>()
      }

      // illegal acquisition due to lock
      assertFails("expected reader acquisition to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal acquisition after release
      acquiredReader.releaseLock()
      assertDoesNotThrow("expected reader acquisition to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<ReadableStreamBYOBReader>()
      }
    }
  }

  @Test fun `should read from default host source`() = runTest {
    val source = ChannelSource()
    val stream = ReadableStream.create(source)

    source.send(Value.asValue(42))
    val reader = stream.getReader() as ReadableStreamDefaultReader
    val result = reader.read().asDeferred().await()
    assertEquals(42, result.value?.asInt())
    assertFalse(result.done)
  }

  @Test fun `should read from default guest source`() = runTest {
    val result = CompletableDeferred<Value?>()
    val channel = Channel<Value>(Channel.UNLIMITED)

    val readAdapter = ProxyExecutable { channel.tryReceive().getOrThrow() }
    channel.send(Value.asValue("guest"))

    executeGuest {
      bindings(JS).putMember("ReadAdapter", readAdapter)

      """
      new ReadableStream({ pull: async (controller) => controller.enqueue(ReadAdapter()) })
      """.trimIndent()
    }.thenAssert { test ->
      val stream = assertNotNull(test.returnValue()).asProxyObject<ReadableStream>()
      val reader = stream.getReader() as ReadableStreamDefaultReader

      reader.read().then(
        onFulfilled = { result.complete(it.value) },
        onCatch = { result.completeExceptionally(TypeError.create(it.toString())) },
      )
    }

    assertEquals(
      expected = "guest",
      actual = result.await()?.asString(),
    )
  }

  @Test fun `should observe backpressure`() = runTest {
    val source = ChannelSource(capacity = Channel.RENDEZVOUS)
    val strategy = CountQueuingStrategy(1.0)

    var sent = 0
    val counter = launch {
      while (isActive) {
        source.send(Value.asValue(sent))
        sent++
      }
    }

    // after allowing the producer to run, it should suspend due to a
    // lack of consumers, since this is a pull source, not a push one
    yield()
    assertEquals(0, sent)

    val stream = ReadableStream.create(source, strategy)
    val reader = stream.getReader() as ReadableStreamDefaultReader

    // upon creation, the stream should pull one value from the source,
    // and we'll see it come out the other side through the reader
    yield() // allow the source to read from the channel
    yield() // allow the counter to tick
    assertEquals(1, sent)

    assertEquals(
      expected = 0,
      actual = reader.read().asDeferred().await().value?.asInt(),
    )

    // after the value is read, the stream will pull again to reach the
    // high watermark specified by the queueing strategy
    yield() // allow the source to read from the channel
    yield() // allow the counter to tick
    assertEquals(2, sent)
    counter.cancel()
  }

  @Test fun `should read from byob host source`() = runTest {
    // ensure there is a "current" context in the test thread; this is needed to create
    // the typed array instances when interacting with the BYOB stream
    val context = engine.acquire()
    context.enter()

    val source = ChannelByteSource()
    val stream = ReadableStream.create(source)

    // default mode
    assertDoesNotThrow {
      val helloBytes = "Hello".encodeToByteArray()
      source.send(helloBytes)

      val reader = stream.getReader() as ReadableStreamDefaultReader
      val result = assertNotNull(reader.read().asDeferred().await().value)

      val resultBytes = ByteArray(helloBytes.size)
      ArrayBufferViews.getBackingBuffer(result).readBuffer(0, resultBytes, 0, resultBytes.size)

      assertEquals("Hello", resultBytes.decodeToString())
      reader.releaseLock()
    }

    // byob mode (full chunk)
    assertDoesNotThrow {
      val helloBytes = "Hello".encodeToByteArray()
      source.send(helloBytes)

      val options = ProxyObject.fromMap(mapOf("mode" to "byob"))
      val reader = stream.getReader(Value.asValue(options)) as ReadableStreamBYOBReader

      val resultBytes = ByteArray(helloBytes.size)
      assertNotEquals("Hello", resultBytes.decodeToString())

      val resultView = ArrayBufferViews.newView(Uint8Array, ByteBuffer.wrap(resultBytes))
      assertNotNull(reader.read(resultView).asDeferred().await().value)

      assertEquals("Hello", resultBytes.decodeToString())
      reader.releaseLock()
    }

    // byob mode (stitched chunks)
    assertDoesNotThrow {
      val helloBytes = "Hello".encodeToByteArray()
      source.send(helloBytes)
      source.send(helloBytes)

      val options = ProxyObject.fromMap(mapOf("mode" to "byob"))
      val reader = stream.getReader(Value.asValue(options)) as ReadableStreamBYOBReader

      val stitchedBytes = ByteArray(helloBytes.size * 2)
      assertNotEquals("HelloHello", stitchedBytes.decodeToString())

      val resultView = ArrayBufferViews.newView(Uint8Array, ByteBuffer.wrap(stitchedBytes))
      val readOptions = ProxyObject.fromMap(mapOf("min" to stitchedBytes.size))
      assertNotNull(reader.read(resultView, Value.asValue(readOptions)).asDeferred().await().value)

      assertEquals("HelloHello", stitchedBytes.decodeToString())
      reader.releaseLock()
    }

    // byob mode (split chunk)
    assertDoesNotThrow {
      val helloBytes = "Hello".encodeToByteArray()
      source.send(helloBytes + helloBytes)

      val options = ProxyObject.fromMap(mapOf("mode" to "byob"))
      val readOptions = ProxyObject.fromMap(mapOf("min" to helloBytes.size))
      val reader = stream.getReader(Value.asValue(options)) as ReadableStreamBYOBReader

      val firstHelloBytes = ByteArray(helloBytes.size)
      assertNotEquals("Hello", firstHelloBytes.decodeToString())

      val firstResultView = ArrayBufferViews.newView(Uint8Array, ByteBuffer.wrap(firstHelloBytes))
      assertNotNull(reader.read(firstResultView, Value.asValue(readOptions)).asDeferred().await().value)
      assertEquals("Hello", firstHelloBytes.decodeToString())

      val secondHelloBytes = ByteArray(helloBytes.size)
      assertNotEquals("Hello", secondHelloBytes.decodeToString())

      val resultView = ArrayBufferViews.newView(Uint8Array, ByteBuffer.wrap(secondHelloBytes))
      assertNotNull(reader.read(resultView, Value.asValue(readOptions)).asDeferred().await().value)
      assertEquals("Hello", secondHelloBytes.decodeToString())

      reader.releaseLock()
    }

    // remember to explicitly leave the context! if you don't, other tests may break;
    // once the Elide testing APIs are refactored, we can remove the need for this
    context.leave()
  }

  @Test fun `should forward values to tee branches`() = runTest {
    val source = ChannelSource()
    val stream = ReadableStream.create(source)

    val (branchA, branchB) = stream.tee()

    source.send(Value.asValue(1))
    assertEquals(1, (branchA.getReader() as ReadableStreamDefaultReader).read().asDeferred().await().value?.asInt())
    assertEquals(1, (branchB.getReader() as ReadableStreamDefaultReader).read().asDeferred().await().value?.asInt())
  }

  @Test fun `should pipe stream to output`() = runTest {
    val source = ChannelSource()
    val stream = ReadableStream.create(source)

    val sink = ChannelSink()
    val output = WritableStream.create(sink)

    stream.pipeTo(output)

    source.send(Value.asValue(42))
    assertEquals(42, sink.receive().asInt())
  }

  @Test fun `should pipe stream through transform`() = runTest {
    val source = ChannelSource()
    val stream = ReadableStream.create(source)

    val transform = TransformStream.create(TransformStreamTransformer.Identity)
    stream.pipeThrough(transform)

    source.send(Value.asValue(42))
    val reader = transform.readable.getReader() as ReadableStreamDefaultReader

    assertEquals(
      expected = 42,
      actual = reader.read().asDeferred().await().value?.asInt(),
    )
  }
}

