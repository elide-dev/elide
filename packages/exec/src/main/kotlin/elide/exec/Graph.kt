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

package elide.exec

import kotlinx.serialization.Serializable

/**
 *
 */
@Serializable
public sealed interface Graph<ID : NodeId, Root : Node, Node> {
  /**
   *
   */
  public val root: Root

  /**
   * Count of nodes held by this graph.
   */
  public val nodeCount: UInt

  /**
   * Count of edges held by this graph.
   */
  public val edgeCount: UInt

  /**
   *
   */
  public fun asSequence(): Sequence<Node>

  /**
   *
   */
  public operator fun contains(node: Node): Boolean

  /**
   *
   */
  public operator fun contains(id: ID): Boolean

  /**
   *
   */
  public operator fun get(id: ID): Node & Any

  /**
   *
   */
  public fun findByName(id: ID): Node?

  /**
   *
   */
  public sealed interface MutableGraph<ID : NodeId, Root : Node, Node> : Graph<ID, Root, Node> {
    /**
     *
     */
    public fun add(node: Node)

    /**
     *
     */
    public fun edge(from: Node, to: Node)

    /**
     *
     */
    public operator fun plus(node: Node) {
      add(node)
    }

    /**
     *
     */
    public operator fun plus(node: Pair<Node, Node>) {
      edge(node.first, node.second)
    }

    /**
     *
     */
    public operator fun plusAssign(node: Node) {
      add(node)
    }

    /**
     *
     */
    public infix fun Node.dependsOn(other: Node)
  }
}
