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

import elide.runtime.intrinsics.js.JsPromise
import elide.runtime.intrinsics.js.stream.WritableStreamDefaultController
import elide.runtime.intrinsics.js.stream.WritableStreamSink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import org.graalvm.polyglot.Value
import kotlin.coroutines.CoroutineContext

/**
 * A simple sink implementation that sends written values through a [Channel]. The sink itself can be used as a
 * [ReceiveChannel] by test code to read the chunks written to it. The underlying channel will be closed when the
 * sink is closed or aborted.
 */
internal class ChannelSink private constructor(
  private val channel: Channel<Value>,
  private val scope: CoroutineScope,
) : WritableStreamSink, ReceiveChannel<Value> by channel {
  /** Construct a new channel sink. */
  constructor(context: CoroutineContext, capacity: Int = Channel.Factory.UNLIMITED) : this(
    channel = Channel<Value>(capacity),
    scope = CoroutineScope(context + SupervisorJob()),
  )

  override fun write(chunk: Value, controller: WritableStreamDefaultController): JsPromise<Unit> {
    val promise = JsPromise<Unit>()
    scope.launch {
      runCatching { channel.send(chunk) }
        .onSuccess { promise.resolve(Unit) }
        .onFailure { promise.reject(it) }
    }
    return promise
  }

  override fun close(): JsPromise<Unit> {
    channel.close()
    scope.cancel()
    return JsPromise.Companion.resolved(Unit)
  }

  override fun abort(reason: Any?): JsPromise<Unit> {
    val cause = kotlin.coroutines.cancellation.CancellationException("Sink was aborted with reason: $reason")
    channel.close(cause)
    scope.cancel(cause)
    return JsPromise.Companion.resolved(Unit)
  }
}

@Suppress("TestFunctionName")
internal fun CoroutineScope.ChannelSink(capacity: Int = Channel.UNLIMITED): ChannelSink {
  return ChannelSink(coroutineContext, capacity)
}
