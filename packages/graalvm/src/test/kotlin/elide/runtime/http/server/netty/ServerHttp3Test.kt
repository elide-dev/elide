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

import elide.runtime.http.server.Http3Options
import elide.runtime.http.server.HttpApplicationOptions
import io.netty.handler.codec.http.HttpVersion
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue

class ServerHttp3Test : AbstractServerStackTest() {
  private fun h3Options(useDomainSockets: Boolean): HttpApplicationOptions {
    return HttpApplicationOptions(
      http3 = Http3Options(testCertificate, testAddress(useDomainSockets)),
      http = null,
    )
  }

  private fun testServer(
    transport: HttpServerTransport,
    useDomainSockets: Boolean,
    allowFailure: (Throwable) -> Boolean = { false },
    block: HttpApplicationStack.() -> Unit,
  ) {
    assumeFalse(useDomainSockets)
    assumeTrue(transport.isAvailable())
    withTestServer(h3Options(useDomainSockets), transport, allowFailure, block)
  }

  @DynamicTransportTest fun `should handle HTTP_3 requests`(
    transport: HttpServerTransport,
    useDomainSockets: Boolean,
  ) = testServer(transport, useDomainSockets) {
    val serverAddress = services.single().bindResult.getOrThrow().address

    TestClients.http3(serverAddress, testCertificate).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_1)
    }
  }
}
