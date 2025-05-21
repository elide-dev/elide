package elide.runtime.gvm.internals.intrinsics.js.webstreams

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.stream.QueueingStrategy
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultController
import elide.runtime.intrinsics.js.stream.WritableStreamSink
import elide.testing.annotations.TestCase

@OptIn(DelicateElideApi::class)
@TestCase internal class WritableStreamTest : AbstractJsIntrinsicTest<WritableStreamIntrinsic>() {
  override fun provide(): WritableStreamIntrinsic {
    return WritableStreamIntrinsic()
  }

  override fun testInjectable() {
    // noop
  }

  private fun channelSink(channel: SendChannel<Any?>): WritableStreamSink = object : WritableStreamSink {
    // reserved
    override val type: Any get() = Unit

    override fun write(chunk: Any?, controller: WritableStreamDefaultController): JsPromise<Unit> {
      assert(channel.trySend(chunk).isSuccess) { "Failed to consume chunk $chunk" }
      return JsPromise.resolved(Unit)
    }

    override fun close(): JsPromise<Unit> {
      channel.close()
      return JsPromise.resolved(Unit)
    }

    override fun abort(reason: Any?): JsPromise<Unit> {
      channel.close(TypeError.create(reason.toString()))
      return JsPromise.resolved(Unit)
    }
  }

  @Test fun `should handle write`() = runTest {
    val sinkChannel = Channel<Any?>(capacity = Channel.UNLIMITED)
    val channelSink = channelSink(sinkChannel)

    val stream = WritableDefaultStream(channelSink, QueueingStrategy.Default)
    val writer = stream.getWriter()

    val writes = List(5) {
      writer.write(it).catch { reason -> fail("Write promise was rejected: $reason") }
    }

    repeat(writes.size) { i ->
      assertEquals(expected = i, actual = sinkChannel.tryReceive().getOrThrow())
      assert(writes[i].isDone)
    }
  }
}
