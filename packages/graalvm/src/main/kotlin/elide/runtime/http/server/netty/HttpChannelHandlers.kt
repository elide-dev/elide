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
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerExpectContinueHandler
import io.netty.handler.codec.http.HttpServerUpgradeHandler
import io.netty.handler.codec.http2.*
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler
import io.netty.handler.ssl.SslContext
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.http3.Http3FrameToHttpObjectCodec
import io.netty.incubator.codec.http3.Http3ServerConnectionHandler
import io.netty.incubator.codec.quic.InsecureQuicTokenHandler
import io.netty.incubator.codec.quic.QuicSslContext
import io.netty.util.AsciiString
import java.net.InetSocketAddress
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import elide.runtime.http.server.CallContext
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.netty.HttpChannelHandlers.cleartext
import elide.runtime.http.server.netty.HttpChannelHandlers.http3
import elide.runtime.http.server.netty.HttpChannelHandlers.tls

/**
 * A collection of internal helpers used to configure and initialize Netty channels when creating an
 * [HttpApplicationStack].
 *
 * The [cleartext], [tls], and [http3] functions create channel initializers for cleartext HTTP with h2c upgrade,
 * HTTPS with ALPN, and HTTP/3, respectively.
 */
internal data object HttpChannelHandlers {
  // default protocol used for ALPN
  private const val FALLBACK_PROTOCOL = "http/1.1"

  // max length of h2c upgrade requests
  private const val MAX_UPGRADE_LENGTH = 65536

  private const val H3_INITIAL_MAX_DATA = 10_000_000L
  private const val H3_INITIAL_MAX_STREAM_DATA = 1_000_000L
  private const val H3_INITIAL_MAX_STREAMS = 100L
  private const val H3_MAX_IDLE_TIMEOUT_MILLIS = 5_000L

