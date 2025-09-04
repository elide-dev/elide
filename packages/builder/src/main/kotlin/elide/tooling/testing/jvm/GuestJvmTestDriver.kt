package elide.tooling.testing.jvm

import elide.runtime.core.PolyglotContext
import elide.runtime.plugins.jvm.Jvm
import elide.tooling.testing.TestDriver
import elide.tooling.testing.TestOutcome
import elide.tooling.testing.TestTypeKey

/**
 * Test driver for [JVM test cases][JvmTestCase], using a [PolyglotContext] to obtain an instance of the test class and
 * invoke the test method.
 */
public class GuestJvmTestDriver(private val contextProvider: () -> PolyglotContext) : TestDriver<JvmTestCase> {
  override val type: TestTypeKey<JvmTestCase> get() = JvmTestCase

  override suspend fun run(testCase: JvmTestCase): TestOutcome {
    val guestContext = contextProvider()
    val testClass = guestContext.bindings(Jvm.Plugin).getMember(testCase.className)
      ?: return TestOutcome.Error("Failed to resolve test class: ${testCase.className}")

    if (!testClass.canInstantiate()) return TestOutcome.Error("Cannot instantiate test class ${testCase.className}")
    val testInstance = testClass.newInstance()

    if (!testInstance.canInvokeMember(testCase.methodName))
      return TestOutcome.Error("Cannot invoke member ${testCase.methodName}")

    return runCatching { testInstance.invokeMember(testCase.methodName) }.fold(
      onSuccess = { TestOutcome.Success },
      onFailure = { TestOutcome.Failure(it) }
    )
  }
}
