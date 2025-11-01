/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.http.server.netty

import io.netty.buffer.ByteBuf
import io.netty.util.concurrent.EventExecutor
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import elide.runtime.Logging
import elide.runtime.http.server.*

internal class NettyContentStream(
  private val eventLoop: EventExecutor,
  private var streamConsumer: ContentStreamConsumer? = null,
  private var streamProducer: ContentStreamSource? = null,
) : ReadableContentStream, WritableContentStream {
  private sealed interface StreamFrame {
    data object End : StreamFrame
    @JvmInline value class Data(val content: ByteBuf) : StreamFrame
  }

  private sealed interface StreamState {
    data object Open : StreamState
    data object Closing : StreamState
    @JvmInline value class Closed(val error: Throwable? = null) : StreamState
  }

  // noop reader used when a consumer fails to attach
  private data object InertReader : ReadableContentStream.Reader {
    override fun pull() = Unit
    override fun release() = Unit
  }

  // reader adapter that delegates calls to the stream on the event loop
  @JvmInline private value class StreamReader(private val body: NettyContentStream) : ReadableContentStream.Reader {
    override fun pull() = body.onPull()
    override fun release() = body.onClose(null)
  }

  // noop reader used when a producer fails to attach
  private data object InertWriter : WritableContentStream.Writer {
    override fun write(content: ByteBuf) = Unit
    override fun end(error: Throwable?) = Unit
  }

  // writer adapter that delegates calls to the stream on the event loop
  @JvmInline private value class StreamWriter(private val body: NettyContentStream) : WritableContentStream.Writer {
    override fun write(content: ByteBuf) = body.onPush(content)
    override fun end(error: Throwable?) = body.onEnd(error)
  }

  private val queue = LinkedList<StreamFrame>()

  @Volatile private var state: StreamState = StreamState.Open
  @Volatile private var demand = 0

  private val consumed = AtomicBoolean(false)
  private val sourced = AtomicBoolean(false)
  private val closed = AtomicBoolean(false)

  private inline fun runOnEventLoop(preferImmediate: Boolean = false, crossinline block: () -> Unit) {
    if (preferImmediate && eventLoop.inEventLoop()) block()
    else eventLoop.execute { block() }
  }

  // ---- inherited API surface ----

  override fun consume(consumer: ContentStreamConsumer) {
    if (consumed.compareAndSet(false, true)) onConsumerAttached(consumer)
    // stream is busy, close the consumer it unless it fails during the first callback
    else if (notifyConsumerAttached(consumer, InertReader)) notifyConsumerClosed(consumer, StreamBusyException())
  }

  override fun source(producer: ContentStreamSource) {
    if (sourced.compareAndSet(false, true)) onProducerAttached(producer)
    // stream is busy, close the producer it unless it fails during the first callback
    else if (notifyProducerAttached(producer, InertWriter)) notifyProducerClosed(producer, StreamBusyException())
  }

  override fun close(error: Throwable?) {
    if (!closed.compareAndSet(false, true)) return
    onClose(error)
  }

  // ---- event handling ----

  private fun onConsumerAttached(consumer: ContentStreamConsumer) = runOnEventLoop(preferImmediate = true) {
    when (val snap = state) {
      is StreamState.Closed -> {
        // stream is closed, detach the consumer immediately
        if (notifyConsumerAttached(consumer, InertReader))
          notifyConsumerClosed(consumer, StreamClosedException(snap.error))
        streamConsumer = null
      }

      StreamState.Open, StreamState.Closing -> if (streamProducer == null) {
        // if there is no producer, end immediately
        finalizeClose()
        if (notifyConsumerAttached(consumer, InertReader)) notifyConsumerClosed(consumer)
      } else {
        // either we're still open, or not accepting new data but the buffer still has some
        notifyConsumerAttached(consumer, StreamReader(this))
        streamConsumer = consumer
      }
    }
  }

  private fun onProducerAttached(producer: ContentStreamSource) = runOnEventLoop(preferImmediate = true) {
    when (val snap = state) {
      is StreamState.Closed, StreamState.Closing -> {
        // stream is either closed or ended, detach the producer immediately
        // (we won't accept new data beyond this point anyway)
        notifyProducerClosed(producer, StreamClosedException((snap as? StreamState.Closed)?.error))
        streamConsumer = null
      }

      StreamState.Open -> {
        notifyProducerAttached(producer, StreamWriter(this))
        streamProducer = producer
      }
    }
  }

  private fun onPush(content: ByteBuf) {
    if (closed.get()) return // early return to avoid scheduling

    runOnEventLoop {
      when (state) {
        StreamState.Open -> {
          // check if we can deliver the data directly to a waiting consumer;
          // otherwise leave it on the queue
          streamConsumer?.takeIf { demand > 0 }?.let {
            demand--
            notifyRead(it, content)

            // try pulling again if there's outstanding demand
            if (demand > 0) streamProducer?.let(::notifyPull)
          } ?: queue.add(StreamFrame.Data(content))
        }

        is StreamState.Closed, StreamState.Closing -> {
          // we're not accepting new data, discard it; this can happen if we
          // pass the closed check but the stream closes before the task runs
          content.release()
        }
      }
    }
  }

  private fun onPull() {
    if (closed.get()) return // early return to avoid scheduling

    runOnEventLoop {
      // proceed with the pull only if there's still a consumer; this can happen
      // if we pass the closed check but the stream closes before the task runs
      val consumer = streamConsumer ?: return@runOnEventLoop
      val producer = streamProducer // producer not required
      demand++ // accumulate demand

      when (val snap = state) {
        // no more data, surface any errors and detach
        is StreamState.Closed -> streamConsumer?.let {
          notifyConsumerClosed(it, StreamClosedException(snap.error))
          streamConsumer = null
        }

        // drain until we hit EOS or demand is satisfied
        StreamState.Open, StreamState.Closing -> while (demand > 0) when (val frame = queue.poll()) {
          is StreamFrame.Data -> {
            // stop early if the consumer throws
            if (notifyRead(consumer, frame.content)) demand--
            else break
          }

          StreamFrame.End -> {
            // we've hit the end of the stream, update the state and stop
            finalizeClose()
            notifyConsumerClosed(consumer)
            break
          }

          null -> {
            // request more data from an active producer if open, or
            // we're not getting any more data, detach
            if (snap is StreamState.Open && producer != null) notifyPull(producer)
            else notifyConsumerClosed(consumer)
            break
          }
        }
      }
    }
  }

  private fun onEnd(error: Throwable?) {
    if (closed.get()) return // early return to avoid scheduling

    runOnEventLoop {
      when (state) {
        StreamState.Open -> if (error == null && demand == 0) {
          // there's no demand and no error, add EOS mark
          state = StreamState.Closing
          queue.add(StreamFrame.End)
        } else {
          // there's an error, we need to interrupt the stream, note that
          // demand can never be positive here, because pulling without
          // a producer closes the stream automatically
          finalizeClose(error)
          streamConsumer?.let { notifyConsumerClosed(it, error) }
          streamProducer?.let { notifyProducerClosed(it, error) }
        }

        // noop, but log the error so it's not lost
        StreamState.Closing -> log.warn("Stream error signal received after closing: $error", error)
        is StreamState.Closed -> Unit // noop
      }
    }
  }

  private fun onClose(error: Throwable?) = runOnEventLoop(preferImmediate = false) {
    finalizeClose(error)

    streamConsumer?.let { notifyConsumerClosed(it, error) }
    streamConsumer = null

    streamProducer?.let { notifyProducerClosed(it, error) }
    streamProducer = null
  }

  // ---- consumer callbacks ----

  private fun notifyConsumerAttached(consumer: ContentStreamConsumer, reader: ReadableContentStream.Reader): Boolean {
    return runCatching { consumer.onAttached(reader); true }
      .recoverCatching { consumer.onClose(IllegalStateException("Consumer failed to attach: $it", it)); false }
      .getOrElse { log.error("Consumer failed to close after attach failure: $it", it); false }
  }

  private fun notifyConsumerClosed(consumer: ContentStreamConsumer, error: Throwable? = null) {
    runCatching { consumer.onClose(error) }.onFailure { cause ->
      log.error("Consumer failed to close: $cause", cause)
    }
  }

  private fun notifyRead(consumer: ContentStreamConsumer, content: ByteBuf): Boolean {
    val result = runCatching { consumer.onRead(content) }
      .onFailure { onClose(IllegalStateException("Consumer failed to read content: $it", it)) }

    content.release()
    return result.isSuccess
  }

  // ---- producer callbacks ----

  private fun notifyProducerAttached(producer: ContentStreamSource, writer: WritableContentStream.Writer): Boolean {
    return runCatching { producer.onAttached(writer); true }
      .recoverCatching { producer.onClose(IllegalStateException("Producer failed to attach: $it", it)); false }
      .getOrElse { log.error("Producer failed to close after attach failure: $it", it); false }
  }

  private fun notifyProducerClosed(producer: ContentStreamSource, error: Throwable? = null) {
    runCatching { producer.onClose(error) }
      .onFailure { cause -> log.error("Producer failed to close: $cause", cause) }
  }

  private fun notifyPull(producer: ContentStreamSource) {
    runCatching { producer.onPull() }
      .onFailure { onClose(IllegalStateException("Producer failed while pulling: $it", it)) }
  }

  // ---- helpers ----

  private fun finalizeClose(cause: Throwable? = null) {
    // clear the queue and release all remaining data
    while (true) queue.poll()?.let { if (it is StreamFrame.Data) it.content.release() } ?: break
    state = StreamState.Closed(cause)
  }

  private companion object {
    private val log = Logging.of(NettyContentStream::class.java)
  }
}
