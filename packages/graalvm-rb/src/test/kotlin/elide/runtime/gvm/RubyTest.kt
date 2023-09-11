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

package elide.runtime.gvm

import elide.annotations.Inject
import elide.runtime.gvm.internals.AbstractDualTest
import elide.runtime.gvm.internals.context.ContextManager
import elide.runtime.gvm.internals.ruby.AbstractRubyIntrinsicTest
import elide.runtime.gvm.internals.ruby.RubyRuntime
import kotlinx.coroutines.runBlocking
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Value
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Abstract base for all non-intrinsic Ruby guest execution tests. */
abstract class RubyTest : AbstractDualTest() {
  companion object {
    init {
      System.setProperty("elide.ruby.vm.enableStreams", "true")
    }
  }

  private val initialized: AtomicBoolean = AtomicBoolean(false)

  // Guest context manager.
  @Inject lateinit var contextManager: ContextManager<Context, Context.Builder>

  // Ruby runtime.
  @Inject internal lateinit var ruby: RubyRuntime

  override fun buildContext(engine: Engine, conf: (Context.Builder.() -> Unit)?): Context.Builder {
    throw UnsupportedOperationException("not supported for this test case")
  }

  override fun <V : Any> withContext(op: Context.() -> V, conf: (Context.Builder.() -> Unit)?): V {
    TODO("Not yet implemented")
  }

  // Run the provided `op` with an active (and exclusively owned) Ruby VM context.
  override fun <V: Any> withContext(op: Context.() -> V): V = runBlocking {
    if (!initialized.get()) {
      contextManager.installContextFactory {
        ruby.builder(it)
      }
      contextManager.installContextConfigurator {
        ruby.configureVM(it)
      }
      contextManager.installContextSpawn {
        ruby.spawn(it)
      }
      contextManager.activate(start = false)
      initialized.set(true)
    }
    contextManager {
      op.invoke(this)
    }
  }

  private inline fun executeGuestInternal(
    ctx: Context,
    bindUtils: Boolean,
    op: Context.() -> String,
  ): Value {
    // resolve the script
    val script = op.invoke(ctx)

    // install bindings under test, if directed
    val target = ctx.getBindings("ruby")

    // install utility bindings, if directed
    if (bindUtils) {
      target.putMember("test", AbstractRubyIntrinsicTest.CaptureAssertion())
    }

    // prep for execution
    val hasErr = AtomicBoolean(false)
    val subjectErr: AtomicReference<Throwable> = AtomicReference(null)

    // execute script
    val returnValue = try {
      ctx.enter()
      ctx.eval("ruby", script)
    } catch (err: Throwable) {
      hasErr.set(true)
      subjectErr.set(err)
      throw subjectErr.get()
    } finally {
      ctx.leave()
    }
    return returnValue
  }

  override fun executeGuest(bind: Boolean, op: Context.() -> String) = GuestTestExecution(::withContext) {
    executeGuestInternal(
      ctx = this,
      op = op,
      bindUtils = true,
    )
  }

  // Run the provided `op` on the host, and the provided `guest` via `executeGuest`.
  override fun dual(bind: Boolean, op: () -> Unit): DualTestExecutionProxy {
    op.invoke()
    return object : DualTestExecutionProxy() {
      override fun guest(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bindUtils = true,
          op = guestOperation,
        )
      }.doesNotFail()

      override fun thenRun(guestOperation: Context.() -> String) = GuestTestExecution(::withContext) {
        executeGuestInternal(
          this,
          bindUtils = true,
          op = guestOperation,
        )
      }
    }
  }
}
