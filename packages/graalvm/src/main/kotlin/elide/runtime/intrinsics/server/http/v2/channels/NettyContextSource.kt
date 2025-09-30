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
import io.netty.handler.codec.http.LastHttpContent
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import elide.runtime.intrinsics.server.http.v2.HttpContentSource
import elide.runtime.intrinsics.server.http.v2.HttpContentSource.Consumer
import elide.runtime.intrinsics.server.http.v2.HttpContentSource.Handle

/**
 * Adapter used to read content from a Netty channel using the [HttpContentSource] API.
 *
 * When a consumer calls [Handle.pull] and the internal buffer is empty, the [attachedContext]'s
 * [ChannelHandlerContext.read] method will be called to request new data.
 *
 * A [NettyHttpContextAdapter] will call the source's [handleRead] method to deliver new data received through the
 * Netty pipeline. If no consumer is active when data is received, it will be sent to the internal buffer.
 */
internal class NettyContextSource(private val attachedContext: ChannelHandlerContext) : HttpContentSource {
  /**
   * Wrapper around a consumer that ensures methods will not be called after release. Use [deliver] to send data to the
   * consumer.
   *
   * When a [pull] call succeeds, [pulled] will be set to `true` and [ChannelHandlerContext.read] will be called on
   * the [attachedContext] to initiate a new read sequence. If the source's [buffer] is not empty, [pull] will instead
   * consume the first item in it without requesting new data from the context.
   */
  private inner class ContextBoundHandle(private val consumer: Consumer) : Handle {
    override fun pull() {
      check(attachedHandle.get() == this) { "Consumer is released or source is already closed" }
      if (!pulled.compareAndSet(false, true)) return

      // pull from the buffer first, schedule delivery
      buffer.removeFirstOrNull()?.let { content ->
        attachedContext.executor().submit {
          consumer.consume(content, this)
          if (closed.get() && buffer.isEmpty()) release()
        }

        pulled.set(false)
        return
      }

      attachedContext.channel().read()
    }

    override fun release() {
      if (!attachedHandle.compareAndSet(this, null)) return
      consumer.released()
    }

    /** Send [content] to the consumer. This method ensures the handle is attached to the source before sending. */
    fun deliver(content: HttpContent) = consumer.consume(content, this)
  }

  /** Backup queue used to store incoming data when no consumer is attached. */
  private val buffer = mutableListOf<HttpContent>()

  /** Reference to the currently attached consumer handle; always `null` if the source is closed. */
  private val attachedHandle = AtomicReference<ContextBoundHandle>()

  /** Whether this source is closed and should no longer allow consumers or data delivery calls. */
  private val closed = AtomicBoolean()

  /** Atomic flag set by [Handle.pull] and cleared when data arrives via [handleRead]. */
  private val pulled = AtomicBoolean()

  /**
   * Whether this source is expecting content to be delivered through [handleRead]. Returns `true` after a consumer
   * calls [Handle.pull] until [handleRead] is next called.
   */
  fun shouldRead(): Boolean = pulled.get() || closed.get()

  /**
   * Handle an incoming [content] chunk and deliver it to the attached consumer. If the specified [content] is the
   * [LastHttpContent], the source will be closed after delivery.
   *
   * @return `true` if the source remains open after the call, `false` otherwise.
   */
  fun handleRead(content: HttpContent): Boolean {
    if (closed.get()) return false

    pulled.set(false)
    attachedHandle.get()?.deliver(content) ?: buffer.add(content)

    return (content !is LastHttpContent).also { moreExpected -> if (!moreExpected) close() }
  }

  override fun sink(consumer: Consumer) {
    check(!closed.get() || buffer.isNotEmpty()) { "Source is closed" }

    val handle = ContextBoundHandle(consumer)
    check(attachedHandle.compareAndSet(null, handle)) { "Source already has an attached consumer" }
    consumer.attached(handle)
  }

  override fun release() {
    attachedHandle.get()?.release()
  }

  override fun close() {
    if (!closed.compareAndSet(false, true)) return
    if (buffer.isEmpty()) release()
  }
}
