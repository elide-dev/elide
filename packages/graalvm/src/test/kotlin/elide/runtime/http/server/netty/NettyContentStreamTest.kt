/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.netty

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import elide.runtime.http.server.StreamBusyException
import elide.runtime.http.server.StreamClosedException

class NettyContentStreamTest : NettyStreamTest() {
  private lateinit var _channel: EmbeddedChannel

  override val channel: EmbeddedChannel
    get() = _channel

  @BeforeEach fun setup() {
    _channel = EmbeddedChannel()
  }

  @AfterEach fun teardown() {
    channel.finishAndReleaseAll()
  }

  private fun newStream(): NettyContentStream = NettyContentStream(channel.eventLoop())

  private fun bufferOf(text: String): ByteBuf = Unpooled.copiedBuffer(text, Charsets.UTF_8)

  private fun drainEventLoop() {
    channel.runPendingTasks()
  }

  @Test fun `should reject a second consumer attachment`() {
    val stream = newStream()
    val producer = RecordingProducer()
    stream.source(producer)
    drainEventLoop()

    assertEquals(1, producer.attachCount, "expected producer to be attached once")
    assertNotNull(producer.writer, "expected writer to be available after attach")

    val first = RecordingConsumer()
    stream.consume(first)
    drainEventLoop()

    assertEquals(1, first.attachCount, "expected first consumer to attach")
    assertEquals(0, first.closeCount, "expected first consumer to remain attached")

    val second = RecordingConsumer()
    stream.consume(second)
    drainEventLoop()

    assertEquals(1, second.attachCount, "expected second consumer to receive attach callback")
    assertEquals(1, second.closeCount, "expected second consumer to close immediately")
    assertIs<StreamBusyException>(second.closeCause, "expected busy error for second consumer")
  }

  @Test fun `should reject a second producer attachment`() {
    val stream = newStream()
    val first = RecordingProducer()
    stream.source(first)
    drainEventLoop()

    assertEquals(1, first.attachCount, "expected first producer to attach")
    assertNotNull(first.writer, "expected first producer to receive writer")

    val second = RecordingProducer()
    stream.source(second)
    drainEventLoop()

    assertEquals(1, second.attachCount, "expected second producer to receive attach callback")
    assertEquals(1, second.closeCount, "expected second producer to close immediately")
    assertIs<StreamBusyException>(second.closeCause, "expected busy error for second producer")
  }

  @Test fun `should release queued data when close races with pending write`() {
    val stream = newStream()
    val producer = RecordingProducer()
    stream.source(producer)
    drainEventLoop()

    val buffer = Unpooled.buffer(4).writeInt(42)
    producer.write(buffer)
    stream.close()
    drainEventLoop()

    assertEquals(0, buffer.refCnt(), "expected queued buffer to be released on close")
    assertEquals(1, producer.closeCount, "expected producer to be closed once")
    assertNull(producer.closeCause, "expected close to be graceful")
  }

  @Test fun `should close gracefully when pull is in flight`() {
    val stream = newStream()
    val producer = RecordingProducer()
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    val reader = consumer.reader
    assertNotNull(reader, "expected consumer reader to be available")

    reader.pull()
    stream.close()
    drainEventLoop()

    assertEquals(0, consumer.readCount, "expected no data to be delivered")
    assertEquals(1, consumer.closeCount, "expected consumer to be closed once")
    assertNull(consumer.closeCause, "expected consumer to close without error")

    assertEquals(1, producer.closeCount, "expected producer to close once")
    assertNull(producer.closeCause, "expected producer to close without error")
  }

  @Test fun `should deliver data following nominal flow`() {
    val stream = newStream()
    val producer = RecordingProducer()
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    val reader = consumer.reader
    assertNotNull(reader, "expected reader to be assigned")

    consumer.onReadAction = { content ->
      consumer.received += content.toString(Charsets.UTF_8)
      reader.pull()
    }

    producer.onPullAction = {
      producer.write(bufferOf("first"))
      producer.write(bufferOf("second"))
      producer.end()
    }

    reader.pull()
    drainEventLoop()

    assertEquals(listOf("first", "second"), consumer.received, "expected ordered delivery")
    assertEquals(1, consumer.closeCount, "expected consumer to close after EOS")
    assertNull(consumer.closeCause, "expected graceful close")
  }

  @Test fun `should handle consumer exception during attach`() {
    val stream = newStream()
    val producer = RecordingProducer()
    stream.source(producer)
    drainEventLoop()

    val consumer = RecordingConsumer().apply {
      onAttach = { throw IllegalStateException("boom") }
    }

    stream.consume(consumer)
    drainEventLoop()

    assertEquals(1, consumer.closeCount, "expected consumer to close after attach failure")
    val cause = consumer.closeCause
    assertIs<IllegalStateException>(cause)
    assertIs<IllegalStateException>(cause.cause)
  }

  @Test fun `should handle consumer exception during read`() {
    val stream = newStream()
    val producer = RecordingProducer()
    val consumer = RecordingConsumer().apply {
      onReadAction = { throw IllegalStateException("boom") }
    }

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    producer.onPullAction = { producer.write(bufferOf("payload")) }

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, consumer.readCount, "expected a single read before failure")
    assertEquals(1, consumer.closeCount, "expected consumer to close after failure")
    val cause = consumer.closeCause
    assertIs<IllegalStateException>(cause)
    assertIs<IllegalStateException>(cause.cause)

