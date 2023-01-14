@file:Suppress("JSUnresolvedVariable", "JSUnresolvedFunction", "JSCheckFunctionSignatures", "ExceptionCaughtLocallyJS")

package elide.runtime.intrinsics.js

import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Test cases for the JS error bridge. */
@TestCase internal class JsErrorTest : AbstractJsTest() {
  @Test fun testErrorBridgePresence() {
    executeGuest {
      // language=javascript
      """
        test(__errBridge).isNotNull();
      """
    }.doesNotFail()
  }

  @Test fun testTypeError() {
    executeGuest {
      // language=javascript
      """
        let out;
        try {
          __errBridge.typeError("a type error occurred");
        } catch (err) {
          out = err;
        }
        test(out).isNotNull();
        test(out instanceof TypeError).shouldBeTrue();
        test(out.message).isEqualTo("a type error occurred");
      """
    }.doesNotFail()
  }

  @Test fun testTypeErrorMethodPresence() {
    executeGuest {
      // language=javascript
      """
        test(__errBridge).isNotNull();
        test(!!__errBridge.typeError).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun testTypeErrorConstructor() {
    executeGuest {
      // language=javascript
      """
        let out;
        try {
          throw new TypeError("a type error occurred");
        } catch (err) {
          out = err;
        }
        test(out).isNotNull();
        test(out instanceof TypeError).shouldBeTrue();
        test(out.message).isEqualTo("a type error occurred");
      """
    }.doesNotFail()
  }

  @Test fun testTypeErrorPresence() {
    executeGuest {
      // language=javascript
      """
        test(TypeError).isNotNull();
      """
    }.doesNotFail()
  }

  @Test fun testValueError() {
    executeGuest {
      // language=javascript
      """
        let out;
        try {
          __errBridge.valueError("a value error occurred");
        } catch (err) {
          out = err;
        }
        test(out).isNotNull();
        test(out instanceof ValueError).shouldBeTrue();
        test(out.message).isEqualTo("a value error occurred");
      """
    }.doesNotFail()
  }

  @Test fun testValueErrorMethodPresence() {
    executeGuest {
      // language=javascript
      """
        test(__errBridge).isNotNull();
        test(!!__errBridge.valueError).shouldBeTrue();
      """
    }.doesNotFail()
  }

  @Test fun testValueErrorConstructor() {
    executeGuest {
      // language=javascript
      """
        let out;
        try {
          throw new ValueError("a value error occurred");
        } catch (err) {
          out = err;
        }
        test(out).isNotNull();
        test(out instanceof ValueError).shouldBeTrue();
        test(out.message).isEqualTo("a value error occurred");
      """
    }.doesNotFail()
  }

  @Test fun testValueErrorPresence() {
    executeGuest {
      // language=javascript
      """
        test(ValueError).isNotNull();
      """
    }.doesNotFail()
  }
}
