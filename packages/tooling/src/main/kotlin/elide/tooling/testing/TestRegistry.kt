package elide.tooling.testing

public interface TestRegistry {
  public val isEmpty: Boolean

  public operator fun get(key: TestNodeKey): TestNode

  public fun register(node: TestNode)

  public fun entries(): Sequence<TestNode>
}

public operator fun TestRegistry.plusAssign(node: TestNode): Unit = register(node)
public operator fun TestRegistry.iterator(): Iterator<TestNode> = entries().iterator()
public inline fun TestRegistry.forEach(block: (TestNode) -> Unit): Unit = entries().forEach(block)