  // creates an inline channel initializer handler
  private inline fun initializer(crossinline block: Channel.() -> Unit) = object : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) = ch.block()
  }

  /**
   * Returns a new [ChannelInitializer] that configures each channel with an HTTP/1.x cleartext pipeline that supports
   * upgrading to HTTP/2 via h2c. Prior knowledge is not supported.
   */
  internal fun cleartext(
    application: HttpApplication<CallContext>,
    serverName: String,
    altServices: AltServicesConfig,
    stack: Future<HttpApplicationStack>,
  ): ChannelInitializer<Channel> {
    return initializer {
      val resolvedAltServices = if (stack.state() != Future.State.SUCCESS) AltServices()
      else resolveAltServices(altServices, stack.resultNow())

      // HTTP/1 stack
      val httpCodec = HttpServerCodec()
      pipeline().addLast(httpCodec)

      // HTTP/2 stack used after upgrade
      val h2fc = Http2FrameCodecBuilder.forServer().build()
      val h2mux =
        Http2MultiplexHandler(http2Pipeline(application, serverName, resolvedAltServices, cleartext = true))

      // upgrade path
      val factory = HttpServerUpgradeHandler.UpgradeCodecFactory { protocol ->
        if (!AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, protocol)) null
        else Http2ServerUpgradeCodec(h2fc, h2mux)
      }

      pipeline()
        .addLast(HttpServerUpgradeHandler(httpCodec, factory, MAX_UPGRADE_LENGTH))
        // HTTP/1-only fallback
        .addLast(HttpServerExpectContinueHandler())
        .addLast(AltServiceAdvertisingHandler.forH1(resolvedAltServices, cleartext = true))
        .addLast(NettyCallHandlerAdapter(application, serverName))
    }
  }

  /**
   * Returns a new [ChannelInitializer] that configures each channel with an HTTPS pipeline using the given [ssl]
   * context, with support for HTTP/1.x, HTTP/2, and protocol negotiation via ALPN.
   *
   * The [application] runs inside a [NettyCallHandlerAdapter], which is placed behind an
   * [Http2StreamFrameToHttpObjectCodec] when HTTP/2 is selected.
   */
  internal fun tls(
    application: HttpApplication<CallContext>,
    ssl: SslContext,
    serverName: String,
    altServices: AltServicesConfig,
    stack: Future<HttpApplicationStack>,
  ): ChannelInitializer<Channel> {
    return initializer {
      val resolvedAltServices = if (stack.state() != Future.State.SUCCESS) AltServices()
      else resolveAltServices(altServices, stack.resultNow())

      pipeline()
        .addLast(ssl.newHandler(alloc()))
        .addLast(TlsAlpnHandler(application, resolvedAltServices, serverName))
    }
  }

  /**
   * Returns a new [ChannelInitializer] that configures a UDP socket channel to support HTTP/3 with the given
   * [quicSSL] context.
   *
   * The [application] runs inside a [NettyCallHandlerAdapter] placed behind an [Http3FrameToHttpObjectCodec], which
   * translates between HTTP/3 and HTTP/1.x.
   */
  internal fun http3(
    application: HttpApplication<CallContext>,
    quicSSL: QuicSslContext,
    serverName: String,
  ) = initializer {
    val initHttp3Stream = initializer {
      pipeline()
        .addLast(Http3FrameToHttpObjectCodec(true))
        .addLast(NettyCallHandlerAdapter(application, serverName))
    }

    val initQuicChannel = initializer {
      pipeline().addLast(Http3ServerConnectionHandler(initHttp3Stream))
    }

    val quic = Http3.newQuicServerCodecBuilder()
      .sslContext(quicSSL)
      .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
      .handler(initQuicChannel)
      .maxIdleTimeout(H3_MAX_IDLE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
      .initialMaxData(H3_INITIAL_MAX_DATA)
      .initialMaxStreamDataBidirectionalLocal(H3_INITIAL_MAX_STREAM_DATA)
      .initialMaxStreamDataBidirectionalRemote(H3_INITIAL_MAX_STREAM_DATA)
      .initialMaxStreamsBidirectional(H3_INITIAL_MAX_STREAMS)
      .initialMaxStreamsUnidirectional(H3_INITIAL_MAX_STREAMS)
      .build()

    pipeline().addLast(quic)
  }

  /** Initializes a nested HTTP/2 pipeline, adding a frame-to-object codec before the call handler adapter. */
  private fun http2Pipeline(
    application: HttpApplication<CallContext>,
    serverName: String,
    altServices: AltServices,
    cleartext: Boolean,
  ) = initializer {
    pipeline()
      .addLast(Http2StreamFrameToHttpObjectCodec(true))
      .addLast(AltServiceAdvertisingHandler.forH2(altServices, cleartext))
      .addLast(NettyCallHandlerAdapter(application, serverName))
  }

  /**
   * Resolve the given alt services [source] configuration once a [stack] becomes available, providing resolved hosts
   * and ports that can be used to construct the records.
   */
  private fun resolveAltServices(source: AltServicesConfig, stack: HttpApplicationStack): AltServices {
    return AltServices(
      h2c = source.h2c?.let { resolveAltService(it, stack) },
      h2 = source.h2?.let { resolveAltService(it, stack) },
      h3 = source.h3?.let { resolveAltService(it, stack) },
    )
  }

  /**
   * Prepare an [AltService] from a [source] once the application [stack] is bound and resolved hostnames and ports
   * are available.
   */
  private fun resolveAltService(source: AltServiceSource, stack: HttpApplicationStack): AltService? {
    val sponsor = stack.services.find { it.label == source.sponsorService }
      ?.bindResult?.getOrNull()?.address.let { it as? InetSocketAddress } // no domain sockets
      ?: return null // should not be possible (the sponsor itself calls this method)

    val alt = stack.services.find { it.label == source.altService }
      ?.bindResult?.getOrNull()?.address.let { it as? InetSocketAddress } // no domain sockets
      ?: return null // configured alt service may have failed to start

    // if the sponsor and the alt end up with different resolved hostnames, add
    // the _unresolved_ alt host to the authority (e.g. 'localhost', not '0.0.0.0')
    val hostname = source.altServiceHost.takeIf { sponsor.hostName != alt.hostName }.orEmpty()
    val port = alt.port

    return AltService(protocol = source.protocol, authority = "$hostname:$port")
  }

  /** Negotiates HTTP/1.1 vs HTTP/2 over TLS. */
  private class TlsAlpnHandler(
    private val application: HttpApplication<CallContext>,
    private val altServices: AltServices,
    private val serverName: String,
  ) : ApplicationProtocolNegotiationHandler(FALLBACK_PROTOCOL) {
    override fun configurePipeline(context: ChannelHandlerContext, protocol: String) {
      when (protocol) {
        // HTTP/2 frame codec <-> multiplex handler <=> (frame codec <-> call handler)
        ApplicationProtocolNames.HTTP_2 -> context.pipeline().addLast(
          Http2FrameCodecBuilder.forServer().build(),
          Http2MultiplexHandler(http2Pipeline(application, serverName, altServices, cleartext = false)),
        )

        // HTTP/1 codec <-> expect/continue handler <-> call handler
        ApplicationProtocolNames.HTTP_1_1 -> context.pipeline().addLast(
          HttpServerCodec(),
          HttpServerExpectContinueHandler(),
          AltServiceAdvertisingHandler.forH1(altServices),
          NettyCallHandlerAdapter(application, serverName),
        )

        else -> throw IllegalStateException("Unknown ALPN: $protocol")
      }
    }
  }
}
