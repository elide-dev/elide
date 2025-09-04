package elide.tooling.testing

import kotlin.time.Duration

public data class TestResult(
  val test: TestNodeKey,
  val outcome: TestOutcome,
  val duration: Duration,
)

public sealed interface TestOutcome {
  public data object Skipped : TestOutcome
  public data object Success : TestOutcome
  @JvmInline public value class Failure(public val reason: Any? = null) : TestOutcome
  @JvmInline public value class Error(public val reason: Any? = null) : TestOutcome
}
