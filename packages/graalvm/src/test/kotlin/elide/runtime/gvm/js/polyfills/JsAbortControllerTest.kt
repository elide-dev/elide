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
@file:Suppress("JSCheckFunctionSignatures", "JSUnresolvedFunction")

package elide.runtime.gvm.js.polyfills

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.*
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortController
import elide.runtime.gvm.internals.intrinsics.js.abort.AbortSignal
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.intrinsics.js.err.JsError
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.intrinsics.js.node.events.CustomEvent
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests that the `AbortController` and `AbortSignal` polyfills are available globally. */
@TestCase internal class JsAbortControllerTest : AbstractJsTest() {
  val exec = GuestExecution.direct()

  // `AbortController` type should be present globally.
  @Test fun testAbortControllerPresent() = test {
    code("""
      test(AbortController).isNotNull();
    """)
  }.doesNotFail()

  // `AbortSignal` type should be present globally.
  @Test fun testAbortSignalPresent() = test {
    code("""
      test(AbortSignal).isNotNull();
    """)
  }.doesNotFail()

  // `AbortController` can be constructed directly.
  @Test fun testAbortControllerConstructable() = test {
    code("""
      test(new AbortController()).isNotNull();
    """)
  }.doesNotFail()

  // `AbortController` should have a non-null `signal` by default.
  @Test fun testAbortControllerSignalNotNull() = test {
    code("""
      test(new AbortController()).isNotNull();
      test(new AbortController().signal).isNotNull();
    """)
  }.doesNotFail()

  // `AbortController` should have a non-null and non-aborted `signal` by default.
  @Test fun testAbortControllerSignalNotAbortedByDefault() = test {
    code("""
      test(new AbortController()).isNotNull();
      test(new AbortController().signal).isNotNull();
      test(new AbortController().signal.aborted).isEqualTo(false);
    """)
  }.doesNotFail()

  // `AbortController` constructor does not accept parameters.
  @Test fun testAbortControllerConstructorAcceptsNoParams() {
    assertThrows<TypeError> {
      AbortController.Factory.newInstance(Value.asValue(1))
    }
    test {
      code("""
      new AbortController(1)
    """)
    }.fails()
  }

  // `AbortSignal` cannot be constructed directly.
  @Test fun testAbortSignalNotConstructable() = test {
    code("""
      new AbortSignal();
    """)
  }.fails()

  @Test fun `AbortSignal factory should support aborted()`() = dual {
    val signal = assertNotNull(AbortSignal.factory(GuestExecutorProvider { exec }).abort())
    assertTrue(signal.aborted, "Signal should be pre-aborted")
  }.guest {
    // language=JavaScript
    """
      test(AbortSignal.abort().aborted === true).isEqualTo(true);
    """
  }

