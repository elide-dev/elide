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

import elide.runtime.http.server.CertificateSource
import elide.runtime.http.server.HttpsOptions
import elide.runtime.http.server.buildSelfSignedBundle
import elide.runtime.http.server.netty.HttpsService.LABEL
import io.netty.channel.ChannelHandler
import io.netty.channel.EventLoopGroup
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.*
import java.net.SocketAddress

/** Configures an HTTPS server with support for HTTP/1.x and HTTP/2 and protocol selection via ALPN. */
internal data object HttpsService : StackServiceContributor(LABEL) {
  const val LABEL: String = "https"
  const val SCHEME: String = "https"

  override val useUdp: Boolean = false
  override val targetScheme: String = SCHEME

  override fun isApplicable(scope: BindingScope): Boolean = scope.options.https != null
  override fun selectAddress(scope: BindingScope): SocketAddress {
    return requireNotNull(scope.options.https?.address)
  }

  override fun newGroup(scope: BindingScope, transport: HttpServerTransport, child: Boolean): EventLoopGroup {
    val cleartextTransport = scope.transports[HttpCleartextService.LABEL]
    return when {
      child -> scope.childGroupFor(HttpCleartextService)
        ?.takeIf { cleartextTransport === transport }
        ?: transport.eventLoopGroup()

      else -> scope.parentGroupFor(HttpCleartextService)
        ?.takeIf { cleartextTransport === transport }
        ?: transport.eventLoopGroup()
    }
  }

  override fun prepareHandler(scope: BindingScope): ChannelHandler {
    val options = requireNotNull(scope.options.https)
    return HttpChannelHandlers.tls(
      application = scope.application,
      ssl = prepareSslContext(options),
      serverName = scope.options.serverName,
      altServices = altServices(scope),
      stack = scope.deferredStack,
    )
  }

  private fun altServices(scope: BindingScope): AltServicesConfig {
    return AltServicesConfig(
      // HTTP/3 advertising can be disabled in options
      h3 = if (scope.options.http3?.advertise != true) null
      else Http3Service.altServiceFor(Http3Service.ALPN_ID, scope, this),
    )
  }

  /**
   * Build an [SslContext] for an HTTPS server from the given [options], requesting support for ALPN with upgrade and
   * fallback behaviors.
   */
  private fun prepareSslContext(options: HttpsOptions): SslContext {
    val builder = when (options.certificate) {
      is CertificateSource.File -> with(options.certificate) {
        SslContextBuilder.forServer(certFile.toFile(), keyFile.toFile(), keyPassphrase)
      }

      is CertificateSource.SelfSigned -> with(options.certificate) {
        val certificate = buildSelfSignedBundle()
        SslContextBuilder.forServer(certificate.keyPair.private, certificate.certificate)
      }
    }

    return builder
      .sslProvider(if (SslProvider.isAlpnSupported(SslProvider.OPENSSL)) SslProvider.OPENSSL else SslProvider.JDK)
      .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
      .applicationProtocolConfig(
        ApplicationProtocolConfig(
          ApplicationProtocolConfig.Protocol.ALPN,
          ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
          ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
          ApplicationProtocolNames.HTTP_2, ApplicationProtocolNames.HTTP_1_1,
        ),
      )
      .build()
  }
}
