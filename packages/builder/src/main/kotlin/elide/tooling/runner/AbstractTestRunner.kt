package elide.tooling.runner

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.TestEventController
import elide.tooling.testing.TestCase
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestOutcome
import elide.tooling.testing.TestResult
import elide.tooling.testing.TestRunResult
import elide.tooling.testing.TestRunner
import elide.tooling.testing.TestStats
import elide.tooling.testing.TestRunner.TestRun
import elide.tooling.testing.TestTypeKey

/**
 * Base test runner implementation, allowing subclasses to schedule tests within a flow while delegating the actual
 * execution context to the consumer.
 */
public abstract class AbstractTestRunner(drivers: Set<TestDriver<TestCase>>) : TestRunner {
  /** Available test drivers associated by their type keys. */
  protected open val drivers: Map<TestTypeKey<TestCase>, TestDriver<TestCase>> = drivers.associateBy { it.type }

  /** Event controller used to emit test events during executions. */
  protected abstract val events: TestEventController

  /**
   * Process a sequence of test nodes and produce a cold flow of results. Implementations can choose to execute tests
   * sequentially or use custom scheduling logic within the collector's scope.
   */
  protected abstract fun testFlow(tests: Sequence<TestCase>): Flow<TestResult>

  /**
   * Execute a single test case, using a driver that matches its type. A [TestConfigurator.TestSeen] event is =
   * automatically emitted before execution.
   */
  protected suspend fun runTest(test: TestCase): TestResult {
    events.emit(TestConfigurator.TestSeen)

    val outcome: TestOutcome
    val duration = measureTime {
      outcome = drivers[test.type]?.run(test) ?: TestOutcome.Error("No driver found for type ${test.type}")
    }

    return TestResult(test.id, outcome, duration)
  }

  override fun runTests(tests: Sequence<TestCase>): TestRun {
    val runResult = CompletableDeferred<TestRunResult>()

    val seen = AtomicInteger(0)
    val executed = AtomicInteger(0)
    val passed = AtomicInteger(0)
    val failed = AtomicInteger(0)
    val skipped = AtomicInteger(0)
    val duration = AtomicLong(0L)

    val testFlow = testFlow(tests).onEach { result ->
      seen.incrementAndGet()
      if (result.outcome != TestOutcome.Skipped) executed.incrementAndGet()

      when (result.outcome) {
        is TestOutcome.Error,
        is TestOutcome.Failure -> {
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
        failed.get() > 0 -> TestOutcome.Failure()
        else -> TestOutcome.Success
      }

      runResult.complete(TestRunResult(outcome, stats, cause != null))
    }.catch {
      // suppress failures, they are reported through the outcome of the test run
    }

    return object : TestRun {
      override val result: Deferred<TestRunResult> = runResult
      override val testResults: Flow<TestResult> = testFlow
    }
  }
}
