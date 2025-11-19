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

import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.HttpsOptions
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.ssl.ApplicationProtocolNames
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ServerHttpsTest : AbstractServerStackTest() {
  private fun tlsOptions(useDomainSockets: Boolean): HttpApplicationOptions {
    return HttpApplicationOptions(
      https = HttpsOptions(testCertificate, testAddress(useDomainSockets)),
      http = null,
    )
  }

  private fun testServer(
      transport: ServerTransport,
      useDomainSockets: Boolean,
      allowFailure: (Throwable) -> Boolean = { false },
      block: HttpApplicationStack.() -> Unit,
  ) {
    assumeTrue(transport.isAvailable(tcpDomain = useDomainSockets))
    withTestServer(tlsOptions(useDomainSockets), transport, allowFailure, block)
  }

  @DynamicTransportTest fun `should handle HTTP_1 over TLS`(
      transport: ServerTransport,
      useDomainSockets: Boolean,
  ) = testServer(transport, useDomainSockets) {
    val serverAddress = services.single().bindResult.getOrThrow().address

    TestClients.tlsH1(serverAddress, testCertificate).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_0, "/http_1_0")
    }

    TestClients.tlsH1(serverAddress, testCertificate).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_1, "/http_1_1")
    }
  }

  @DynamicTransportTest fun `should support ALPN`(
      transport: ServerTransport,
      useDomainSockets: Boolean,
  ) = testServer(transport, useDomainSockets) {
    val serverAddress = services.single().bindResult.getOrThrow().address
    val selectedProtocol = AtomicReference<String?>()

    TestClients.tlsH2(
      target = serverAddress,
      certificate = testCertificate,
      onProtocolSelected = { selectedProtocol.set(it) },
    ).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_1)
    }

    val protocol = assertNotNull(selectedProtocol.get(), "expected a protocol to be negotiated")
    assertEquals(ApplicationProtocolNames.HTTP_2, protocol, "expected HTTP/2 to be negotiated")
  }
}
