/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.tool.server

import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress
import java.time.Instant
import kotlin.io.path.Path
import elide.runtime.http.server.*
import elide.tool.cli.cfg.ElideCLITool.ELIDE_TOOL_VERSION
import elide.tooling.project.manifest.ElidePackageManifest.ServerSettings

fun ServerSettings?.toHttpApplicationOptions(): HttpApplicationOptions {
  if (this == null) return HttpApplicationOptions()

  return HttpApplicationOptions(
    serverName = serverName ?: "elide/$ELIDE_TOOL_VERSION",
    http = if (!cleartext) null else CleartextOptions(
      address.toAddress(
        CleartextOptions.DEFAULT_HTTP_HOST,
        CleartextOptions.DEFAULT_HTTP_PORT,
      ),
    ),
    https = https?.let {
      HttpsOptions(
        certificate = it.certificate.toCertificateSource(),
        address = it.address.toAddress(HttpsOptions.DEFAULT_HTTPS_HOST, HttpsOptions.DEFAULT_HTTPS_PORT),
      )
    },
    http3 = http3?.let {
      Http3Options(
        certificate = it.certificate.toCertificateSource(),
        address = it.address.toAddress(Http3Options.DEFAULT_HTTP3_HOST, Http3Options.DEFAULT_HTTP3_PORT),
        advertise = it.advertise,
      )
    },
  )
}

private fun ServerSettings.SSLCertificate.toCertificateSource(): CertificateSource = when (this) {
  is ServerSettings.SSLCertificate.LocalFileCertificate -> CertificateSource.File(
    certFile = Path(certFile),
    keyFile = Path(keyFile),
    keyPassphrase = keyPassphrase,
  )

  is ServerSettings.SSLCertificate.SelfSignedCertificate -> CertificateSource.SelfSigned(
    subject = subject ?: CertificateSource.SelfSigned.DEFAULT_SUBJECT,
    notBefore = Instant.now().minusSeconds(notBefore ?: CertificateSource.SelfSigned.DEFAULT_START_OFFSET_SECONDS),
    notAfter = Instant.now().plusSeconds(notAfter ?: CertificateSource.SelfSigned.DEFAULT_END_OFFSET_SECONDS),
  )
}

private fun ServerSettings.BindingAddress?.toAddress(
  defaultHost: String,
  defaultPort: Int,
): SocketAddress {
  return when (this) {
    is ServerSettings.BindingAddress.DomainSocketAddress -> UnixDomainSocketAddress.of(path)
    is ServerSettings.BindingAddress.SocketAddress -> InetSocketAddress(hostname ?: defaultHost, port ?: defaultPort)
    else -> InetSocketAddress(defaultHost, defaultPort)
  }
}
