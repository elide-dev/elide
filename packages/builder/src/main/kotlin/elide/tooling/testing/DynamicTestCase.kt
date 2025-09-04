package elide.tooling.testing

import elide.runtime.core.PolyglotContext
import elide.runtime.intrinsics.testing.TestEntrypoint

/**
 * A test case registered by guest-side code, e.g. by using intrinsics. The [entrypointProvider] function can be used
 * to obtain an executable value encapsulating the guest test code.
 */
public class DynamicTestCase(
  override val id: TestNodeKey,
  override val parent: TestNodeKey?,
  override val displayName: String,
  public val entrypointProvider: (PolyglotContext) -> TestEntrypoint,
) : TestCase {
  override val type: TestTypeKey<DynamicTestCase> get() = DynamicTestCase

  public companion object : TestTypeKey<DynamicTestCase>
}
