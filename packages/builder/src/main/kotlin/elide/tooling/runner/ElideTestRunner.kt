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
package elide.tooling.runner

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.TestEventController
import elide.tooling.testing.*
import elide.tooling.testing.TestOutcome.Failure
import elide.tooling.testing.TestRunner.TestRun

public class ElideTestRunner(
  drivers: Set<TestDriver<TestCase>>,
  private val events: TestEventController,
  private val maxParallelTests: Int = SERIAL,
  context: CoroutineContext = Dispatchers.IO,
) : TestRunner {
  @JvmInline private value class ChannelScope(val channel: Channel<TestResult>) : TestDriver.TestRunScope {
    override fun emit(result: TestResult) {
      channel.trySend(result)
    }
  }

  /** Available test drivers associated by their type keys. */
  private val drivers: Map<TestTypeKey<TestCase>, TestDriver<TestCase>> = drivers.associateBy { it.type }

  private val runnerScope = CoroutineScope(context + SupervisorJob())

  override fun runTests(tests: Sequence<TestCase>): TestRun {
    val resultsChannel = Channel<TestResult>(Channel.UNLIMITED)
    val runResult = CompletableDeferred<TestRunResult>()
    val scope = ChannelScope(resultsChannel)

    val seen = AtomicInteger(0)
    val executed = AtomicInteger(0)
    val passed = AtomicInteger(0)
    val failed = AtomicInteger(0)
    val skipped = AtomicInteger(0)
    val duration = AtomicLong(0L)

    runnerScope.launch {
      if (maxParallelTests > SERIAL || maxParallelTests == UNLIMITED) {
        // run tests in parallel with a configurable limit
        val dispatcher = currentCoroutineContext()[CoroutineDispatcher]
          ?.let { if (maxParallelTests != UNLIMITED) it.limitedParallelism(maxParallelTests) else it }
          ?: error("Coroutine dispatcher not found, cannot execute tests")

        supervisorScope {
          for (case in tests) launch(dispatcher) { runTest(case, scope) }
        }
      } else {
        // run tests sequentially
        for (case in tests) runTest(case, scope)
      }

      resultsChannel.close()
    }

    val testResults = resultsChannel.receiveAsFlow().onEach { result ->
      seen.incrementAndGet()
      if (result.outcome != TestOutcome.Skipped) executed.incrementAndGet()

      when (result.outcome) {
        is TestOutcome.Error,
        is Failure -> {
          events.emit(TestConfigurator.TestFail)
          failed.incrementAndGet()
        }

        TestOutcome.Skipped -> {
          events.emit(TestConfigurator.TestSkip)
          skipped.incrementAndGet()
        }

        TestOutcome.Success -> {
          events.emit(TestConfigurator.TestPass)
          passed.incrementAndGet()
        }
      }
    }.onCompletion { cause ->
      val stats = TestStats(
        tests = seen.get().toUInt(),
        executions = executed.get().toUInt(),
        fails = failed.get().toUInt(),
        passes = passed.get().toUInt(),
        skips = skipped.get().toUInt(),
        duration = duration.get().milliseconds,
      )

      val outcome = when {
        cause != null -> TestOutcome.Error(cause)
        failed.get() > 0 -> Failure()
        else -> TestOutcome.Success
      }

      runResult.complete(TestRunResult(outcome, stats, cause != null))
    }

    return object : TestRun {
      override val result: Deferred<TestRunResult> = runResult
      override val testResults: Flow<TestResult> = testResults
    }
  }

  private suspend fun runTest(test: TestCase, scope: TestDriver.TestRunScope) {
    events.emit(TestConfigurator.TestSeen)

    val driver = drivers[test.type]
    if (driver == null) {
      // skip test if no driver is available
      scope.emit(TestResult(test, TestOutcome.Skipped, Duration.ZERO))
      return
    }

    val startMark = TimeSource.Monotonic.markNow()
    runCatching { driver.run(test, scope) }.onFailure { cause ->
      scope.emit(TestResult(test, TestOutcome.Error(cause), startMark.elapsedNow()))
    }
  }

  public companion object {
    public const val SERIAL: Int = 1
    public const val UNLIMITED: Int = -1
  }
}
