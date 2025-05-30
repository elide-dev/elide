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

package elide.runtime.gvm.internals.ruby

import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function
import kotlinx.coroutines.test.runTest
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.core.PolyglotEngineConfiguration
import elide.runtime.gvm.internals.AbstractDualTest.Ruby
import elide.runtime.gvm.internals.AbstractIntrinsicTest
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.runtime.plugins.ruby.ruby
import elide.vm.annotations.Polyglot
import elide.runtime.plugins.ruby.Ruby as RubyPlugin

/** Specializes the [AbstractIntrinsicTest] base with support for Ruby guest testing. */
@OptIn(DelicateElideApi::class)
abstract class AbstractRubyIntrinsicTest<T : GuestIntrinsic> : AbstractIntrinsicTest<T, Ruby>() {
  override fun configureEngine(config: PolyglotEngineConfiguration) {
    config.configure(RubyPlugin)
  }

  // Logic to execute a guest-side test.
  @Suppress("SameParameterValue")
  private fun executeGuestInternal(
ctx: PolyglotContext,
 bind: Boolean,
 bindUtils: Boolean,
 op: Ruby,
): Value {
    // resolve the script
    val script = op.invoke(ctx)

    // prep intrinsic bindings under test
    val bindings = if (bind) {
      val target = HashMap<Symbol, Any>()
      provide().install(GuestIntrinsic.MutableIntrinsicBindings.Factory.wrap(target))
      target
    } else {
      emptyMap()
    }

    // install bindings under test, if directed
    val target = ctx.bindings(RubyPlugin)
    bindings.forEach {
      target.putMember(it.key.symbol, it.value)
    }

    // install utility bindings, if directed
    if (bindUtils) {
      target.putMember("test", CaptureAssertion())
    }

    // prep for execution
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // execute script
    val returnValue = try {
      ctx.enter()
      ctx.ruby(script)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  override fun dual(bind: Boolean, op: suspend () -> Unit): DualTestExecutionProxy<Ruby> {
    runTest { op.invoke() }
    return object : DualTestExecutionProxy<Ruby>() {
      override fun guest(guestOperation: Ruby) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }.doesNotFail()

      override fun thenRun(guestOperation: Ruby) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }
    }
  }

/** Assertion capture interface. */
  @FunctionalInterface internal interface RubyAssertion : TestAssertion, Function<Any?, TestContext> {
    /** Invoke a null-check-based assertion. */
    @Polyglot override fun apply(value: Any?): TestContext
  }

  /** Default top-level assertion implementation. */
  internal class CaptureAssertion : RubyAssertion {
    private val heldValue: AtomicReference<Any?> = AtomicReference(null)
    override val value: Any? get() = heldValue.get()

    @Polyglot override fun apply(value: Any?): TestContext {
      heldValue.set(value)
      return TestResultContext(this)
    }
  }
}
