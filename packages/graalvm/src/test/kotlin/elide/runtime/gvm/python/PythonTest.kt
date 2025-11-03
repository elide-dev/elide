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

import kotlinx.coroutines.test.runTest
import elide.annotations.Inject
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.core.PolyglotValue
import elide.runtime.gvm.GraalVMGuest
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.gvm.internals.IntrinsicsManager
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.python.python
import elide.runtime.plugins.python.Python.Plugin as PythonPlugin

abstract class PythonTest : AbstractDualTest<AbstractDualTest.Python>() {
  @Inject lateinit var defaultIntrinsicsManager: IntrinsicsManager

  private fun executeGuestInternal(
    context: PolyglotContext,
    bind: Boolean,
    op: Python
  ): PolyglotValue {
    val script = op(context)

    if (bind) {
      val intrinsicsMap = mutableMapOf<Symbol, Any>()
      val bindings = GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(intrinsicsMap)

      defaultIntrinsicsManager.resolver().resolve(GraalVMGuest.PYTHON).forEach { intrinsic ->
        intrinsic.install(bindings)
      }

      val target = context.bindings(PythonPlugin)
      bindings.forEach { target.putMember(it.key.symbol, it.value) }
    }

    val result = runCatching {
      context.enter()
      context.python(script)
    }

    context.leave()
    return result.getOrThrow()
  }

  override fun configureEngine(config: PolyglotEngineConfiguration) {
    config.configure(PythonPlugin)
  }

  override fun executeGuest(bind: Boolean, op: Python): AbstractDualTest<Python>.GuestTestExecution {
    return GuestTestExecution(::withContext) {
      executeGuestInternal(this, bind, op)
    }
  }

  override fun dual(bind: Boolean, op: suspend () -> Unit): AbstractDualTest<Python>.DualTestExecutionProxy<Python> {
    runTest { op.invoke() }
    return object : DualTestExecutionProxy<Python>() {
      override fun guest(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(this, bind, guestOperation)
      }.doesNotFail()

      override fun thenRun(guestOperation: Python) = GuestTestExecution(::withContext) {
        executeGuestInternal(this, bind, guestOperation)
      }
    }
  }
}
