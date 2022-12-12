package elide.runtime.gvm.internals

import elide.annotations.Inject
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context as VMContext
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.atomic.AtomicBoolean

/** Tests for the [NativeContextManagerImpl]. */
@TestCase internal class NativeContextManagerTest {
  // Context manager under test.
  @Inject internal lateinit var contextManager: ContextManager<VMContext, VMContext.Builder>

  @Test fun testInjectable() {
    assertNotNull(contextManager, "should be able to inject context manager instance")
  }

  @Test @Disabled fun testAcquireSuspend() = runBlocking {
    val acquired = AtomicBoolean(false)
    assertDoesNotThrow {
      contextManager {
        acquired.set(true)
      }
    }
    assertTrue(acquired.get(), "should be able to acquire safe guest VM execution context")
  }
}
