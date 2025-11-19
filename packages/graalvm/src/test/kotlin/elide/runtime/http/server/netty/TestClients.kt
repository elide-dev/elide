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
package elide.runtime.http.server.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioDomainSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.unix.DomainSocketAddress
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpClientUpgradeHandler
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http2.*
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.handler.ssl.SslContextBuilder
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3ClientConnectionHandler
import io.netty.incubator.codec.http3.Http3FrameToHttpObjectCodec
import io.netty.incubator.codec.quic.QuicChannel
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import org.junit.jupiter.api.Assumptions
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import elide.runtime.http.server.CertificateSource

data object TestClients {
  /**
   * If the [address] is a native transport domain address, this method returns the NIO equivalent, otherwise returns
   * [address].
   */
  private fun nioAddress(address: SocketAddress): SocketAddress {
    return if (address is DomainSocketAddress) UnixDomainSocketAddress.of(address.path())
    else address
  }

  /** Create a new NIO event loop group for the client. */
  private fun nioGroup(): EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())

  /** Select the channel class to be used to connect to the given [target]. */
  private fun channelType(target: SocketAddress, udp: Boolean = false): Class<out Channel> {
    Assumptions.assumeFalse(udp && target.isDomainSocket()) {
      "UDP over domain sockets is not yet supported"
    }

    return if (target.isDomainSocket()) NioDomainSocketChannel::class.java
    else if (!udp) NioSocketChannel::class.java
    else NioDatagramChannel::class.java
  }

  /** Shortcut for creating a new channel initializer handler. */
  private fun initializer(block: Channel.() -> Unit): ChannelInitializer<Channel> {
    return object : ChannelInitializer<Channel>() {
      override fun initChannel(ch: Channel) = block(ch)
    }
  }

  /** Adds required handlers in front of the client. */
  private fun ChannelPipeline.addSupportHandlers(): ChannelPipeline = apply {
    addLast(HttpObjectAggregator(64 * 1024))
  }

  /** Connect to the remote address and return the connected channel. */
  private fun Bootstrap.connectSync(): Channel {
    return connect().sync().channel()
  }

  private fun clientBootstrap(target: SocketAddress, group: EventLoopGroup): Bootstrap {
    return Bootstrap()
      .group(group)
      .channel(channelType(target))
      .remoteAddress(target)
  }

  /**
   * Configures and connects a single-use client that sends cleartext HTTP/1.x requests, without support for protocol
   * upgrades.
   */
  fun cleartext(target: SocketAddress): TestClient {
    val client = TestClient()
    val address = nioAddress(target)

    val init = initializer {
      pipeline()
        .addLast(HttpClientCodec())
        .addSupportHandlers()
        .addLast(client)
    }

    val group = nioGroup()
    val channel = clientBootstrap(address, group)
      .handler(init)
      .connectSync()

    client.attach(group, channel)
    return client
  }

  /**
   * Configures and connects a single-use client that sends cleartext HTTP/1.x requests and requests to upgrade to
   * HTTP/2 via h2c.
   */
  fun cleartextH2C(
    target: SocketAddress,
    onUpgrade: (event: HttpClientUpgradeHandler.UpgradeEvent) -> Unit = {},
  ): TestClient {
    val client = TestClient()
    val address = nioAddress(target)
    val group = nioGroup()

    val h2StreamInit = initializer {
      pipeline()
        .addLast(Http2StreamFrameToHttpObjectCodec(/* isServer = */ false))
        .addSupportHandlers()
        .addLast(client)
    }

    // trigger the callback when the upgrade happens
    val upgradeListener = object : ChannelInboundHandlerAdapter() {
      override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        if (evt !is HttpClientUpgradeHandler.UpgradeEvent) return super.userEventTriggered(ctx, evt)
        else onUpgrade(evt)
      }
    }

    val upgradeCodec = Http2ClientUpgradeCodec(
      Http2FrameCodecBuilder.forClient().build() as Http2ConnectionHandler,
      Http2MultiplexHandler(h2StreamInit, h2StreamInit),
    )

    val h1Codec = HttpClientCodec()
    val pipelineInit = initializer {
      pipeline()
        .addLast(h1Codec)
        .addLast(HttpClientUpgradeHandler(h1Codec, upgradeCodec, 64 * 1024))
        // fallback HTTP/1.1 pipeline (required to start the upgrade)
        .addSupportHandlers()
        .addLast(client)
        .addLast(upgradeListener)
    }

    val channel = clientBootstrap(address, group)
      .handler(pipelineInit)
      .connectSync()

    client.attach(group, channel)
    return client
  }

  /** Configures and connects a single-use client that sends HTTP/1.x requests over TLS without negotiating upgrades. */
  fun tlsH1(target: SocketAddress, certificate: CertificateSource.File): TestClient {
    val sslContext = SslContextBuilder.forClient()
      .trustManager(certificate.certFile.toFile())
      .endpointIdentificationAlgorithm(null)
      .build()

    val client = TestClient()
    val address = nioAddress(target)

    val init = initializer {
      pipeline()
        .addLast(sslContext.newHandler(alloc()))
        .addLast(HttpClientCodec())
        .addSupportHandlers()
        .addLast(client)
    }

    val group = nioGroup()
    val channel = clientBootstrap(address, group)
      .handler(init)
      .connectSync()

    client.attach(group, channel)
    return client
  }

  /** Configures and connects a single-use client that negotiates HTTP/2 over TLS via ALPN. */
  fun tlsH2(
    target: SocketAddress,
    certificate: CertificateSource.File,
    onProtocolSelected: (String) -> Unit = {},
  ): TestClient {
    // request support for ALPN
    val sslContext = SslContextBuilder.forClient()
      .trustManager(certificate.certFile.toFile())
      .applicationProtocolConfig(
        ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          listOf(ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1),
        ),
      )
      .endpointIdentificationAlgorithm(null)
      .build()

    val client = TestClient()
    val address = nioAddress(target)
    val group = nioGroup()

    // ALPN selector with callback
    val alpnLatch = CountDownLatch(1)
    val alpnHandler = object : ApplicationProtocolNegotiationHandler("") {
      override fun configurePipeline(ctx: ChannelHandlerContext, protocol: String) {
        if (ApplicationProtocolNames.HTTP_2 == protocol) {
          ctx.pipeline()
            .addLast(Http2FrameCodecBuilder.forClient().build())
            .addLast(Http2MultiplexHandler(initializer { }))

          Http2StreamChannelBootstrap(ctx.channel()).open()
            .get().pipeline()
            .addLast(Http2StreamFrameToHttpObjectCodec(false))
            .addSupportHandlers()
            .addLast(client)

          alpnLatch.countDown()
        }

        if (ApplicationProtocolNames.HTTP_1_1 == protocol) ctx.pipeline()
          .addLast(HttpClientCodec())
          .addSupportHandlers()
          .addLast(client)

        onProtocolSelected(protocol)
      }
    }

    val init = initializer {
      pipeline()
        .addLast(sslContext.newHandler(alloc()))
        .addLast(alpnHandler)
    }

    val channel = clientBootstrap(address, group)
      .handler(init)
      .connectSync()

    alpnLatch.await()
    client.attach(group, channel)
    return client
  }

  /** Configures and connects a single-use client that sends HTTP/3 over Quic. */
  fun http3(target: SocketAddress, certificate: CertificateSource.File): TestClient {
    val client = TestClient()

    val quicSsl = QuicSslContextBuilder.forClient()
      .applicationProtocols(*Http3.supportedApplicationProtocols())
      .trustManager(certificate.certFile.toFile())
      .build()

    val quicCodec = Http3.newQuicClientCodecBuilder()
      .sslContext(quicSsl)
      .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
      .initialMaxData(10000000)
      .initialMaxStreamDataBidirectionalLocal(1000000)
      .initialMaxStreamsUnidirectional(1000000)
      .build()

    val group = nioGroup()
    val udpChannel = Bootstrap()
      .group(group)
      .channel(NioDatagramChannel::class.java)
      .handler(initializer { pipeline().addLast(quicCodec) })
      .bind(0).sync().channel()

    val h3StreamInit = initializer {
      pipeline()
        .addLast(Http3FrameToHttpObjectCodec(false))
        .addLast(HttpObjectAggregator(1024))
        .addLast(client)
    }

    val quicChannel = QuicChannel.newBootstrap(udpChannel)
      .handler(Http3ClientConnectionHandler())
      .remoteAddress(InetSocketAddress("localhost", (target as InetSocketAddress).port))
      .connect()
      .get()

    // manually start the HTTP/3 stream once we have an open quic stream
    Http3.newRequestStream(quicChannel, h3StreamInit).get()
    client.attach(group, udpChannel)

    return client
  }
}
