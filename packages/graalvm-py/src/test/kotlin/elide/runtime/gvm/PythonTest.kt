/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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

package elide.runtime.gvm

import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.test.runTest
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.gvm.internals.AbstractPythonIntrinsicTest
import elide.runtime.plugins.python.Python
import elide.runtime.plugins.python.python

/** Abstract base for all non-intrinsic Python guest execution tests. */
@OptIn(DelicateElideApi::class)
abstract class PythonTest : AbstractDualTest<AbstractDualTest.Python>() {
  override fun configureEngine(config: PolyglotEngineConfiguration) {
    config.install(Python)
  }

  @Suppress("SameParameterValue")
  private fun executeGuestInternal(ctx: PolyglotContext, bindUtils: Boolean, op: Python): Value {
    // resolve the script
    val script = op.invoke(ctx)

    // install bindings under test, if directed
    val target = ctx.bindings(Python)

    // install utility bindings, if directed
    if (bindUtils) {
      target.putMember("test", AbstractPythonIntrinsicTest.CaptureAssertion())
    }

    // prep for execution
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // execute script
    val returnValue = try {
      ctx.enter()
      ctx.python(script)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  override fun executeGuest(bind: Boolean, op: Python) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      ctx = this,
      op = op,
      bindUtils = true,
    )
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  override fun dual(bind: Boolean, op: suspend () -> Unit): DualTestExecutionProxy<Python> {
    runTest { op.invoke() }
    return object : DualTestExecutionProxy<Python>() {
      override fun guest(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bindUtils = true,
          op = guestOperation,
        )
      }.doesNotFail()

      override fun thenRun(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bindUtils = true,
          op = guestOperation,
        )
      }
    }
  }
}
