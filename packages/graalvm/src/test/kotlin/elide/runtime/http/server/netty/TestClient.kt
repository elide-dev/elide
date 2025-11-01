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

import io.netty.channel.Channel
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.http.*
import io.netty.util.ReferenceCountUtil
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.channels.ClosedChannelException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals

@Sharable class TestClient : ChannelInboundHandlerAdapter(), Closeable {
  private val context = AtomicReference<ChannelHandlerContext?>(null)
  private val inFlight = AtomicReference<CompletableFuture<FullHttpResponse>?>(null)

  private val group = AtomicReference<EventLoopGroup>()
  private val channel = AtomicReference<Channel>()

  fun attach(group: EventLoopGroup, channel: Channel) {
    check(this.group.compareAndSet(null, group))
    check(this.channel.compareAndSet(null, channel))
  }

  fun send(req: FullHttpRequest): CompletableFuture<FullHttpResponse> {
    val c = context.get() ?: throw IllegalStateException("The handler is not registered")

    // enforce single in-flight
    val promise = CompletableFuture<FullHttpResponse>()
    if (!inFlight.compareAndSet(null, promise)) {
      promise.completeExceptionally(IllegalStateException("Another request is in flight"))
      return promise
    }

    // set Host if missing
    if (!req.headers().contains(HttpHeaderNames.HOST)) {
      req.headers().set(HttpHeaderNames.HOST, hostHeader(c.channel().remoteAddress()))
    }

    // write the request
    c.writeAndFlush(req).addListener { write ->
      if (!write.isSuccess) inFlight.getAndSet(null)?.completeExceptionally(write.cause())
    }

    return promise
  }

  override fun handlerAdded(ctx: ChannelHandlerContext) {
    context.set(ctx)
  }

  override fun handlerRemoved(ctx: ChannelHandlerContext) {
    context.set(null)
  }

  override fun channelInactive(ctx: ChannelHandlerContext) {
    inFlight.getAndSet(null)?.completeExceptionally(ClosedChannelException())
    ctx.fireChannelInactive()
  }

  override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
    if (msg is FullHttpResponse) {
      // Retain for the caller. Aggregator will release the original after this method returns.
      val delivered = msg.retainedDuplicate()
      val p = inFlight.getAndSet(null)
      if (p != null) {
        p.complete(delivered)
      } else {
        // No waiter. Drop safely.
        ReferenceCountUtil.release(delivered)
      }
      return
    }
    ctx.fireChannelRead(msg)
  }

  override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
    inFlight.getAndSet(null)?.completeExceptionally(cause)
    ctx.fireExceptionCaught(cause)
  }

  private fun hostHeader(remote: SocketAddress): String = when (remote) {
    is InetSocketAddress -> {
      val host = remote.hostString
      val port = remote.port
      // Leave off default ports
      if (port == 80 || port == 443) host else "$host:$port"
    }

    else -> "localhost"
  }

  override fun close() {
    channel.get()?.close()?.get()
    group.get()?.shutdownGracefully(0, 0, TimeUnit.MILLISECONDS)
  }
}

internal fun TestClient.assertRequestOk(
  version: HttpVersion,
  path: String = "/test",
  buildRequest: FullHttpRequest.() -> Unit = {},
) {
  val request = DefaultFullHttpRequest(version, HttpMethod.GET, path)
  request.apply(buildRequest)

  val response = send(request).get()

  assertEquals(HttpResponseStatus.OK, response.status())
}