  @Test fun `AbortSignal factory should support any()`() {
    val orig = AbortSignal.create()
    val orig2 = AbortSignal.create()
    val orig3 = AbortSignal.create()
    val aborted = AbortSignal.aborted()
    val reasoned = AbortSignal.aborted("Because")
    assertFalse(AbortSignal.delegated(listOf(orig)).aborted, "Delegated signal should not be aborted")
    assertFalse(AbortSignal.delegated(listOf(orig, orig2)).aborted)
    assertFalse(AbortSignal.delegated(listOf(orig, orig2, orig3)).aborted)
    assertNull(AbortSignal.delegated(listOf(orig)).reason)
    assertNull(AbortSignal.delegated(listOf(orig, orig2)).reason)
    assertNull(AbortSignal.delegated(listOf(orig, orig2, orig3)).reason)
    assertTrue(AbortSignal.delegated(listOf(aborted)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, aborted)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, orig2, aborted)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, orig2, orig3, aborted)).aborted)
    assertNull(AbortSignal.delegated(listOf(aborted)).reason)
    assertNull(AbortSignal.delegated(listOf(orig, aborted)).reason)
    assertNull(AbortSignal.delegated(listOf(orig, orig2, aborted)).reason)
    assertTrue(AbortSignal.delegated(listOf(reasoned)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, reasoned)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, orig2, reasoned)).aborted)
    assertTrue(AbortSignal.delegated(listOf(orig, orig2, orig3, reasoned)).aborted)
    assertNotNull(AbortSignal.delegated(listOf(reasoned)).reason)
    assertNotNull(AbortSignal.delegated(listOf(orig, reasoned)).reason)
    assertNotNull(AbortSignal.delegated(listOf(orig, orig2, reasoned)).reason)
    assertNotNull(AbortSignal.delegated(listOf(orig, orig2, orig3, reasoned)).reason)
    assertNull(orig.reason)
    assertFalse(orig.aborted)
    assertNull(orig2.reason)
    assertFalse(orig2.aborted)
    assertNull(orig3.reason)
    assertFalse(orig3.aborted)
    assertNull(aborted.reason)
    assertTrue(aborted.aborted)
    assertTrue(reasoned.aborted)
    assertNotNull(reasoned.reason)
    val signal = assertNotNull(AbortSignal.factory(GuestExecutorProvider { exec }).abort())
    assertTrue(signal.aborted, "Signal should be pre-aborted")
    val fac = assertNotNull(AbortSignal.factory(GuestExecutorProvider { exec }))
    val delegateByFactory = assertNotNull(fac.any(listOf(orig)))
    assertFalse(delegateByFactory.aborted)
    assertNull(delegateByFactory.reason)
    val delegateByFactory2 = assertNotNull(fac.any(listOf(orig, aborted)))
    assertTrue(delegateByFactory2.aborted)
    assertNull(delegateByFactory2.reason)
    val delegateByFactory3 = assertNotNull(fac.any(listOf(orig, reasoned)))
    assertTrue(delegateByFactory3.aborted)
    assertNotNull(delegateByFactory3.reason)
  }

  @Test fun `AbortSignal - delegate should propagate abort event`() {
    val one = AbortSignal.create()
    val two = AbortSignal.create()
    val delegate = AbortSignal.delegated(listOf(one, two))
    val oneAborted = AtomicBoolean(false)
    val twoAborted = AtomicBoolean(false)
    one.addEventListener("abort") {
      oneAborted.compareAndSet(false, true)
    }
    two.addEventListener("abort") {
      twoAborted.compareAndSet(false, true)
    }
    assertFalse(oneAborted.get())
    assertFalse(twoAborted.get())
    assertFalse(delegate.aborted)
    assertFalse(one.aborted)
    assertFalse(two.aborted)
    assertNull(delegate.reason)
    assertNull(one.reason)
    assertNull(two.reason)
    // ---- abort
    delegate.assignAborted("test")
    // ---- end abort
    assertTrue(delegate.aborted)
    assertTrue(one.aborted)
    assertTrue(two.aborted)
    assertTrue(oneAborted.get())
    assertTrue(twoAborted.get())
    assertNotNull(delegate.reason)
    assertNotNull(one.reason)
    assertNotNull(two.reason)
    assertEquals(delegate.reason, one.reason)
    assertEquals(delegate.reason, two.reason)
  }

  @Test fun `AbortController should behave as a proxy object`() {
    val ctr = AbortController()
    assertFalse(ctr.hasMember("unknown"))
    assertTrue(ctr.hasMember("signal"))
    assertTrue(ctr.hasMember("abort"))
    assertFalse(ctr.memberKeys.contains("unknown"))
    assertTrue(ctr.memberKeys.contains("signal"))
    assertTrue(ctr.memberKeys.contains("abort"))
    assertIs<elide.runtime.intrinsics.js.AbortSignal>(ctr.getMember("signal"))
    assertIs<ProxyExecutable>(ctr.getMember("abort"))
    ctr.putMember("sample", Value.asValue(0))
    assertNull(ctr.getMember("sample"))
  }

  @Test fun `AbortSignal should behave as a proxy object`() {
    val signal = AbortController().signal
    assertIs<AbortSignal>(signal)
    assertFalse(signal.hasMember("unknown"))
    assertTrue(signal.hasMember("aborted"))
    assertTrue(signal.hasMember("reason"))
    assertTrue(signal.hasMember("throwIfAborted"))
    assertFalse(signal.memberKeys.contains("unknown"))
    assertTrue(signal.memberKeys.contains("aborted"))
    assertTrue(signal.memberKeys.contains("reason"))
    assertTrue(signal.memberKeys.contains("throwIfAborted"))
    signal.putMember("sample", Value.asValue(0))
    assertNull(signal.getMember("sample"))
    assertNotNull(signal.getMember("throwIfAborted"))
    assertNotNull(signal.getMember("aborted"))
  }

  @Test fun `AbortController - abort should mark underlying signal as aborted`() {
    val ctr = AbortController()
    assertFalse(ctr.signal.aborted)
    ctr.abort()
    assertTrue(ctr.signal.aborted)
    assertNull(ctr.signal.reason)
    val ctr2 = AbortController()
    assertFalse(ctr2.signal.aborted)
    ctr2.abort("Because")
    assertTrue(ctr2.signal.aborted)
    assertEquals("Because", ctr2.signal.reason)
    val ctr3 = AbortController()
    assertFalse(ctr3.signal.aborted)
    assertFalse((ctr3.signal as AbortSignal).getMember("aborted") as Boolean)
    assertNull((ctr3.signal as AbortSignal).getMember("reason"))
    ctr3.abort("Because")
    assertTrue(ctr3.signal.aborted)
    assertTrue((ctr3.signal as AbortSignal).getMember("aborted") as Boolean)
    assertEquals("Because", (ctr3.signal as AbortSignal).getMember("reason") as String)
    val ctr4 = AbortController()
    assertFalse(ctr4.signal.aborted)
    assertFalse((ctr4.signal as AbortSignal).getMember("aborted") as Boolean)
    assertNull((ctr4.signal as AbortSignal).getMember("reason"))
    val aborter = ctr4.getMember("abort")
    assertIs<ProxyExecutable>(aborter)
    assertDoesNotThrow { aborter.execute(Value.asValue("Because")) }
    assertTrue(ctr4.signal.aborted)
    assertTrue((ctr4.signal as AbortSignal).getMember("aborted") as Boolean)
    assertEquals("Because", ((ctr4.signal as AbortSignal).getMember("reason") as Value).asString())
    val ctr5 = AbortController()
    assertFalse(ctr5.signal.aborted)
    assertFalse((ctr5.signal as AbortSignal).getMember("aborted") as Boolean)
    assertNull((ctr5.signal as AbortSignal).getMember("reason"))
    val aborter2 = ctr5.getMember("abort")
    assertIs<ProxyExecutable>(aborter2)
    assertDoesNotThrow { aborter2.execute(Value.asValue("Because"), Value.asValue("Secondly")) }
    assertTrue(ctr5.signal.aborted)
    assertTrue((ctr5.signal as AbortSignal).getMember("aborted") as Boolean)
    assertEquals("Because", ((ctr5.signal as AbortSignal).getMember("reason") as Value).asString())
    val ctr6 = AbortController()
    assertFalse(ctr6.signal.aborted)
    assertFalse((ctr6.signal as AbortSignal).getMember("aborted") as Boolean)
    assertNull((ctr6.signal as AbortSignal).getMember("reason"))
    val aborter3 = ctr6.getMember("abort")
    assertIs<ProxyExecutable>(aborter3)
    assertDoesNotThrow { aborter3.execute() }
    assertTrue(ctr6.signal.aborted)
    assertTrue((ctr6.signal as AbortSignal).getMember("aborted") as Boolean)
    assertNull((ctr6.signal as AbortSignal).getMember("reason"))
  }

  @Test fun `AbortSignal - timeout should schedule abort`() {
    val fac = AbortSignal.factory(GuestExecutorProvider { exec })
    val sig = fac.timeout(500)
    assertFalse(sig.aborted)
    assertNull(sig.reason)
    exec.schedule({
      assertTrue(sig.aborted)
      assertNotNull(sig.reason)
      val reason = sig.reason
      assertIs<JsError>(reason)
      assertEquals("Timed out", reason.message)
    }, 501, MILLISECONDS).get(
      1000, MILLISECONDS,
    )
  }

  @Test fun `AbortSignal - emits 'abort' event when aborted`() {
    val ctr = AbortController()
    val sig = ctr.signal
    var called = false
    sig.addEventListener("abort") {
      called = true
    }
    assertFalse(sig.aborted)
    assertFalse(called)
    ctr.abort()
    assertTrue(sig.aborted)
    assertTrue(called)
  }

  @Test fun `AbortSignal - throwIfAborted functions as advertised`() = dual {
    val ctr = AbortController()
    val sig = ctr.signal
    assertDoesNotThrow { sig.throwIfAborted() }
    ctr.abort()
    assertTrue(sig.aborted)
    assertThrows<Throwable> { sig.throwIfAborted() }
  }.guest {
    // language=JavaScript
    """
      const ac = new AbortController();
      const signal = ac.signal;
      let didThrow = false;
      try {
        signal.throwIfAborted();
      } catch (e) {
        didThrow = true;
      }
      test(didThrow).isEqualTo(false);
      ac.abort();
      test(signal.aborted).isEqualTo(true);
      try {
        signal.throwIfAborted();
      } catch (e) {
        didThrow = true;
      }
      test(didThrow).isEqualTo(true);
    """
  }

  @Test fun `AbortSignal - throwIfAborted throws Throwable reasons`() {
    val ctr = AbortController()
    val sig = ctr.signal
    assertDoesNotThrow { sig.throwIfAborted() }
    ctr.abort(IllegalStateException("hi"))
    assertTrue(sig.aborted)
    val exc = assertThrows<IllegalStateException> { sig.throwIfAborted() }
    assertEquals("hi", exc.message)
  }

  @Test fun `AbortSignal - throwIfAborted throws primitive reasons`() = dual {
    val ctr = AbortController()
    val sig = ctr.signal
    assertDoesNotThrow { sig.throwIfAborted() }
    ctr.abort("Because")
    assertTrue(sig.aborted)
    assertThrows<Throwable> { sig.throwIfAborted() }
  }.guest {
    // language=JavaScript
    """
      const ac = new AbortController();
      const signal = ac.signal;
      let didThrow = false;
      try {
        signal.throwIfAborted();
      } catch (e) {
        didThrow = true;
      }
      test(didThrow).isEqualTo(false);
      ac.abort("Because");
      test(signal.aborted).isEqualTo(true);
      let err = null;
      try {
        signal.throwIfAborted();
      } catch (e) {
        err = e;
        didThrow = true;
      }
      test(didThrow).isEqualTo(true);
      test(err.message).isEqualTo("Because");
    """
  }

  @Test fun `AbortSignal - throwIfAborted default message is 'Aborted'`() = dual {
    val ctr = AbortController()
    val sig = ctr.signal
    assertDoesNotThrow { sig.throwIfAborted() }
    ctr.abort("Because")
    assertTrue(sig.aborted)
    assertThrows<Throwable> { sig.throwIfAborted() }
  }.guest {
    // language=JavaScript
    """
      const ac = new AbortController();
      const signal = ac.signal;
      let didThrow = false;
      try {
        signal.throwIfAborted();
      } catch (e) {
        didThrow = true;
      }
      test(didThrow).isEqualTo(false);
      ac.abort();
      test(signal.aborted).isEqualTo(true);
      let err = null;
      try {
        signal.throwIfAborted();
      } catch (e) {
        err = e;
        didThrow = true;
      }
      test(didThrow).isEqualTo(true);
      test(err.message).isEqualTo("Aborted");
    """
  }

  @Test fun `AbortSignal - throwIfAborted works with non-string types`() = dual {
    val ctr = AbortController()
    val sig = ctr.signal
    assertDoesNotThrow { sig.throwIfAborted() }
    ctr.abort("Because")
    assertTrue(sig.aborted)
    assertThrows<Throwable> { sig.throwIfAborted() }
  }.guest {
    // language=JavaScript
    """
      const ac = new AbortController();
      const signal = ac.signal;
      let didThrow = false;
      try {
        signal.throwIfAborted();
      } catch (e) {
        didThrow = true;
      }
      test(didThrow).isEqualTo(false);
      ac.abort(5);
      test(signal.aborted).isEqualTo(true);
      let err = null;
      try {
        signal.throwIfAborted();
      } catch (e) {
        err = e;
        didThrow = true;
      }
      test(didThrow).isEqualTo(true);
      test(err.message).isEqualTo("5");
    """
  }
}
