/*
 *  Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 *  Licensed under the MIT license (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *    https://opensource.org/license/mit/
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under the License.
 */

package elide.runtime.gvm.python

import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.plugins.python.python
import kotlinx.coroutines.test.runTest

abstract class PythonTest : AbstractDualTest<AbstractDualTest.Python>() {
  private fun executeGuestInternal(context: PolyglotContext, op: Python): PolyglotValue {
    val script = op(context)
    val result = runCatching {
      context.enter()
      context.python(script)
    }

    context.leave()
    return result.getOrThrow()
  }

  override fun executeGuest(bind: Boolean, op: Python): AbstractDualTest<Python>.GuestTestExecution {
    return GuestTestExecution(::withContext) {
      executeGuestInternal(this, op)
    }
  }

  override fun dual(bind: Boolean, op: suspend () -> Unit): AbstractDualTest<Python>.DualTestExecutionProxy<Python> {
      runTest { op.invoke() }
    return object : DualTestExecutionProxy<Python>() {
      override fun guest(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(this, guestOperation)
      }.doesNotFail()

      override fun thenRun(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(this, guestOperation)
      }
    }
  }
}
