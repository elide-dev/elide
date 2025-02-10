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
package elide.runtime.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.cluster.NodeClusterModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `cluster` built-in module. */
@TestCase internal class NodeClusterTest : NodeModuleConformanceTest<NodeClusterModule>() {
  override val moduleName: String get() = "cluster"
  override fun provide(): NodeClusterModule = NodeClusterModule()
  @Inject lateinit var cluster: NodeClusterModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Worker")
    yield("disconnect")
    yield("fork")
    yield("isMaster")
    yield("isPrimary")
    yield("isWorker")
    yield("schedulingPolicy")
    yield("settings")
    yield("setupMaster")
    yield("setupPrimary")
    yield("worker")
    yield("workers")
  }

  @Test override fun testInjectable() {
    assertNotNull(cluster)
  }
}
