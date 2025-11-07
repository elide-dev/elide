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

import elide.runtime.http.server.netty.HttpCleartextService.LABEL
import io.netty.channel.ChannelHandler
import io.netty.handler.ssl.ApplicationProtocolNames
import java.net.InetSocketAddress
import java.net.SocketAddress

/** Configures a basic cleartext HTTP server, supporting both HTTP/1.x and HTTP/2 with upgrades via H2C. */
internal data object HttpCleartextService : StackServiceContributor(LABEL) {
  const val LABEL: String = "http"
  const val SCHEME: String = "http"
  const val ALPN_H2C: String = "h2c"

  override val useUdp: Boolean = false
  override val targetScheme: String = SCHEME

  override fun isApplicable(scope: BindingScope): Boolean = scope.options.http != null
  override fun selectAddress(scope: BindingScope): SocketAddress {
    return requireNotNull(scope.options.http?.address)
  }

  override fun prepareHandler(scope: BindingScope): ChannelHandler {
    return HttpChannelHandlers.cleartext(
      application = scope.application,
      serverName = scope.options.serverName,
      altServices = altServices(scope),
      stack = scope.deferredStack,
    )
  }

  private fun altServices(scope: BindingScope): AltServicesConfig {
    val bindingAddress = bindingAddress(scope) as? InetSocketAddress

    return AltServicesConfig(
      h2c = if (bindingAddress == null) null
      else AltServiceSource(ALPN_H2C, LABEL, LABEL, bindingAddress.hostName),
      // HTTPS (h2) is always advertised if possible
      h2 = HttpsService.altServiceFor(ApplicationProtocolNames.HTTP_2, scope, this),
      // HTTP/3 advertising can be disabled in options
      h3 = if (scope.options.http3?.advertise != true) null
      else Http3Service.altServiceFor(Http3Service.ALPN_ID, scope, this),
    )
  }
}
