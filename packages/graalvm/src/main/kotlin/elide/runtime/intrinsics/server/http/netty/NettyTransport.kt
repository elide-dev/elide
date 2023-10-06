/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.intrinsics.server.http.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollChannelOption
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.incubator.channel.uring.IOUring
import io.netty.incubator.channel.uring.IOUringEventLoopGroup
import io.netty.incubator.channel.uring.IOUringServerSocketChannel
import kotlin.reflect.KClass
import elide.runtime.intrinsics.server.http.netty.NettyTransport.Companion.resolve

/**
 * Defines a specific transport used by the Netty backend, the [resolve] function will return the preferred
 * [NettyTransport] for the current platform.
 */
internal sealed interface NettyTransport<T : ServerSocketChannel> {
  /** Configure and return a transport-specific [EventLoopGroup]. */
  fun eventLoopGroup(): EventLoopGroup

  /** Return the class marker used to indicate Netty which server socket type should be used. */
  fun socketChannel(): KClass<T>

  /**
   * Apply this transport in the target [ServerBootstrap] [scope]. The default implementation configures the
   * [eventLoopGroup] and [socketChannel] options.
   */
  fun bootstrap(scope: ServerBootstrap) {
    try {
      scope.group(eventLoopGroup())
      scope.channel(socketChannel().java)
    } catch (unsatisfied: UnsatisfiedLinkError) {
      scope.group(NioEventLoopGroup())
      scope.channel(NioServerSocketChannel::class.java)
    }
  }

  companion object {
    /** Resolve a [NettyTransport] implementation for the current platform. */
    internal fun resolve(): NettyTransport<*> = when {
      // prefer `io_uring` if available (Linux-only, modern kernels)
      IOUring.isAvailable() -> IOUringTransport

      // next up, prefer `epoll` if available (Linux-only, nearly all kernels)
      Epoll.isAvailable() -> EpollTransport

      // next up, opt for `kqueue` on Unix-like systems
      KQueue.isAvailable() -> KQueueTransport

      // otherwise, fallback to NIO
      else -> NioTransport
    }
  }
}

/** IO-Uring transport layer, available on Linux only. */
internal data object IOUringTransport : NettyTransport<IOUringServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return IOUringEventLoopGroup(Runtime.getRuntime().availableProcessors())
  }

  override fun socketChannel(): KClass<IOUringServerSocketChannel> {
    return IOUringServerSocketChannel::class
  }
}

/** Epoll transport layer, available on most Linux kernels. */
internal data object EpollTransport : NettyTransport<EpollServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return EpollEventLoopGroup()
  }

  override fun socketChannel(): KClass<EpollServerSocketChannel> {
    return EpollServerSocketChannel::class
  }

  override fun bootstrap(scope: ServerBootstrap) {
    super.bootstrap(scope)
    scope.option(EpollChannelOption.SO_REUSEPORT, true)
  }
}

/** KQueue transport layer, available for Unix-like systems */
internal data object KQueueTransport : NettyTransport<KQueueServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return KQueueEventLoopGroup()
  }

  override fun socketChannel(): KClass<KQueueServerSocketChannel> {
    return KQueueServerSocketChannel::class
  }
}

/** Generic Java NIO transport layer, used as fallback where native transport is not available. */
internal data object NioTransport : NettyTransport<NioServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return NioEventLoopGroup()
  }

  override fun socketChannel(): KClass<NioServerSocketChannel> {
    return NioServerSocketChannel::class
  }
}
