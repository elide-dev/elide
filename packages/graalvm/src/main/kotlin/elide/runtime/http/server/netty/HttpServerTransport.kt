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

import io.netty.bootstrap.AbstractBootstrap
import io.netty.channel.Channel
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.*
import io.netty.channel.kqueue.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerDomainSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.incubator.channel.uring.IOUring
import io.netty.incubator.channel.uring.IOUringDatagramChannel
import io.netty.incubator.channel.uring.IOUringEventLoopGroup
import io.netty.incubator.channel.uring.IOUringServerSocketChannel
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress
import kotlin.io.path.pathString
import elide.runtime.Logging
import elide.runtime.http.server.netty.HttpServerTransport.Availability
import elide.runtime.http.server.netty.HttpServerTransport.Available
import elide.runtime.http.server.netty.HttpServerTransport.Unavailable.Companion.nativeTransportUnavailable

/**
 * Platform-specific transport that provides the [event loop groups][eventLoopGroup] and channel classes used by
 * the server (for both [TCP][tcpChannel] and [UDP][udpChannel] sockets).
 *
 * Use [HttpServerTransport.resolve] to resolve the preferred transport implementation for the current platform.
 */
public interface HttpServerTransport {
  /** Describes the availability of an [HttpServerTransport] on a specific platform. */
  public sealed interface Availability

  /** Indicates that the target [HttpServerTransport] is available and supports the requested features. */
  public data object Available : Availability

  /** Indicates that the target [HttpServerTransport] is unavailable or does not support the requested features. */
  @JvmInline public value class Unavailable(public val reason: String) : Availability {
    public companion object {
      public val UNSUPPORTED_TCP_DOMAIN_SOCKETS: Unavailable = Unavailable("TCP domain sockets are not supported")
      public val UNSUPPORTED_UDP_DOMAIN_SOCKETS: Unavailable = Unavailable("UDP domain sockets are not supported")

      public fun nativeTransportUnavailable(reason: Throwable?): Unavailable {
        return Unavailable(reason?.message ?: "no reason provided")
      }
    }
  }

  /**
   * Check if this transport is available for use, optionally requiring support for specific features.
   *
   * @param tcpDomain Require support for TCP over Unix domain sockets.
   * @param udpDomain Require support for UDP over Unix domain sockets.
   * @return The availability status of this transport for the current platform and requested feature set.
   */
  public fun checkAvailable(
    tcpDomain: Boolean = false,
    udpDomain: Boolean = false,
  ): Availability

  /**
   * Returns an [EventLoopGroup] to be used by the server (for either parent or child groups). Each call creates a new
   * instance that must be manually closed after use to prevent leaking resources.
   */
  public fun eventLoopGroup(): EventLoopGroup

  /** Returns the channel class to be used for TCP server sockets. */
  public fun tcpChannel(domain: Boolean = false): Class<out ServerChannel>

  /** Returns the channel class to be used for UDP server sockets */
  public fun udpChannel(domain: Boolean = false): Class<out Channel>

  /** Returns a possibly transport-specific equivalent for the given [address]. */
  public fun mapAddress(address: SocketAddress): SocketAddress = address

  /**
   * Apply additional configuration to a [bootstrap], after the event loop groups and channel types have been set.
   * Implementations may choose to add transport-specific options during this step; does nothing by default.
   */
  public fun configure(bootstrap: AbstractBootstrap<*, Channel>) {
    // noop
  }

  public companion object {
    /** Lists all supported server transports, in descending order of priority. */
    public val all: Array<HttpServerTransport> = arrayOf(
      // prefer `io_uring` if available (Linux-only, modern kernels)
      IOUringTransport,
      // next up, prefer `epoll` if available (Linux-only, nearly all kernels)
      EpollTransport,
      // next up, opt for `kqueue` on Unix-like systems
      KQueueTransport,
      // otherwise, fallback to NIO
      NioTransport,
    )

    private val log = Logging.of(HttpServerTransport::class)

    /**
     * Resolve the preferred [HttpServerTransport] for the current platform, optionally requiring support for the
     * specified features.
     *
     * @param tcpDomainSockets Whether support for TCP over Unix domain sockets is required.
     * @param udpDomainSockets Whether support for UDP over Unix domain sockets is required.
     * @return A [HttpServerTransport] with support for the requested features, or `null` if none is found.
     */
    public fun resolve(
      tcpDomainSockets: Boolean = false,
      udpDomainSockets: Boolean = false,
    ): HttpServerTransport? {
      for (transport in all) {
        val result = transport.checkAvailable(tcpDomainSockets, udpDomainSockets)

        if (result is Unavailable) log.debug("{} is unavailable because: {}", transport, result.reason)
        else return transport
      }

      return null
    }
  }
}