    assertEquals(1, producer.closeCount, "expected producer to be closed due to failure")
    assertIs<IllegalStateException>(producer.closeCause)
  }

  @Test fun `should handle producer exception during attach`() {
    val stream = newStream()
    val producer = RecordingProducer().apply {
      onAttach = { throw IllegalStateException("boom") }
    }

    stream.source(producer)
    drainEventLoop()

    assertEquals(1, producer.closeCount, "expected producer to close after attach failure")
    val cause = producer.closeCause
    assertIs<IllegalStateException>(cause)
    assertIs<IllegalStateException>(cause.cause)
  }

  @Test fun `should handle producer exception during pull`() {
    val stream = newStream()
    val producer = RecordingProducer().apply {
      onPullAction = { throw IllegalStateException("boom") }
    }
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, producer.pullCount, "expected a single pull attempt")
    assertEquals(1, producer.closeCount, "expected producer to close after pull failure")
    val producerCause = producer.closeCause
    assertIs<IllegalStateException>(producerCause)
    assertIs<IllegalStateException>(producerCause.cause)

    assertEquals(1, consumer.closeCount, "expected consumer to close on producer failure")
    val consumerCause = consumer.closeCause
    assertIs<IllegalStateException>(consumerCause)
    assertIs<IllegalStateException>(consumerCause.cause)
  }

  @Test fun `should surface stream errors to new consumers`() {
    val stream = newStream()
    val failure = IllegalStateException("boom")

    stream.close(failure)
    drainEventLoop()

    val consumer = RecordingConsumer()
    stream.consume(consumer)
    drainEventLoop()

    assertEquals(1, consumer.attachCount, "expected attach callback even when closed")
    assertEquals(1, consumer.closeCount, "expected consumer to close immediately")
    val cause = consumer.closeCause
    assertIs<StreamClosedException>(cause)
    assertEquals(failure, cause.cause, "expected original error to propagate")
  }

  @Test fun `should drain closing stream when consumer attaches after end`() {
    val stream = newStream()
    val producer = RecordingProducer()
    stream.source(producer)
    drainEventLoop()

    producer.write(bufferOf("first"))
    producer.write(bufferOf("second"))
    producer.end()
    drainEventLoop()

    val consumer = RecordingConsumer()
    stream.consume(consumer)
    drainEventLoop()

    val reader = consumer.reader
    assertNotNull(reader, "expected reader to be available")
    assertEquals(0, consumer.readCount, "expected data to be buffered until pull")

    repeat(3) {
      reader.pull()
      drainEventLoop()
    }

    assertEquals(listOf("first", "second"), consumer.received, "expected buffered data to be delivered")
    assertEquals(1, consumer.closeCount, "expected consumer to close after draining EOS")
    assertNull(consumer.closeCause, "expected graceful close")
  }

  @Test fun `should close with producer supplied error`() {
    val stream = newStream()
    val producerFailure = IllegalStateException("boom")
    val producer = RecordingProducer().apply {
      onPullAction = {
        write(bufferOf("payload"))
        end(producerFailure)
      }
    }
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, consumer.closeCount, "expected consumer to close after producer error")
    val cause = consumer.closeCause
    assertEquals(producerFailure, cause, "expected producer error to be surfaced directly")

    assertEquals(1, producer.closeCount, "expected producer to close after signaling error")
    assertEquals(producerFailure, producer.closeCause)
  }

  @Test fun `should close automatically once end marker is observed`() {
    val stream = newStream()
    val producer = RecordingProducer().apply {
      onPullAction = { end() }
    }
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, consumer.closeCount, "expected consumer to close after EOS pull")
    assertNull(consumer.closeCause, "expected graceful close")
  }

  @Test fun `should close automatically when consumer pulls without producer`() {
    val stream = newStream()
    val consumer = RecordingConsumer()

    stream.consume(consumer)
    drainEventLoop()

    assertEquals(1, consumer.attachCount, "expected consumer to attach once")
    assertEquals(1, consumer.closeCount, "expected consumer to close immediately without producer")
    assertNull(consumer.closeCause, "expected graceful close")
  }

  @Test fun `should close automatically when end called with waiting consumer`() {
    val stream = newStream()
    val producer = RecordingProducer().apply {
      onPullAction = { end() }
    }
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, consumer.closeCount, "expected consumer to close after end with demand")
    assertNull(consumer.closeCause)
  }

  @Test fun `should notify producer to pull when queue empty`() {
    val stream = newStream()
    val producer = RecordingProducer()
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    consumer.reader!!.pull()
    drainEventLoop()

    assertEquals(1, producer.pullCount, "expected producer to receive pull signal")
  }

  @Test fun `should accumulate demand across multiple pulls`() {
    val stream = newStream()
    val producer = RecordingProducer()
    val consumer = RecordingConsumer()

    stream.source(producer)
    stream.consume(consumer)
    drainEventLoop()

    val reader = consumer.reader
    assertNotNull(reader)

    reader.pull()
    reader.pull()
    drainEventLoop()

    assertEquals(2, producer.pullCount, "expected a pull for each demand signal")
  }

  @Test fun `should deliver buffered data only after consumer pulls when ended`() {
    val stream = newStream()
    val producer = RecordingProducer()
    stream.source(producer)
    drainEventLoop()

    producer.write(bufferOf("first"))
    producer.write(bufferOf("second"))
    producer.end()

    val consumer = RecordingConsumer()
    stream.consume(consumer)
    drainEventLoop()

    assertEquals(0, consumer.readCount, "expected no data delivery on attach")
    assertEquals(0, consumer.closeCount, "expected consumer to stay attached")

    val reader = consumer.reader
    assertNotNull(reader)

    reader.pull()
    drainEventLoop()
    reader.pull()
    drainEventLoop()
    reader.pull()
    drainEventLoop()

    assertEquals(listOf("first", "second"), consumer.received, "expected buffered data after pulls")
    assertEquals(1, consumer.closeCount, "expected consumer to close after draining buffered data")
    assertNull(consumer.closeCause)
  }
}
