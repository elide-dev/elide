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

package elide.tool.testing

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.datetime.Clock
import elide.tool.cli.CommandContext
import elide.tool.err.ErrorHandler.ErrorUtils.buildStacktrace
import elide.tool.testing.TestEvent.Type
import elide.tool.testing.TestEvent.Type.*

/**
 * TBD.
 */
abstract class CommandTestRunner<Test, Context, Cmd>: TestCase<Test, Context>
        where Context: TestContext,
              Test: Testable<Context>,
              Cmd: CommandContext {
  // Spawn a test event.
  private fun testEvent(
    ctx: Context,
    test: Test,
    type: Type,
    result: TestResult? = null,
  ): TestEvent<Test, Context> {
    return object: TestEvent<Test, Context> {
      override val type: Type get() = type
      override val test: Test get() = test
      override val context: Context get() = ctx
      override val result: TestResult? get() = result
    }
  }

  /**
   * Run a single test.
   */
  suspend fun runTest(case: Test, listener: TestEventListener<Test, Context>): TestResult {
    // create a context
    return context().use {
      // prepare containers/timestamps
      val start = Clock.System.now()
      val err: AtomicReference<Throwable> = AtomicReference(null)
      val success = AtomicBoolean(false)
      val result: AtomicReference<TestResult> = AtomicReference(null)

      // dispatch pre-events
      listener.onTestEvent(testEvent(it, case, PRE_EXECUTE))

      try {
        listener.onTestEvent(testEvent(it, case, EXECUTE))
        case.runTestIn(it)
        listener.onTestEvent(testEvent(it, case, POST_EXECUTE))
        success.set(true)

      } catch (thr: Throwable) {
        err.set(thr)

      } finally {
        val end = Clock.System.now()

        result.set(if (success.get()) TestResult.success(
          test = case,
          testInfo = case.testInfo(),
          start = start,
          end = end,
        ) else TestResult.failure(
          test = case,
          testInfo = case.testInfo(),
          start = start,
          end = end,
          err = err.get(),
          errOutput = StringBuilder(err.get()?.buildStacktrace()),
        ))

        // the test has finished
        listener.onTestEvent(testEvent(it, case, RESULT, result.get()))
      }

      // dispatch post-events
      listener.onTestEvent(testEvent(it, case, DONE, result.get()))
      result.get()
    }
  }
}
