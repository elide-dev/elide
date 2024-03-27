package elide.embedded

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmbeddedRuntimeTest {
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
}