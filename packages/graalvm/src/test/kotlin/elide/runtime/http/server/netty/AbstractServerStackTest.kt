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

import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.support.ParameterDeclarations
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.UnixDomainSocketAddress
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlin.io.path.writeText
import elide.runtime.Logging
import elide.runtime.http.server.*

abstract class AbstractServerStackTest {
  @TempDir lateinit var tempDir: Path
  lateinit var domainSocket: Path
  lateinit var testCertificate: CertificateSource.File

  @BeforeEach fun setup() {
    // cleaned up automatically after each test when tempDir is deleted
    domainSocket = tempDir.resolve("test.sock")

    // prepare a default self-signed bundle and write it to a file
    val sslCertificate = tempDir.resolve("test-cert.pem")
    val sslPrivateKey = tempDir.resolve("test-key.pem")

    val bundle = CertificateSource.SelfSigned().buildSelfSignedBundle()
    sslCertificate.writeText(bundle.certificatePEM)
    sslPrivateKey.writeText(bundle.privateKeyPEM)

    testCertificate = CertificateSource.File(sslCertificate, sslPrivateKey)
  }

  /** Returns an address (optionally corresponding to a domain socket) that can be used to bind a test server. */
  protected fun testAddress(useDomainSocket: Boolean = false, label: String? = null): SocketAddress {
    if (!useDomainSocket) return InetSocketAddress("localhost", 0)

    val path = if (label == null) domainSocket
    else domainSocket.resolveSibling("test-$label.sock")

    return UnixDomainSocketAddress.of(path)
  }

  /**
   * Start a server stack with the given [options] and transport override, execute [block], and the shut down the
   * server. The test will fail before [block] if any of the services fail to start, or afterward if server shutdown
   * is not clean.
   */
  protected fun withTestServer(
    options: HttpApplicationOptions,
    transport: HttpServerTransport? = null,
    allowFailure: (Throwable) -> Boolean = { false },
    block: HttpApplicationStack.() -> Unit
  ) {
    val caughtErrors = CopyOnWriteArrayList<Throwable>()
    val testApplication = object : HttpApplication<CallContext.Empty> {
      override fun toString(): String = "TestApplication"
      override fun newContext(
        request: HttpRequest,
        response: HttpResponse,
        requestBody: ReadableContentStream,
        responseBody: WritableContentStream
      ) = CallContext.Empty

      override fun handle(call: HttpCall<CallContext.Empty>) {
        log.debug("Observed request {} {}", call.request.method(), call.request.uri())
        call.send()
      }

      override fun onError(error: Throwable?) {
        if (error != null && !allowFailure(error)) caughtErrors.add(error)
      }
    }

    val server = HttpApplicationStack.bind(testApplication, options, transport)
    log.info("Test server stack started")
    server.services.forEach { service ->
      val message = service.bindResult.fold(
        onSuccess = { "︎✅ Test service '${service.label}' now listening at ${it.assembleUri()}" },
        onFailure = { "❌ Test service '${service.label}' failed with: $it" },
      )
      log.info(message)
    }
    val failures = server.services.filter { it.bindResult.isFailure }
    if (failures.isNotEmpty()) fail {
      val message = failures.joinToString("\n") {
        " - [${it.label}] ${it.bindResult.exceptionOrNull()?.stackTraceToString()}"
      }

      "Some test services failed to start:\n$message"
    }

    try {
      block(server)
    } finally {
      // wait for shutdown
      server.close(force = true)
      val errors = server.awaitClose()
      if (errors.isNotEmpty()) fail { "Test server failed to shutdown properly:\n${errors.joinToString("\n")}" }
      if (caughtErrors.isNotEmpty()) fail { "Test server failed during use:\n${caughtErrors.joinToString("\n")}" }
    }
  }

  /** Returns the bound address of the single service in this stack. */
  protected fun HttpApplicationStack.singleBoundAddress(): SocketAddress {
    return services.single().bindResult.getOrThrow().address
  }

  companion object {
    private val log = Logging.of(AbstractServerStackTest::class)
  }
}

@ParameterizedTest(name = "{0}, domain sockets={1}")
@ArgumentsSource(ServerTestMatrix::class)
@Timeout(value = 15, unit = TimeUnit.SECONDS)
annotation class DynamicTransportTest
class ServerTestMatrix : ArgumentsProvider {
  override fun provideArguments(
    parameters: ParameterDeclarations?,
    context: ExtensionContext?
  ): Stream<out Arguments?>? {
    val builder = Stream.builder<Arguments>()

    for (transport in HttpServerTransport.all) {
      // test each transport with both regular and domain sockets
      builder.add(Arguments.of(transport, true))
      builder.add(Arguments.of(transport, false))
    }

    return builder.build()
  }
}
