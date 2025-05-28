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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.WritableStream
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultWriter
import elide.runtime.intrinsics.js.stream.WritableStreamSink
import elide.testing.annotations.TestCase
import elide.runtime.plugins.js.JavaScript as JS

@OptIn(DelicateElideApi::class)
@TestCase internal class WritableStreamTest : AbstractJsIntrinsicTest<WritableStreamIntrinsic>() {
  /** Intrinsic installer. */
  @Inject lateinit var intrinsic: WritableStreamIntrinsic

  override fun provide(): WritableStreamIntrinsic = intrinsic
  override fun testInjectable() {
    assertNotNull(intrinsic)
  }

  @Test fun `should allow creating writable streams`() = runTest {
    // host-side construction
    assertDoesNotThrow("expected stream to be created from host call") {
      WritableStream.create(WritableStreamSink.DiscardingSink)
    }

    // guest-side construction
    executeGuest {
      bindings(JS).putMember("TestSink", WritableStreamSink.DiscardingSink)
      "new WritableStream(TestSink)"
    }.thenAssert {
      it.doesNotFail()

      val stream = assertNotNull(it.returnValue())
      assertDoesNotThrow("expected a writable stream instance") { stream.asProxyObject<WritableDefaultStream>() }
    }
  }

  @Test fun `should manage writer acquisition`() = runTest {
    executeGuest { "(stream) => new WritableStreamDefaultWriter(stream)" }.thenAssert { test ->
      val stream = WritableStream.create(WritableStreamSink.DiscardingSink)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // direct construction
      val directWriter = assertDoesNotThrow("expected direct writer constructor to succeed") {
        guestCall.execute(stream).asProxyObject<WritableStreamDefaultWriter>()
      }

      // illegal construction due to lock
      assertFails("expected writer constructor to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal construction after release
      directWriter.releaseLock()
      assertDoesNotThrow("expected writer constructor to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<WritableStreamDefaultWriter>()
      }
    }

    executeGuest { "(stream) => stream.getWriter()" }.thenAssert { test ->
      val stream = WritableStream.create(WritableStreamSink.DiscardingSink)
      val guestCall = assertNotNull(test.returnValue()).also { assert(it.canExecute()) }

      // indirect acquisition
      val acquiredWriter = assertDoesNotThrow("expected writer acquisition to succeed") {
        guestCall.execute(stream).asProxyObject<WritableStreamDefaultWriter>()
      }

      // illegal acquisition due to lock
      assertFails("expected writer acquisition to fail using locked stream") {
        guestCall.execute(stream)
      }

      // legal acquisition after release
      acquiredWriter.releaseLock()
      assertDoesNotThrow("expected writer acquisition to succeed with released stream") {
        guestCall.execute(stream).asProxyObject<WritableStreamDefaultWriter>()
      }
    }
  }

  @Test fun `should write to host sink`() = runTest {
    val sink = ChannelSink()
    val stream = WritableStream.create(sink)

    // write from host
    val hostWriter = stream.getWriter()
    hostWriter.write(Value.asValue("host"))
    hostWriter.releaseLock()

    assertEquals(
      expected = "host",
      actual = sink.receive().asString(),
    )

    // write from guest
    executeGuest {
      bindings(JS).putMember("TestStream", stream)
      """
      const writer = TestStream.getWriter();
      writer.write('guest');
      writer.releaseLock();
      """.trimIndent()
    }.doesNotFail()

    assertEquals(
      expected = "guest",
      actual = sink.receive().asString(),
    )
  }

  @Test fun `should write to guest sink`() = runTest {
    val channel = Channel<Value>(Channel.UNLIMITED)
    val sendAdapter = ProxyExecutable { channel.trySend(it.single()) }

    executeGuest {
      bindings(JS).putMember("SendAdapter", sendAdapter)

      """
      const stream = new WritableStream({ write: async (chunk) => SendAdapter(chunk) });
      const writer = stream.getWriter();
      writer.write('guest');
      """.trimIndent()
    }.doesNotFail()

    assertEquals(
      expected = "guest",
      actual = channel.receive().asString(),
    )
  }

  @Test fun `should communicate backpressure`() = runTest {
    val sink = ChannelSink(capacity = Channel.RENDEZVOUS)
    val stream = WritableStream.create(sink)
    val writer = stream.getWriter()

    var backpressureSet = false
    val backpressureMonitor = launch {
      while (isActive) {
        backpressureSet = true
        writer.ready.asDeferred().await()
        backpressureSet = false
        // allow test code to affect backpressure
        yield()
      }
    }

    // sink is empty, the first read should stay in-flight until we read from the sink;
    // backpressure should be set to 'true', refreshing the promise and suspending the counter
    val writePromise = writer.write(Value.asValue("value"))

    yield() // by yielding here we allow the counter to prove us wrong
    assert(backpressureSet)

    // clear the sink, backpressure should be back to 'false', settling the promise
    assertEquals("value", sink.receive().asString())

    // wait for the write to finish, the additional yield is required to ensure
    // that the counter is allowed to run
    writePromise.asDeferred().await()
    yield()

    assertFalse(backpressureSet)
    backpressureMonitor.cancel()
  }
}

