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
import elide.runtime.intrinsics.js.stream.ReadableStreamController
import elide.runtime.intrinsics.js.stream.ReadableStreamDefaultController
import elide.runtime.intrinsics.js.stream.ReadableStreamSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import org.graalvm.polyglot.Value
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViewType
import elide.runtime.gvm.internals.intrinsics.js.ArrayBufferViews
import elide.runtime.intrinsics.js.ReadableStream
import elide.runtime.intrinsics.js.stream.ReadableByteStreamController

/**
 * A simple source implementation that provides written values through a [Channel]. The sink itself can be used as a
 * [SendChannel] by test code to provide the chunks read from the source. The underlying channel will be closed when
 * the source is cancelled.
 */
internal class ChannelSource private constructor(
  private val channel: Channel<Value>,
  private val scope: CoroutineScope,
) : ReadableStreamSource, SendChannel<Value> by channel {
  /** Construct a new channel sink. */
  constructor(context: CoroutineContext, capacity: Int = Channel.Factory.UNLIMITED) : this(
    channel = Channel<Value>(capacity),
    scope = CoroutineScope(context + SupervisorJob()),
  )

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    check(controller is ReadableStreamDefaultController)
    val promise = JsPromise<Unit>()
    scope.launch {
      runCatching { controller.enqueue(channel.receive()) }
        .onSuccess { promise.resolve(Unit) }
        .onFailure { promise.reject(it) }
    }
    return promise
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    channel.cancel(reason?.let { kotlin.coroutines.cancellation.CancellationException(it.toString()) })
    return JsPromise.Companion.resolved(Unit)
  }
}

/**
 * A simple source implementation that provides written values through a [Channel]. The sink itself can be used as a
 * [SendChannel] by test code to provide the chunks read from the source. The underlying channel will be closed when
 * the source is cancelled.
 */
internal class ChannelByteSource private constructor(
  private val channel: Channel<ByteArray>,
  private val scope: CoroutineScope,
) : ReadableStreamSource, SendChannel<ByteArray> by channel {
  /** Construct a new channel sink. */
  constructor(context: CoroutineContext, capacity: Int = Channel.Factory.UNLIMITED) : this(
    channel = Channel<ByteArray>(capacity),
    scope = CoroutineScope(context + SupervisorJob()),
  )

  override val type: ReadableStream.Type
    get() = ReadableStream.Type.BYOB

  override fun pull(controller: ReadableStreamController): JsPromise<Unit> {
    check(controller is ReadableByteStreamController)
    val promise = JsPromise<Unit>()
    scope.launch {
      val view = ArrayBufferViews.newView(ArrayBufferViewType.Uint8Array, ByteBuffer.wrap(channel.receive()))
      runCatching { controller.enqueue(view) }
        .onSuccess { promise.resolve(Unit) }
        .onFailure { promise.reject(it) }
    }
    return promise
  }

  override fun cancel(reason: Any?): JsPromise<Unit> {
    channel.cancel(reason?.let { kotlin.coroutines.cancellation.CancellationException(it.toString()) })
    return JsPromise.Companion.resolved(Unit)
  }
}

@Suppress("TestFunctionName")
internal fun CoroutineScope.ChannelSource(capacity: Int = Channel.UNLIMITED): ChannelSource {
  return ChannelSource(coroutineContext, capacity)
}

@Suppress("TestFunctionName")
internal fun CoroutineScope.ChannelByteSource(capacity: Int = Channel.UNLIMITED): ChannelByteSource {
  return ChannelByteSource(coroutineContext, capacity)
}
