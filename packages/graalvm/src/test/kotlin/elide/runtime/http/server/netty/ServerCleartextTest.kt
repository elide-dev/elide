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

import elide.runtime.http.server.CleartextOptions
import elide.runtime.http.server.HttpApplicationOptions
import io.netty.handler.codec.http.HttpClientUpgradeHandler
import io.netty.handler.codec.http.HttpVersion
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ServerCleartextTest : AbstractServerStackTest() {
  private fun cleartextOptions(useDomainSockets: Boolean): HttpApplicationOptions {
    return HttpApplicationOptions(http = CleartextOptions(testAddress(useDomainSockets)))
  }

  private fun testServer(
    transport: HttpServerTransport,
    useDomainSockets: Boolean,
    allowFailure: (Throwable) -> Boolean = { false },
    block: HttpApplicationStack.() -> Unit,
  ) {
    assumeTrue(transport.isAvailable(tcpDomain = useDomainSockets))
    withTestServer(cleartextOptions(useDomainSockets), transport, allowFailure, block)
  }

  @DynamicTransportTest fun `should handle cleartext HTTP_1`(
    transport: HttpServerTransport,
    useDomainSockets: Boolean,
  ) = testServer(transport, useDomainSockets) {
    TestClients.cleartext(singleBoundAddress()).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_0, "/http_1_0")
    }

    TestClients.cleartext(singleBoundAddress()).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_1, "/http_1_1")
    }
  }

  @DynamicTransportTest fun `should allow upgrading to cleartext HTTP-2`(
    transport: HttpServerTransport,
    useDomainSockets: Boolean
  ) = testServer(transport, useDomainSockets) {
    val serverAddress = services.single().bindResult.getOrThrow().address
    val upgradeSucceeded = AtomicReference<Boolean?>(null)

    TestClients.cleartextH2C(
      target = serverAddress,
      onUpgrade = {
        when (it) {
          HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_SUCCESSFUL -> upgradeSucceeded.set(true)
          HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_REJECTED -> upgradeSucceeded.set(false)
          HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_ISSUED -> Unit // ignore, not a response
        }
      },
    ).use { client ->
      client.assertRequestOk(HttpVersion.HTTP_1_1)
    }

    assertNotNull(upgradeSucceeded.get(), "Expected H2C upgrade to take place")
    assertTrue(upgradeSucceeded.get()!!, "Expected H2C upgrade to succeed")
  }
}