/**
 * Check if this transport is available for use, optionally requiring support for specific features.
 *
 * @param tcpDomain Require support for TCP over Unix domain sockets.
 * @param udpDomain Require support for UDP over Unix domain sockets.
 * @return Whether the transport is available for the current platform and supports the requested features.
 */
public fun HttpServerTransport.isAvailable(
  tcpDomain: Boolean = false,
  udpDomain: Boolean = false,
): Boolean = checkAvailable(tcpDomain, udpDomain) is Available

/** Generic Java NIO transport layer, used as fallback where native transport is not available. */
internal data object NioTransport : HttpServerTransport {
  override fun checkAvailable(tcpDomain: Boolean, udpDomain: Boolean): Availability {
    if (udpDomain) return HttpServerTransport.Unavailable.UNSUPPORTED_UDP_DOMAIN_SOCKETS
    return Available
  }

  override fun eventLoopGroup(): EventLoopGroup {
    return MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
  }

  override fun tcpChannel(domain: Boolean): Class<out ServerChannel> {
    return if (domain) NioServerDomainSocketChannel::class.java
    else NioServerSocketChannel::class.java
  }

  override fun udpChannel(domain: Boolean): Class<out Channel> {
    return if (domain) error("UDP over domain sockets is not supported when using the NIO server transport")
    else NioDatagramChannel::class.java
  }

  override fun mapAddress(address: SocketAddress): SocketAddress = when (address) {
    is DomainSocketAddress -> UnixDomainSocketAddress.of(address.path())
    else -> address
  }
}

/** Base class for Unix native transports. */
internal abstract class UnixTransport : HttpServerTransport {
  override fun mapAddress(address: SocketAddress): SocketAddress = when (address) {
    is UnixDomainSocketAddress -> DomainSocketAddress(address.path.pathString)
    else -> address
  }
}

/** IO-Uring transport layer, available on Linux only. */
internal data object IOUringTransport : UnixTransport() {
  override fun checkAvailable(tcpDomain: Boolean, udpDomain: Boolean): Availability = when {
    !IOUring.isAvailable() -> nativeTransportUnavailable(IOUring.unavailabilityCause())
    tcpDomain -> HttpServerTransport.Unavailable.UNSUPPORTED_TCP_DOMAIN_SOCKETS
    udpDomain -> HttpServerTransport.Unavailable.UNSUPPORTED_UDP_DOMAIN_SOCKETS
    else -> Available
  }

  override fun eventLoopGroup(): EventLoopGroup {
    return IOUringEventLoopGroup(Runtime.getRuntime().availableProcessors())
  }

  override fun tcpChannel(domain: Boolean): Class<out ServerChannel> {
    return if (domain) error("Domain sockets are not supported when using the IO_URING server transport")
    else IOUringServerSocketChannel::class.java
  }

  override fun udpChannel(domain: Boolean): Class<out Channel> {
    return if (domain) error("Domain sockets are not supported when using the IO_URING server transport")
    else IOUringDatagramChannel::class.java
  }
}

/** Epoll transport layer, available on most Linux kernels. */
internal data object EpollTransport : UnixTransport() {
  override fun checkAvailable(tcpDomain: Boolean, udpDomain: Boolean): Availability = when {
    !Epoll.isAvailable() -> nativeTransportUnavailable(Epoll.unavailabilityCause())
    else -> Available
  }

  override fun eventLoopGroup(): EventLoopGroup {
    return MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory())
  }

  override fun tcpChannel(domain: Boolean): Class<out ServerChannel> {
    return if (domain) EpollServerDomainSocketChannel::class.java
    else EpollServerSocketChannel::class.java
  }

  override fun udpChannel(domain: Boolean): Class<out Channel> {
    return if (domain) EpollDomainDatagramChannel::class.java
    else EpollDatagramChannel::class.java
  }

  override fun configure(bootstrap: AbstractBootstrap<*, Channel>) {
    bootstrap.option(EpollChannelOption.SO_REUSEPORT, true)
  }
}

/** KQueue transport layer, available for Unix-like systems */
internal data object KQueueTransport : UnixTransport() {
  override fun checkAvailable(tcpDomain: Boolean, udpDomain: Boolean): Availability = when {
    !KQueue.isAvailable() -> nativeTransportUnavailable(KQueue.unavailabilityCause())
    else -> Available
  }

  override fun eventLoopGroup(): EventLoopGroup {
    return MultiThreadIoEventLoopGroup(KQueueIoHandler.newFactory())
  }

  override fun tcpChannel(domain: Boolean): Class<out ServerChannel> {
    return if (domain) KQueueServerDomainSocketChannel::class.java
    else KQueueServerSocketChannel::class.java
  }

  override fun udpChannel(domain: Boolean): Class<out Channel> {
    return if (domain) KQueueDomainDatagramChannel::class.java
    else KQueueDatagramChannel::class.java
  }
}
