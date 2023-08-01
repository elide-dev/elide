package elide.server.runtime.jvm

import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertTrue

/** Sanity tests for the [SecurityProviderConfigurator]. */
class SecurityProviderConfiguratorTest {
  @Test fun testInitSecurity() {
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
    assertTrue(
      SecurityProviderConfigurator.ready(),
      "security providers should show as `ready` after initialization",
    )
  }

  @Test fun testInitSecurityTwice() {
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
    assertDoesNotThrow {
      SecurityProviderConfigurator.initialize()
    }
  }
}
