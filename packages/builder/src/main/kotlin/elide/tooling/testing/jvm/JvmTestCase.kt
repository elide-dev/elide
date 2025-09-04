package elide.tooling.testing.jvm

import elide.tooling.testing.TestCase
import elide.tooling.testing.TestNodeKey
import elide.tooling.testing.TestTypeKey

/**
 * Represents a JVM test, characterized by its qualified [className] and a [methodName] within it. Drivers are expected
 * to handle the specifics of loading the class and creating test instances for execution.
 */
public data class JvmTestCase(
    override val id: TestNodeKey,
    override val parent: TestNodeKey?,
    override val displayName: String,
    val className: String,
    val methodName: String,
) : TestCase {
  override val type: TestTypeKey<JvmTestCase> get() = JvmTestCase

  public companion object : TestTypeKey<JvmTestCase>
}
