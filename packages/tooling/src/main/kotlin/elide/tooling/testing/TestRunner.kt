package elide.tooling.testing

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

public interface TestRunner {
  public interface TestRun {
    public val testResults: Flow<TestResult>
    public val result: Deferred<TestRunResult>
  }

  public fun runTests(tests: Sequence<TestCase>): TestRun
}
