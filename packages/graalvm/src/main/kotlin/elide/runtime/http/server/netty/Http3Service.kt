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

import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandler
import io.netty.incubator.codec.http3.Http3
import io.netty.incubator.codec.quic.QuicSslContext
import io.netty.incubator.codec.quic.QuicSslContextBuilder
import java.net.SocketAddress
import elide.runtime.http.server.CertificateSource
import elide.runtime.http.server.Http3Options
import elide.runtime.http.server.buildSelfSignedBundle
import elide.runtime.http.server.netty.Http3Service.LABEL

/** Configures an HTTP/3 server (UDP) using Quic. */
internal data object Http3Service : StackServiceContributor(LABEL) {
  const val LABEL: String = "http3"
  const val SCHEME: String = "https"
  const val ALPN_ID = "h3"

  override val useUdp: Boolean = true
  override val targetScheme: String = SCHEME

  override fun isApplicable(scope: BindingScope): Boolean = scope.options.http3 != null
  override fun prepareBootstrap(): AbstractBootstrap<*, Channel> = Bootstrap()

  override fun selectAddress(scope: BindingScope): SocketAddress {
    return requireNotNull(scope.options.http3?.address)
  }

  override fun prepareHandler(scope: BindingScope): ChannelHandler {
    val options = requireNotNull(scope.options.http3)
    return HttpChannelHandlers.http3(
      application = scope.application,
      quicSSL = prepareQuicSslContext(options),
      serverName = scope.options.serverName,
    )
  }

  /** Prepare a [QuicSslContext] for an HTTP/3-over-Quic server from the given [options]. */
  private fun prepareQuicSslContext(options: Http3Options): QuicSslContext {
    val builder = when (options.certificate) {
      is CertificateSource.File -> with(options.certificate) {
        QuicSslContextBuilder.forServer(keyFile.toFile(), keyPassphrase, certFile.toFile())
      }

      is CertificateSource.SelfSigned -> with(options.certificate) {
        val certificate = buildSelfSignedBundle()
        QuicSslContextBuilder.forServer(certificate.keyPair.private, null, certificate.certificate)
      }
    }

    return builder
      .applicationProtocols(*Http3.supportedApplicationProtocols())
      .build()
  }
}
