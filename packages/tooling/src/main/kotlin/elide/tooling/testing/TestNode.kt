package elide.tooling.testing

/** A locally-unique identifier for a node in the test tree. */
public typealias TestNodeKey = String

public interface TestTypeKey<T : TestCase>

/**
 * A node in the test tree, representing either a group of tests (suite), or a test case.
 *
 * Implementations are expected to provide ecosystem-specific information that can be used to resolve the appropriate
 * entrypoints for test execution, as well as any relevant context and configuration.
 */
public sealed interface TestNode {
  /** Key for this node in the test tree. */
  public val id: TestNodeKey

  /** Key for this node's parent, if it exists. */
  public val parent: TestNodeKey?

  /** Readable name displayed for this node in user-facing components. */
  public val displayName: String
}

/** Represents a group of test nodes, which can include other groups. */
public open class TestGroup(
  override val id: TestNodeKey,
  override val parent: TestNodeKey?,
  override val displayName: String
) : TestNode

/**
 * Describes a test case that can be instantiated and executed.
 *
 * Test cases have an associated [type] key, which can be used to identify compatible test drivers capable of executing
 * a specific implementation.
 */
public interface TestCase : TestNode {
  public val type: TestTypeKey<out TestCase>
}

