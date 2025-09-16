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
package elide.tooling.testing.jvm

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import elide.tooling.testing.*
import elide.tooling.testing.TestDriver.TestRunScope

public class JUnitTestDriver : TestDriver<JUnitTestSuite> {
  override val type: TestTypeKey<JUnitTestSuite> = JUnitTestSuite

  private fun TestIdentifier.toTestCase(root: JUnitTestSuite): TestCase {
    return JUnitDynamicTestCase(
      id = uniqueId,
      parent = parentId.getOrNull() ?: root.id,
      displayName = displayName,
    )
  }

  override suspend fun run(testCase: JUnitTestSuite, scope: TestRunScope) {
    val eventsListener = object : TestExecutionListener {
      private val timestamps = ConcurrentHashMap<String, TimeMark>()

      override fun executionStarted(testIdentifier: TestIdentifier) {
        if (testIdentifier.isContainer) return
        timestamps[testIdentifier.uniqueId] = TimeSource.Monotonic.markNow()
      }

      override fun executionSkipped(testIdentifier: TestIdentifier, reason: String) {
        if (testIdentifier.isContainer) return
        scope.emit(TestResult(testIdentifier.toTestCase(testCase), TestOutcome.Skipped, Duration.ZERO))
      }

      override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testIdentifier.isContainer) return

        val elapsed = timestamps.remove(testIdentifier.uniqueId)?.elapsedNow() ?: Duration.ZERO
        val outcome = when (testExecutionResult.status) {
          TestExecutionResult.Status.SUCCESSFUL -> TestOutcome.Success
          TestExecutionResult.Status.ABORTED -> TestOutcome.Error(testExecutionResult.throwable.getOrNull())
          TestExecutionResult.Status.FAILED -> TestOutcome.Failure(testExecutionResult.throwable.getOrNull())
        }

        scope.emit(TestResult(testIdentifier.toTestCase(testCase), outcome, elapsed))
      }
    }

    LauncherFactory.openSession().use { session ->
      val thread = Thread.currentThread()
      val threadLoader = thread.contextClassLoader

      try {
        thread.contextClassLoader = testCase.loader
        session.launcher.execute(testCase.request, eventsListener)
      } finally {
        thread.contextClassLoader = threadLoader
      }
    }
  }
}
