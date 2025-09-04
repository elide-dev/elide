package elide.tooling.testing

import com.google.common.graph.GraphBuilder
import com.google.common.graph.Traverser
import org.eclipse.aether.util.graph.visitor.PathRecordingDependencyVisitor

@Suppress("UnstableApiUsage")
public class MutableTestRegistry : TestRegistry {
  private val index = mutableMapOf<TestNodeKey, TestNode>()

  private val graph = GraphBuilder.directed()
    .allowsSelfLoops(false)
    .build<TestNodeKey>()

  init {
    graph.addNode(ROOT)
  }

  override val isEmpty: Boolean
    get() = index.isEmpty()

  override fun get(key: TestNodeKey): TestNode {
    return index[key] ?: error("No test node with key <$key> in the registry")
  }

  override fun register(node: TestNode) {
    require(node.id != ROOT) { "Test node key '$ROOT' is reserved for internal use" }
    require(index.putIfAbsent(node.id, node) == null) { "Test node with key '${node.id}' is already registered'" }

    graph.addNode(node.id)
    graph.putEdge(node.parent ?: ROOT, node.id)
  }

  override fun entries(): Sequence<TestNode> = Traverser
    .forTree<String> { graph.successors(it) }
    .breadthFirst(ROOT)
    .asSequence()
    .mapNotNull { index[it] }
    .drop(1)

  private companion object {
    private const val ROOT = "<ROOT>"
  }
}
