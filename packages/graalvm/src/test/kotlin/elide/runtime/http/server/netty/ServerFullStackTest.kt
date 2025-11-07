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

import elide.runtime.http.server.CallContext
import elide.runtime.http.server.CleartextOptions
import elide.runtime.http.server.Http3Options
import elide.runtime.http.server.HttpApplication
import elide.runtime.http.server.HttpApplicationOptions
import elide.runtime.http.server.HttpCall
import elide.runtime.http.server.HttpsOptions
import elide.runtime.http.server.HttpRequestBody
import elide.runtime.http.server.HttpResponseBody
import io.netty.handler.codec.http.*
import io.netty.handler.ssl.ApplicationProtocolNames
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

class ServerFullStackTest : AbstractServerStackTest() {
  private fun withTestServer(
    http: CleartextOptions? = null,
    https: HttpsOptions? = null,
    http3: Http3Options? = null,
    block: HttpApplicationStack.() -> Unit,
  ) {
    withTestServer(
      options = HttpApplicationOptions(http, https, http3),
      block = block,
    )
  }

  private fun TestClient.assertAltSvc(message: String, vararg expected: AltService) = use {
    val request = DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/test")
    val response = send(request).get()

    assertEquals(HttpResponseStatus.OK, response.status())

    val received = response.headers().getAll(HttpHeaderNames.ALT_SVC).map<String, AltService> {
      val (protocol, authority) = it.split("=")
      AltService(protocol, authority)
    }

    assertContentEquals(expected.toList(), received, message)
  }

  private fun HttpApplicationStack.addressFor(service: StackServiceContributor): SocketAddress {
    return services.first { it.label == service.label }.bindResult.getOrThrow().address
  }

  private fun HttpApplicationStack.authorityFor(
    service: StackServiceContributor,
    withHost: Boolean = false
  ): String {
    val address = addressFor(service) as InetSocketAddress
    return "${address.hostName.takeIf { withHost }.orEmpty()}:${address.port}"
  }

  @Test fun `should call application's onStart when server starts successfully`() {
    val onStartCalled = AtomicBoolean(false)
    val caughtError = AtomicReference<Throwable?>(null)

    val application = object : HttpApplication<CallContext.Empty> {
      override fun toString(): String = "LifecycleTestApplication"

      override fun newContext(
        request: HttpRequest,
        response: HttpResponse,
        requestBody: HttpRequestBody,
        responseBody: HttpResponseBody,
      ): CallContext.Empty = CallContext.Empty

      override fun handle(call: HttpCall<CallContext.Empty>) {
        call.send()
      }

      override fun onError(error: Throwable?) {
        if (error != null) caughtError.compareAndSet(null, error)
      }

      override fun onStart(stack: HttpApplicationStack) {
        onStartCalled.set(true)
      }
    }

    val options = HttpApplicationOptions(
      http = CleartextOptions(address = testAddress()),
    )

    val stack = HttpApplicationStack.bind(application, options)
    try {
      val failures = stack.services.filter { it.bindResult.isFailure }
      if (failures.isNotEmpty()) {
        val details = failures.joinToString("\n") {
          val failure = it.bindResult.exceptionOrNull()
          "[${it.label}] ${failure?.stackTraceToString() ?: "unknown failure"}"
        }
        fail("Expected services to bind successfully but found failures:\n$details")
      }

      assertTrue(onStartCalled.get(), "Expected application's onStart lifecycle method to be invoked")

      caughtError.get()?.let { error ->
        fail("Unexpected application error during stack lifecycle:\n${error.stackTraceToString()}")
      }
    } finally {
      stack.close(force = true)
      val shutdownErrors = stack.awaitClose()
      if (shutdownErrors.isNotEmpty()) {
        val details = shutdownErrors.joinToString("\n") { it.stackTraceToString() }
        fail("Server failed to shutdown cleanly:\n$details")
      }
    }
  }

