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
package elide.runtime.intrinsics.server.http.v2.channels

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.HttpContent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.server.http.v2.HttpContentSink
import elide.runtime.intrinsics.server.http.v2.HttpContentSink.Handle
import elide.runtime.intrinsics.server.http.v2.HttpContentSink.Producer

/**
 * Adapter used to write content to a Netty channel using the [HttpContentSink] API.
 *
 * A [NettyHttpContextAdapter] will call this adapter's [maybePull] to request data to be written into the
 * [attachedContext]. The adapter will not continue to write automatically, [maybePull] must be called repeatedly
 * until the producer is exhausted.
 */
internal class NettyContextSink(private val attachedContext: ChannelHandlerContext) : HttpContentSink {
  /**
   * Wrapper around a consumer that ensures methods will not be called after release. Use [maybePull] to request more
   * data from the producer.
   */
  private inner class ContextBoundHandle(private val producer: Producer) : Handle {
    override fun push(content: HttpContent) {
      check(attachedHandle.get() == this) { "Producer is released or sink is already closed" }
      attachedContext.channel().writeAndFlush(content)
    }

    override fun release(close: Boolean) {
      if (!attachedHandle.compareAndSet(this, null)) return
      producer.released()

      if (close) close()
    }

    /** Request more data from the producer. This method ensures the handle is attached to the source first. */
    fun maybePull() {
      if (attachedHandle.get() != this) return
      producer.pull(this)
    }
  }

  /** Currently attached producer handle. */
  private val attachedHandle = AtomicReference<ContextBoundHandle>()

  /** Atomic flag to indicate sink closing. */
  private val closed = AtomicBoolean()

  /**
   * If there is a producer attached to this sink, request more data from it. Returns whether the sink remains open
   * after the call and further pull calls will be accepted,
   *
   * @return `true` if the sink is still open after the call, `false` otherwise.
   */
  fun maybePull(): Boolean {
    if (closed.get()) return false
    attachedHandle.get()?.maybePull()

    return !closed.get()
  }

  override fun source(producer: Producer) {
    check(!closed.get()) { "Sink is already closed" }

    val handle = ContextBoundHandle(producer)
    check(attachedHandle.compareAndSet(null, handle)) { "Sink already has an attached producer" }
  }

  override fun release() {
    attachedHandle.get()?.release()
  }

  override fun close() {
    if (!closed.compareAndSet(false, true)) return
    release()
  }
}
