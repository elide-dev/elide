@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedFunction")

package elide.runtime.gvm.js.polyfills

import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests that the `AbortController` and `AbortSignal` polyfills are available globally. */
@TestCase internal class JsAbortControllerTest : AbstractJsTest() {
  // `AbortController`` type should be present globally.
  @Test fun testAbortControllerPresent() = executeGuest {
    // language=javascript
    """
      test(AbortController).isNotNull();
    """
  }.doesNotFail()

  // `AbortSignal`` type should be present globally.
  @Test fun testAbortSignalPresent() = executeGuest {
    // language=javascript
    """
      test(AbortSignal).isNotNull();
    """
  }.doesNotFail()
}