  @Test fun `should advertise alt services`() {
    // cleartext h1 receives h2, h3
    withTestServer(
      http = CleartextOptions(address = testAddress()),
      https = HttpsOptions(address = testAddress(), certificate = testCertificate),
      http3 = Http3Options(address = testAddress(), certificate = testCertificate),
    ) {
      TestClients.cleartext(addressFor(HttpCleartextService)).assertAltSvc(
        message = "cleartext h1 receives h2, h3",
        AltService("h3", authorityFor(Http3Service)),
        AltService("h2", authorityFor(HttpsService)),
      )
    }

    // cleartext h1 receives h2c only when no h2, h3
    withTestServer(http = CleartextOptions(address = testAddress())) {
      TestClients.cleartext(addressFor(HttpCleartextService)).assertAltSvc(
        message = "h1 receives h2c when h2, h3 not available",
        AltService("h2c", authorityFor(HttpCleartextService)),
      )
    }

    // h2c receives h2, h3
    withTestServer(
      http = CleartextOptions(address = testAddress()),
      https = HttpsOptions(address = testAddress(), certificate = testCertificate),
      http3 = Http3Options(address = testAddress(), certificate = testCertificate),
    ) {
      TestClients.cleartextH2C(addressFor(HttpCleartextService)).assertAltSvc(
        message = "h2c receives h2, h3",
        AltService("h3", authorityFor(Http3Service)),
        AltService("h2", authorityFor(HttpsService)),
      )
    }

    // h2 receives h3
    withTestServer(
      http = CleartextOptions(address = testAddress()),
      https = HttpsOptions(address = testAddress(), certificate = testCertificate),
      http3 = Http3Options(address = testAddress(), certificate = testCertificate),
    ) {
      TestClients.tlsH2(addressFor(HttpsService), testCertificate).assertAltSvc(
        message = "h2 receives h3",
        AltService("h3", authorityFor(Http3Service)),
      )
    }

    // h3 receives no alt-svc
    withTestServer(
      http = CleartextOptions(address = testAddress()),
      https = HttpsOptions(address = testAddress(), certificate = testCertificate),
      http3 = Http3Options(address = testAddress(), certificate = testCertificate),
    ) {
      TestClients.http3(addressFor(Http3Service), testCertificate).assertAltSvc(
        message = "h3 receives nothing",
      )
    }

    // different host names are set explicitly (requires special test setup, ignored for now)
    // withNioTestServer(
    //   http = CleartextOptions(address = InetSocketAddress("127.0.0.1", 0)),
    //   https = HttpsOptions(address = InetSocketAddress("localhost", 0), certificate = testCertificate),
    //   http3 = Http3Options(address = InetSocketAddress("localhost", 0), certificate = testCertificate),
    // ) {
    //   TestClients.cleartext(addressFor(HttpCleartextService)).assertAltSvc(
    //     message = "different host names are set explicitly",
    //     AltService("h3", authorityFor(Http3Service, withHost = true)),
    //     AltService("h2", authorityFor(HttpsService, withHost = true)),
    //   )
    // }

    // domain sockets disable advertising
    withTestServer(
      http = CleartextOptions(address = testAddress(useDomainSocket = false)),
      https = HttpsOptions(address = testAddress(useDomainSocket = true), certificate = testCertificate),
      http3 = Http3Options(address = testAddress(useDomainSocket = true), certificate = testCertificate),
    ) {
      TestClients.cleartext(addressFor(HttpCleartextService)).assertAltSvc(
        message = "domain sockets disable advertising",
        AltService("h2c", authorityFor(HttpCleartextService)),
      )
    }
  }

  @DynamicTransportTest fun `should handle requests on full stack`(
    transport: ServerTransport,
    useDomainSockets: Boolean,
  ) {
    assumeTrue(transport.isAvailable())

    val options = HttpApplicationOptions(
      http = CleartextOptions(testAddress(useDomainSockets, "cleartext")),
      https = HttpsOptions(testCertificate, testAddress(useDomainSockets, "https")),
      http3 = Http3Options(testCertificate, testAddress(useDomainSocket = false)),
    )

    withTestServer(
      options = options,
      transport = transport,
      allowFailure = { false },
    ) {
      val httpAddress = services.first { it.label == HttpCleartextService.LABEL }
        .bindResult.getOrThrow().address

      val httpsAddress = services.first { it.label == HttpsService.LABEL }
        .bindResult.getOrThrow().address

      val http3Address = services.first { it.label == Http3Service.LABEL }
        .bindResult.getOrThrow().address

      // cleartext (no upgrade)
      TestClients.cleartext(httpAddress).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_0, "/cleartext/1/0")
      }
      TestClients.cleartext(httpAddress).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_1, "/cleartext/1/1")
      }

      // cleartext (H2C)
      val upgradeSucceeded = AtomicReference<Boolean?>(null)
      TestClients.cleartextH2C(
        target = httpAddress,
        onUpgrade = {
          when (it) {
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_SUCCESSFUL -> upgradeSucceeded.set(true)
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_REJECTED -> upgradeSucceeded.set(false)
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_ISSUED -> Unit // ignore, not a response
          }
        },
      ).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_0, "/h2c/1/0")

        assertNotNull(upgradeSucceeded.get(), "Expected H2C upgrade to take place")
        assertTrue(upgradeSucceeded.get()!!, "Expected H2C upgrade to succeed")
      }

      upgradeSucceeded.set(null)
      TestClients.cleartextH2C(
        target = httpAddress,
        onUpgrade = {
          when (it) {
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_SUCCESSFUL -> upgradeSucceeded.set(true)
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_REJECTED -> upgradeSucceeded.set(false)
            HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_ISSUED -> Unit // ignore, not a response
          }
        },
      ).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_1, "/h2c/1/1")

        assertNotNull(upgradeSucceeded.get(), "Expected H2C upgrade to take place")
        assertTrue(upgradeSucceeded.get()!!, "Expected H2C upgrade to succeed")
      }

      // https (no upgrade)
      TestClients.tlsH1(httpsAddress, testCertificate).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_0, "/tls/1/0")
      }
      TestClients.tlsH1(httpsAddress, testCertificate).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_1, "/tls/1/1")
      }

      // https (alpn)
      val selectedProtocol = AtomicReference<String?>()
      TestClients.tlsH2(
        target = httpsAddress,
        certificate = testCertificate,
        onProtocolSelected = { selectedProtocol.set(it) },
      ).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_0, "/alpn/1/0")

        val protocol = assertNotNull(selectedProtocol.get(), "expected a protocol to be negotiated")
        assertEquals(ApplicationProtocolNames.HTTP_2, protocol, "expected HTTP/2 to be negotiated")
      }

      selectedProtocol.set(null)
      TestClients.tlsH2(
        target = httpsAddress,
        certificate = testCertificate,
        onProtocolSelected = { selectedProtocol.set(it) },
      ).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_1, "/alpn/1/1")

        val protocol = assertNotNull(selectedProtocol.get(), "expected a protocol to be negotiated")
        assertEquals(ApplicationProtocolNames.HTTP_2, protocol, "expected HTTP/2 to be negotiated")
      }

      // http3
      TestClients.http3(http3Address, testCertificate).use { client ->
        client.assertRequestOk(HttpVersion.HTTP_1_1, "/h3")
      }
    }
  }
}
