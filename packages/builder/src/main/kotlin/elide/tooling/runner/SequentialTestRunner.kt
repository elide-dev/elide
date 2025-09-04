package elide.tooling.runner

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import elide.tooling.config.TestConfigurator
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestCase
import elide.tooling.testing.TestResult

/** Basic test runner that runs all tests sequentially in the order they are provided. */
public class SequentialTestRunner(
  drivers: Set<TestDriver<TestCase>>,
  override val events: TestConfigurator.TestEventController,
) : AbstractTestRunner(drivers) {
  override fun testFlow(tests: Sequence<TestCase>): Flow<TestResult> = tests.asFlow().map { test ->
    runTest(test)
  }
}
