package elide.runtime.intriniscs.server.http.netty

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

internal sealed interface NettyTransport<T : ServerSocketChannel> {

  fun eventLoopGroup(): EventLoopGroup

  fun socketChannel(): KClass<T>

  fun bootstrap(scope: ServerBootstrap) {
    scope.group(eventLoopGroup())
    scope.channel(socketChannel().java)
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

internal data object IOUringTransport : NettyTransport<IOUringServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return IOUringEventLoopGroup(Runtime.getRuntime().availableProcessors())
  }

  override fun socketChannel(): KClass<IOUringServerSocketChannel> {
    return IOUringServerSocketChannel::class
  }
}

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

internal data object KQueueTransport : NettyTransport<KQueueServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return KQueueEventLoopGroup()
  }

  override fun socketChannel(): KClass<KQueueServerSocketChannel> {
    return KQueueServerSocketChannel::class
  }
}

internal data object NioTransport : NettyTransport<NioServerSocketChannel> {
  override fun eventLoopGroup(): EventLoopGroup {
    return NioEventLoopGroup()
  }

  override fun socketChannel(): KClass<NioServerSocketChannel> {
    return NioServerSocketChannel::class
  }
}