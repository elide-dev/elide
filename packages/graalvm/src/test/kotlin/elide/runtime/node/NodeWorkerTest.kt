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
import elide.runtime.node.worker.NodeWorkerModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `worker` built-in module. */
@TestCase internal class NodeWorkerTest : NodeModuleConformanceTest<NodeWorkerModule>() {
  override val moduleName: String get() = "worker"
  override fun provide(): NodeWorkerModule = NodeWorkerModule()
  @Inject lateinit var worker: NodeWorkerModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("BroadcastChannel")
    yield("MessageChannel")
    yield("MessagePort")
    yield("Worker")
    yield("getEnvironmnetData")
    yield("isMainThread")
    yield("markAsUntransferable")
    yield("isMarkedAsUntransferable")
    yield("moveMessagePortToContext")
    yield("parentPort")
    yield("receiveMessageOnPort")
    yield("resourceLimits")
    yield("SHARE_ENV")
    yield("setEnvironmentData")
    yield("threadId")
    yield("workerData")
  }

  @Test override fun testInjectable() {
    assertNotNull(worker)
  }
}
