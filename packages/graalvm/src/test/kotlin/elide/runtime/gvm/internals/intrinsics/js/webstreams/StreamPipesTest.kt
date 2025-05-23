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
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.stream.*
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@OptIn(DelicateElideApi::class)
@TestCase internal class StreamPipesTest : AbstractJsIntrinsicTest<ReadableStreamIntrinsic>() {
  override fun provide(): ReadableStreamIntrinsic {
    return ReadableStreamIntrinsic()
  }

  override fun testInjectable() {
    // noop
  }

  @Test fun `should pipe readable stream to writable stream`() = runTest {
    val output = Channel<Value>(Channel.UNLIMITED)

    val source = object : ReadableStreamSource {
      private val counter = AtomicInteger(0)

      override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
        val next = counter.incrementAndGet()

        (controller as ReadableStreamDefaultController).enqueue(Value.asValue(next))
        if (next == 5) controller.close()

        return JsPromise.resolved(Unit)
      }
    }

    val sink = object : WritableStreamSink {
      override fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
        output.trySend(chunk)
        return JsPromise.resolved(Unit)
      }
    }

    val writable = WritableDefaultStream(sink)
    val readable = ReadableStream.create(source)

    readable.pipeTo(writable)

    output.receiveAsFlow().take(5).collectIndexed { index, value ->
      assertEquals(index + 1, value.asInt())
    }
  }
}
