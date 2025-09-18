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

public interface TestRegistry {
  public val isEmpty: Boolean

  public operator fun get(key: TestNodeKey): TestNode

  public fun register(node: TestNode)

  public fun entries(): Sequence<TestNode>
}

public operator fun TestRegistry.plusAssign(node: TestNode): Unit = register(node)
public operator fun TestRegistry.iterator(): Iterator<TestNode> = entries().iterator()
public inline fun TestRegistry.forEach(block: (TestNode) -> Unit): Unit = entries().forEach(block)
