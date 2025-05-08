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
@file:OptIn(DelicateElideApi::class)

package elide.tooling.runner

import com.google.common.util.concurrent.Futures
import java.lang.AutoCloseable
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.guava.asDeferred
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import elide.runtime.core.DelicateElideApi
import elide.runtime.core.PolyglotContext
import elide.runtime.intrinsics.testing.Reason
import elide.runtime.intrinsics.testing.TestEntrypoint
import elide.runtime.intrinsics.testing.TestResult
import elide.runtime.intrinsics.testing.TestingRegistrar.*
import elide.tooling.config.TestConfigurator
import elide.tooling.config.TestConfigurator.TestEventController

// Provides abstract base behavior for test runner implementations.
public abstract class AbstractTestRunner (
  override val config: TestRunner.Config,
  override val executor: Executor,
  override val events: TestEventController,
  private val contextProvider: () -> PolyglotContext,
  private val timeSource: TimeSource = TimeSource.Monotonic,
) : TestRunner, AutoCloseable {
  // Lock status for the runner; flipped on close.
  private val locked = atomic(false)

  // Indicates whether tests are executing.
  private val executing = atomic(false)

  // Start time for test execution.
  private val startTime = atomic<TimeMark?>(null)

  // Keeps track of test execution results.
  @JvmRecord protected data class TestExecutionResult(
    val test: RegisteredTest,
    val scope: TestScope<*>,
    val result: TestResult,
    val timing: Duration,
  )

  // Holds state for a single test run.
  @JvmRecord protected data class TestRunRequest(
    val test: RegisteredTest,
    val entry: TestEntrypoint,
    val scope: TestScope<*>,
    val context: PolyglotContext,
  )

  /**
   * Stats describing a test run.
   *
   * @property tests Total number of tests seen.
   * @property executions Total number of tests ran.
   * @property passes Total number of tests that passed.
   * @property fails Total number of tests that failed.
   * @property skips Total number of tests that were skipped.
   * @property duration Total duration of the test run.
   */
  @JvmRecord public data class TestStats(
    public val tests: UInt,
    public val executions: UInt,
    public val passes: UInt,
    public val fails: UInt,
    public val skips: UInt,
    public val duration: Duration,
  )

  /**
   * Results of an individual test case run.
   *
   * @property scope The scope in which the test was a member.
   * @property case The test case that was executed.
   * @property result The result of the test case execution.
   * @property duration The duration of the test case execution.
   */
  @JvmRecord public data class TestCaseResult(
    public val scope: TestScope<*>,
    public val case: RegisteredTest,
    public val result: TestResult,
    public val duration: Duration,
  )

  /**
   * Results of a test run.
   *
   * @property result The overall result of the test run.
   * @property exitCode The exit code of the test run.
   * @property stats Statistics about the test run.
   * @property results Results of individual test cases.
   * @property earlyExit Whether the test run exited early (e.g. due to a failure during `failFast` mode).
   */
  @JvmRecord public data class TestRunResult(
    public val result: TestResult,
    public val exitCode: UInt,
    public val stats: TestStats,
    public val results: List<TestCaseResult>,
    public val earlyExit: Boolean = false,
  )

  // Running count of all seen tests.
  private val tests = atomic(0u)

  // Running count of ran tests.
  private val executions = atomic(0u)

  // Running count of passed tests seen by this runner.
  private val passes = atomic(0u)

  // Running count of failed tests seen by this runner.
  private val fails = atomic(0u)

  // Running count of skipped tests seen by this runner.
  private val skips = atomic(0u)

  // Spawned jobs.
  private val jobs = ConcurrentLinkedQueue<Deferred<TestExecutionResult>>()

  // Completion latch. Counted down once when all tests are seen/spawned; again when all tests settle to results.
  private val completion = CountDownLatch(2)

  override suspend fun CoroutineScope.accept(flow: Flow<Pair<TestScope<*>, RegisteredTest>>, final: Boolean) {
    require(!locked.value) { "Test runner is shutting down and cannot accept more tests" }
    flow.flowOn(coroutineContext).filter {
      // filter discovered tests by runner predicate
      when (val predicate = config.testPredicate) {
        null -> true
        else -> predicate.test(it.second)
      }
    }.onStart {
      if (!executing.value) {
        startTime.compareAndSet(expect = null, update = timeSource.markNow())
        executing.compareAndSet(expect = false, update = true)
      }
    }.onCompletion {
      if (final) {
        completion.countDown()
      }
    }.collect { (scope, test) ->
      // obtain the context to use for this test, then use it to resolve the entrypoint
      val ctx = contextProvider.invoke()
      val entry = resolve(ctx, test)

      // evaluate the test's eligibility to run; if it can't run, it is skipped.
      when (val reason = runnable(ctx, test)) {
        // when no reason is provided to skip the test, we can proceed to run it.
        null -> try {
          ctx.enter()
          runTest(TestRunRequest(
            test = test,
            entry = entry,
            scope = scope,
            context = ctx,
          ))
        } finally {
          ctx.leave()
        }

        else -> Futures.immediateFuture(TestExecutionResult(
          test = test,
          scope = scope,
          result = skip(reason),
          timing = Duration.ZERO,
        )).asDeferred().also {
          testSkipped(test, reason)
        }
      }.also { result ->
        // enqueue the result for handling
        jobs.offer(result)
      }
    }
  }

  override suspend fun awaitSettled(): TestRunResult {
    // if we are not in fail-fast mode, we need to wait for all tests to finish. @TODO fail fast
    jobs.awaitAll()

    // completion/conclusion of testing.
    completion.countDown()
    executing.compareAndSet(expect = true, update = false)

    // assemble finalized results.
    val testResults = jobs.map { it.await() }
    val testsFailed = testResults.any { it.result is TestResult.Fail }
    val execTime = requireNotNull(startTime.value).elapsedNow()

    return TestRunResult(
      result = if (testsFailed) fail() else pass(),
      exitCode = if (testsFailed) 1u else 0u,
      results = testResults.map {
        TestCaseResult(
          scope = it.scope,
          case = it.test,
          result = it.result,
          duration = it.timing,
        )
      },
      stats = TestStats(
        tests = tests.value,
        executions = executions.value,
        passes = passes.value,
        fails = fails.value,
        skips = skips.value,
        duration = execTime,
      ),
    )
  }

  override fun close() {
    locked.compareAndSet(expect = false, update = true)
  }

  // Internal event and accounting handler for a seen test.
  @Suppress("UNUSED_PARAMETER") protected fun testSeen(req: TestRunRequest) {
    tests.update { it + 1u }
  }

  // Internal event and accounting handler for a test under execution.
  @Suppress("UNUSED_PARAMETER") protected fun testExec(req: TestRunRequest) {
    executions.update { it + 1u }
  }

  // Internal event and accounting handler for a test which succeeded.
  @Suppress("UNUSED_PARAMETER") protected fun testSucceeded(req: TestRunRequest) {
    passes.update { it + 1u }
    events.emit(TestConfigurator.TestPass, req.test)
  }

  // Internal event and accounting handler for a test which failed.
  @Suppress("UNUSED_PARAMETER") protected fun testFailed(req: TestRunRequest, err: Throwable?) {
    fails.update { it + 1u }
    events.emit(TestConfigurator.TestFail, req.test to err)
  }

  // Internal event and accounting handler for a test which was skipped.
  @Suppress("UNUSED_PARAMETER") protected fun testSkipped(test: RegisteredTest, reason: Reason) {
    skips.update { it + 1u }
    events.emit(TestConfigurator.TestSkip, test to reason)
  }

  /**
   * Create a [TestResult] indicating a passed test.
   *
   * @return A [TestResult.Pass] instance.
   */
  protected fun pass(): TestResult = TestResult.Pass

  /**
   * Create a [TestResult] indicating a failed test.
   *
   * @param err Optional error to attach to the failure.
   * @return A [TestResult.Fail] instance.
   */
  protected fun fail(err: Throwable? = null): TestResult = TestResult.Fail(err)

  /**
   * Create a [TestResult] indicating a skipped test.
   *
   * @param reason Reason for skipping the test.
   * @return A [TestResult.Skip] instance.
   */
  protected fun skip(reason: Reason): TestResult = TestResult.Skip(reason)

  /**
   * Resolve the test entrypoint for a given test.
   *
   * For some contexts and languages, this is as simple as resolving the factory available on the registered test, which
   * may already have access to the entrypoint. For more complex circumstances (JVM), the outer test class may need to
   * be instantiated on-demand, and then the entrypoint resolved from that instance.
   *
   * @param ctx Context to resolve the test entrypoint within.
   * @param test Test to resolve.
   * @return Executable test entrypoint.
   */
  protected open fun resolve(ctx: PolyglotContext, test: RegisteredTest): TestEntrypoint {
    return test.entryFactory.invoke(ctx)
  }

  /**
   * Check assumptions and runnable state of this test.
   *
   * Some tests are not eligible to execute on some platforms, or may only be eligible to execute in other specific
   * circumstances; this method is responsible for resolving any such assumptions visible to the test runner, and either
   * allowing the test to run by returning `null`, or providing a [Reason] the test should be skipped.
   *
   * @param ctx Context to check the test entrypoint within.
   * @param test Test to check.
   * @return Reason not to run the test; if returned, the test should be skipped. Otherwise `null` is returned,
   *   indicating the test should be run.
   */
  protected open fun runnable(ctx: PolyglotContext, test: RegisteredTest): Reason? = null

  /**
   * ## Run a Test
   *
   * This method is responsible for executing the provided test within the provided `context`; for the duration of the
   * test, this method has exclusive access to the `context`. The test's resolved `scope` is provided as well; if no
   * scope is explicitly set for the test, the global scope is provided.
   *
   * The runner implementation is expected to call `async` at the beginning of this method's implementation. The job
   * launched by the method should be lazy, and tied to the current [executor]. Upon job conclusion, a
   * [TestExecutionResult] must be provided, which can be resolved via utility functions [pass], [fail], and [skip].
   *
   * ### Test Exclusion/Eligibility
   *
   * Note: Implementors of this method must check the runnable state of a test with [runnable] before execution; if a
   * reason is returned not to run the test, the test should be skipped.
   *
   * @receiver Coroutine scope that should launch this job.
   * @param request Information specifying the test to run and related context.
   * @return Deferred test execution result (an async job within the receiving scope).
   */
  protected abstract fun CoroutineScope.runTest(request: TestRunRequest): Deferred<TestExecutionResult>
}
