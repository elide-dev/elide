/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

package elide.runtime.gvm.internals

import elide.annotations.Inject
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.python.PythonRuntime
import elide.runtime.intrinsics.GuestIntrinsic
import elide.runtime.intrinsics.Symbol
import elide.vm.annotations.Polyglot
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Function

/** Specializes the [AbstractIntrinsicTest] base with support for Python guest testing. */
abstract class AbstractPythonIntrinsicTest<T : GuestIntrinsic> : AbstractIntrinsicTest<T>() {
  /** Assertion capture interface. */
  @FunctionalInterface internal interface PythonAssertion : TestAssertion, Function<Any?, TestContext> {
    /** Invoke a null-check-based assertion. */
    @Polyglot override fun apply(value: Any?): TestContext
  }

  /** Default top-level assertion implementation. */
  internal class CaptureAssertion : PythonAssertion {
    private val heldValue: AtomicReference<Any?> = AtomicReference(null)
    override val value: Any? get() = heldValue.get()

    @Polyglot override fun apply(value: Any?): TestContext {
      heldValue.set(value)
      return TestResultContext(this)
    }
  }

  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Guest context manager.
  @Inject lateinit var contextManager: ContextManager<Context, Context.Builder>

  // Python runtime.
  @Inject internal lateinit var python: PythonRuntime

  // Run the provided `op` with an active (and exclusively owned) Python VM context.
  override fun <V: Any> withContext(op: Context.() -> V): V = runBlocking {
    if (!initialized.get()) {
      contextManager.installContextFactory {
        python.builder(it)
      }
      contextManager.installContextConfigurator {
        python.configureVM(it)
      }
      contextManager.installContextSpawn {
        python.spawn(it)
      }
      contextManager.activate(start = false)
      initialized.set(true)
    }
    contextManager {
      op.invoke(this)
    }
  }

  // Logic to execute a guest-side test.
  private inline fun executeGuestInternal(
    ctx: Context,
    bind: Boolean,
    bindUtils: Boolean,
    op: Context.() -> String,
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
    val target = ctx.getBindings("python")
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
      ctx.eval("python", script)
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
  override fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy {
    op.invoke()
    return object : DualTestExecutionProxy() {
      override fun guest(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }.doesNotFail()

      override fun thenRun(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bind,
          bindUtils = true,
          guestOperation,
        )
      }
    }
  }
}
