/*
 * Copyright (c) 2024-2025 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
package elide.tooling.testing

import com.google.common.graph.GraphBuilder
import com.google.common.graph.Traverser

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
    // .drop(1)

  private companion object {
    private const val ROOT = "<ROOT>"
  }
}
