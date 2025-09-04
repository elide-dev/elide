package elide.tooling.runner

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import elide.tooling.config.TestConfigurator
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestCase
import elide.tooling.testing.TestResult

/** Test runner that launches tests in parallel, with a configurable concurrency limit. */
public class ConcurrentTestRunner(
  drivers: Set<TestDriver<TestCase>>,
  override val events: TestConfigurator.TestEventController,
  private val maxConcurrency: Int? = null,
) : AbstractTestRunner(drivers) {
  override fun testFlow(tests: Sequence<TestCase>): Flow<TestResult> = flow {
    val dispatcherView = currentCoroutineContext()[CoroutineDispatcher]
      ?.let { if (maxConcurrency != null) it.limitedParallelism(maxConcurrency) else it }
      ?: error("No coroutine dispatcher found in the current context")

    withContext(dispatcherView) {
      for (test in tests) launch { emit(runTest(test)) }
    }
  }
}
