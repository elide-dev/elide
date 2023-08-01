package elide.runtime.gvm.internals

import org.junit.jupiter.api.Assertions.assertNotNull
import elide.annotations.Inject
import elide.runtime.gvm.internals.context.ContextManager
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import org.graalvm.polyglot.Context as VMContext

/** Tests for the [NativeContextManagerImpl]. */
@TestCase internal class NativeContextManagerTest {
  // Context manager under test.
  @Inject internal lateinit var contextManager: ContextManager<VMContext, VMContext.Builder>

  @Test fun testInjectable() {
    assertNotNull(contextManager, "should be able to inject context manager instance")
  }
}
