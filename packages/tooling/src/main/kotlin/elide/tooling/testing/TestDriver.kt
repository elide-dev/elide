package elide.tooling.testing

/**
 * Handles execution logic for a specific type of test case. Test drivers use the data from the test instance to
 * resolve an entrypoint and invoke it.
 */
public interface TestDriver<T : TestCase> {
  /** The type of test cases supported by this driver. */
  public val type: TestTypeKey<T>

  /**
   * Execute a single [testCase] and return its outcome. Exceptions thrown within this method should always be captured
   * and encapsulated by the return value instead.
   */
  public suspend fun run(testCase: T): TestOutcome
}
