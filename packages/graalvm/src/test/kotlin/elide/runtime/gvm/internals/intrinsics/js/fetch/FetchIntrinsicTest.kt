package elide.runtime.gvm.internals.intrinsics.js.fetch

import elide.annotations.Inject
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import org.junit.jupiter.api.Assertions.*

/** Tests for the Fetch API intrinsics provided by Elide. */
@TestCase internal class FetchIntrinsicTest : AbstractJsIntrinsicTest<FetchIntrinsic>() {
  // Injected fetch intrinsic under test.
  @Inject internal lateinit var fetch: FetchIntrinsic

  override fun provide(): FetchIntrinsic = fetch

  @Test override fun testInjectable() {
    assertNotNull(fetch, "should be able to inject fetch intrinsic instance")
  }
}
