/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.runtime.intrinsics.js

import org.graalvm.polyglot.Value
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.gvm.js.AbstractJsTest
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@OptIn(ExperimentalCoroutinesApi::class, DelicateElideApi::class)
@TestCase internal class JsPromiseTest : AbstractJsTest() {
  inline fun PolyglotContext.jsBindings(block: Value.() -> Unit) {
    bindings(elide.runtime.plugins.js.JavaScript).apply(block)
  }

  @Test fun `promise should allow manual completion and rejection from host`() {
    assertDoesNotThrow { JsPromise<Int>().resolve(1) }
    assertDoesNotThrow { JsPromise<Int>().reject("test") }
  }

  @Test fun `promise should reflect closed state`() {
    val promise = JsPromise<Int>()
    assertFalse(promise.isDone, "expected promise to not be closed")

    promise.resolve(1)
    assertTrue(promise.isDone, "expected promise to be closed")
  }

  @Test fun `promise should invoke callbacks on completion`() {
    val success = JsPromise<Int>()
    val failure = JsPromise<Int>()

    val hostSuccess = CompletableDeferred<Int>()
    val hostFailure = CompletableDeferred<Int>()

    success.then(
      onFulfilled = hostSuccess::complete,
      onCatch = { hostSuccess.completeExceptionally(PromiseRejectedException(it)) },
    )

    failure.then(
      onFulfilled = { fail("expected promise to be rejected") },
      onCatch = { hostFailure.completeExceptionally(PromiseRejectedException(it)) },
    )

    executeESM {
      jsBindings {
        putMember("testSuccess", success)
        putMember("testFailure", failure)
      }

      """
      export const result = {}
      testSuccess.then(
        value => result.success = value,
        () => result.success = -1,
      );
      testFailure.then(
        value => result.failure = false,
        () => result.failure = true,
      );
      """
    }.thenAssert {
      val guestHandle = it.returnValue()?.getMember("result")
      assertNotNull(guestHandle, "guest snippet should return a value")

      success.resolve(42)

      assertEquals(42, hostSuccess.getCompleted(), "expected host callback to be invoked")
      assertEquals(42, guestHandle.getMember("success").asInt(), "expected guest success callback to be called")

      failure.reject("test rejection")

      assertTrue(hostFailure.isCancelled, "expected host rejection callback to be invoked")
      assertTrue(guestHandle.getMember("failure").asBoolean(), "expected guest rejection callback to be called")
    }
  }

  @Test fun `closed promise should invoke callbacks on registration`() {
    val success = JsPromise.resolved(42)
    val failure = JsPromise.rejected<Int>("test rejection")

    var hostSuccess = false
    success.then(
      onFulfilled = {
        assertEquals(42, it, "expected resolved value to match constructor")
        hostSuccess = true
      },
      onCatch = { fail("expected promise to be successful") },
    )
    assertTrue(hostSuccess, "expected host resolve callback to be invoked")

    var hostFailure = false
    failure.then(
      onFulfilled = { fail("expected promise to be rejected") },
      onCatch = { hostFailure = true },
    )
    assertTrue(hostFailure, "expected host failure callback to be invoked")

    executeESM {
      jsBindings {
        putMember("testSuccess", success)
        putMember("testFailure", failure)
      }

      """
      export const result = {}
      testSuccess.then(
        value => result.success = value,
        () => result.success = -1,
      );
      testFailure.then(
        value => result.failure = false,
        () => result.failure = true,
      );
      """
    }.thenAssert {
      val guestHandle = it.returnValue()?.getMember("result")
      assertNotNull(guestHandle, "guest snippet should return a value")

      assertEquals(42, guestHandle.getMember("success").asInt(), "expected guest success callback to be called")
      assertTrue(guestHandle.getMember("failure").asBoolean(), "expected guest rejection callback to be called")
    }
  }

  @Test fun `promise should not allow multiple completion calls`() {
    val success = JsPromise<Int>()
    assertTrue(success.resolve(42))
    assertFalse(success.resolve(420))
    success.then(onFulfilled = { assertEquals(42, it, "expected resolved value to first resolution") })

    val failure = JsPromise<Int>()
    assertTrue(failure.reject("reason"))
    assertFalse(failure.reject("another reason"))
    failure.catch { assertEquals("reason", it, "expected rejection cause to match first call") }
  }
}
