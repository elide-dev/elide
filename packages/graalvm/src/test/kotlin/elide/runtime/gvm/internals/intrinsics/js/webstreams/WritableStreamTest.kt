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

    override fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
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

    val stream = WritableDefaultStream(channelSink, QueueingStrategy.DefaultReadStrategy)
    val writer = stream.getWriter()

    val writes = List(5) {
      writer.write(Value.asValue(it)).catch { reason -> fail("Write promise was rejected: $reason") }
    }

    repeat(writes.size) { i ->
      assertEquals(expected = i, actual = (sinkChannel.tryReceive().getOrThrow() as Value).asInt())
      assert(writes[i].isDone)
    }
  }
}
