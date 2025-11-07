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
package elide.runtime.node.http

import elide.runtime.http.server.Http3Options
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.HttpsOptions
import elide.runtime.intrinsics.js.err.TypeError
import org.graalvm.polyglot.Value
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress

internal data object NodeHttpServerOptions {
  private const val MEMBER_ELIDE_OPTIONS = "elide"
  private const val MEMBER_HTTPS_OPTIONS = "https"
  private const val MEMBER_HTTP3_OPTIONS = "http3"

  private const val MEMBER_CERTIFICATE = "certificate"

  private const val MEMBER_PORT = "port"
  private const val MEMBER_HOST = "host"
  private const val MEMBER_PATH = "path"

  fun serverOptions(options: Value?): HttpApplicationOptions {
    if (options == null || !options.hasMembers()) return HttpApplicationOptions()

    val elideOptions = options.takeIf { it.hasMember(MEMBER_ELIDE_OPTIONS) }?.getMember(MEMBER_ELIDE_OPTIONS)
      ?: return HttpApplicationOptions()

    val https = elideOptions.takeIf { it.hasMember(MEMBER_HTTPS_OPTIONS) }
      ?.getMember(MEMBER_HTTPS_OPTIONS)?.let { https ->
        val certificate = https.takeIf { it.hasMember(MEMBER_CERTIFICATE) }?.getMember(MEMBER_CERTIFICATE)
          ?.let(NodeHttpCertificateOptions::unwrapCertificateOptions)
          ?: throw TypeError.Companion.create("HTTPS options must configure a certificate")

            HttpsOptions(
                certificate = certificate,
                address = selectBindingAddress(
                    defaultHost = HttpsOptions.Companion.DEFAULT_HTTPS_HOST,
                    defaultPort = HttpsOptions.Companion.DEFAULT_HTTPS_PORT,
                    options = https,
                ),
            )
      }

    val http3 = elideOptions.takeIf { it.hasMember(MEMBER_HTTP3_OPTIONS) }
      ?.getMember(MEMBER_HTTP3_OPTIONS)?.let { http3 ->
        val certificate = http3.takeIf { it.hasMember(MEMBER_CERTIFICATE) }?.getMember(MEMBER_CERTIFICATE)
          ?.let(NodeHttpCertificateOptions::unwrapCertificateOptions)
          ?: throw TypeError.Companion.create("HTTP/3 options must configure a certificate")

            Http3Options(
                certificate = certificate,
                address = selectBindingAddress(
                    defaultHost = Http3Options.Companion.DEFAULT_HTTP3_HOST,
                    defaultPort = Http3Options.Companion.DEFAULT_HTTP3_PORT,
                    options = http3,
                ),
            )
      }

    return HttpApplicationOptions(https = https, http3 = http3)
  }

  private fun selectBindingAddress(options: Value, defaultHost: String, defaultPort: Int): SocketAddress {
    if (options.hasMember(MEMBER_PATH)) return options.getMember(MEMBER_PATH)?.takeIf { it.isString }?.let { path ->
      UnixDomainSocketAddress.of(path.asString())
    } ?: throw TypeError.Companion.create("Domain socket path must be a string")

    val host = options.takeIf { it.hasMember(MEMBER_HOST) }?.getMember(MEMBER_HOST)?.let {
      if (!it.isString) throw TypeError.Companion.create("Host name must be a string")
      it.asString()
    }

    val port = options.takeIf { it.hasMember(MEMBER_PORT) }?.getMember(MEMBER_PORT)?.let {
      if (!it.isNumber || !it.fitsInInt()) throw TypeError.Companion.create("Server port must be an integer")
      it.asInt()
    }

    return InetSocketAddress(host ?: defaultHost, port ?: defaultPort)
  }
}
