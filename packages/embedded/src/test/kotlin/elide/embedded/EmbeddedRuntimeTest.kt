package elide.embedded

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import tools.elide.call.callMetadata
import tools.elide.call.v1alpha1.fetchRequest
import tools.elide.call.v1alpha1.unaryInvocationRequest
import tools.elide.http.HttpMethod
import tools.elide.http.httpHeader
import tools.elide.http.httpHeaders
import tools.elide.http.httpRequest
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlinx.coroutines.future.await
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createParentDirectories
import kotlin.io.path.writeText
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import elide.embedded.EmbeddedAppConfiguration.EmbeddedDispatchMode
import elide.runtime.Logging

class EmbeddedRuntimeTest {
  private val logging by lazy { Logging.of(EmbeddedRuntimeTest::class) }

  /** Temporary root for generated guest applications. */
  @TempDir lateinit var testGuestRoot: Path

  @Test fun `should return false for duplicate initialization`() {
    val runtime = ElideEmbedded()
    val config = EmbeddedConfiguration(
      protocolVersion = EmbeddedProtocolVersion.V1_0,
      protocolFormat = EmbeddedProtocolFormat.PROTOBUF,
      guestRoot = testGuestRoot,
      guestLanguages = emptySet(),
    )

    assertTrue(runtime.initialize(config), "expected initialization to succeed")

    assertDoesNotThrow("expected duplicate initialization not to throw") {
      assertFalse(runtime.initialize(config), "expected duplicate initialization to fail")
    }
  }

  @Test fun `should dispatch call`() = runTest(timeout = 1.minutes) {
    val runtime = ElideEmbedded()
    val config = EmbeddedConfiguration(
      protocolVersion = EmbeddedProtocolVersion.V1_0,
      protocolFormat = EmbeddedProtocolFormat.PROTOBUF,
      guestRoot = testGuestRoot,
      guestLanguages = setOf(EmbeddedGuestLanguage.JAVA_SCRIPT),
    )

    runtime.initialize(config)
    runtime.start()

    val app = runtime.createApp(
      "test-app",
      EmbeddedAppConfiguration(
        entrypoint = "index.js",
        language = EmbeddedGuestLanguage.JAVA_SCRIPT,
        dispatchMode = EmbeddedDispatchMode.FETCH,
      ),
    )

    val entrypoint = testGuestRoot.resolve("test-app").resolve("index.js")
    entrypoint.createParentDirectories()
    entrypoint.writeText(
      """
      function fetch(request) {
        const response = new Response();

        response.statusCode = 418;
        response.statusMessage = "I'm a teapot 🫖";
        response.headers["server"] = ["chai"];

        return response;
      }

      module.exports = { fetch }
      """.trimIndent(),
    )

    runtime.startApp(app).await()

    // allocate stub request
    val requestMessage = unaryInvocationRequest {
      fetch = fetchRequest {
        metadata = callMetadata {
          appId = "test-app"
          requestId = "test-request"
        }

        request = httpRequest {
          standard = HttpMethod.GET
          path = "/hello/world"
          query = "from=me"
          headers = httpHeaders {
            httpHeader {
              name = "user-agent"
              value = "junit"
            }
          }
        }
      }
    }

    val requestBytes = ByteBuffer.wrap(requestMessage.toByteArray())
    val result = runtime.dispatch(requestBytes, app).await()

    logging.info("Response: ${result.statusCode} ${result.statusMessage}")
  }
}