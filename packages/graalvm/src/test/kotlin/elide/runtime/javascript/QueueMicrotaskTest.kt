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
package elide.runtime.javascript

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyExecutable
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertTrue
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.exec.GuestExecution
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.internals.js.AbstractJsIntrinsicTest
import elide.runtime.gvm.js.undefined
import elide.runtime.intrinsics.js.err.TypeError
import elide.runtime.plugins.js.javascript
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

@TestCase internal class QueueMicrotaskTest : AbstractJsIntrinsicTest<QueueMicrotaskCallable>() {
  @Inject lateinit var queueMicrotask: QueueMicrotaskCallable
  override fun provide(): QueueMicrotaskCallable = queueMicrotask

  @Test override fun testInjectable() {
    assertNotNull(queueMicrotask)
  }

  @Test fun testExecMicrotask() {
    val exec = GuestExecution.direct()
    val prov = GuestExecutorProvider { exec }
    val fresh = QueueMicrotaskCallable(prov)
    var didExec = false
    val invocable = { didExec = true }
    assertDoesNotThrow { fresh.invoke(invocable) }
    assertTrue(didExec)
  }

  @Test fun testExecMicrotaskGuest() = dual {
    val exec = GuestExecution.direct()
    val prov = GuestExecutorProvider { exec }
    val fresh = QueueMicrotaskCallable(prov)
    var didExec = false
    val invocable = { didExec = true }
    assertDoesNotThrow { fresh.invoke(invocable) }
    assertTrue(didExec)
  }.guest {
    // language=JavaScript
    """
      let didExec = false;
      queueMicrotask(() => didExec = true);
      test(didExec).isEqualTo(true);
    """
  }

  @OptIn(DelicateElideApi::class)
  @Test fun testExecMicrotaskGuestDirect() {
    val exec = GuestExecution.direct()
    val prov = GuestExecutorProvider { exec }
    val fresh = QueueMicrotaskCallable(prov)
    val guestFn = withContext {
      javascript(
        // language=JavaScript
        """
          const fn = (() => {
            // hello
          });
          fn;
        """
      )
    }

    assertNotNull(guestFn)
    assertDoesNotThrow { fresh.execute(guestFn) }
  }

  @Test fun testExecMicrotaskRejectsNulls() {
    assertThrows<TypeError> { queueMicrotask.execute(Value.asValue(null)) }
  }

  @Test fun testExecMicrotaskRejectsNonExecutable() {
    assertThrows<TypeError> { queueMicrotask.execute(Value.asValue(5)) }
  }

  @Test fun testExecMicrotaskRejectsNoArgs() {
    assertThrows<TypeError> { queueMicrotask.execute() }
  }
}
