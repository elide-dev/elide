@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedFunction")

package elide.runtime.gvm.js.polyfills

import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests that the `Buffer` polyfill is available globally. */
@TestCase internal class JsBufferTest : AbstractJsTest() {
  // Buffer type should be present globally.
  @Test fun testBufferPresent() = executeGuest {
    // language=javascript
    """
      test(Buffer).isNotNull();
    """
  }.doesNotFail()
}
