@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedFunction")

package elide.runtime.gvm.js.polyfills

import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests that the `Event` polyfill is available globally. */
@TestCase internal class JsEventTest : AbstractJsTest() {
  // `EventTarget` type should be present globally.
  @Test fun testEventTargetPresent() = executeGuest {
    // language=javascript
    """
      test(EventTarget).isNotNull();
    """
  }.doesNotFail()
}
