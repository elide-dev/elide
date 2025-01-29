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
package elide.runtime.gvm.internals.js.node

import kotlin.test.Test
import kotlin.test.assertNotNull
import elide.annotations.Inject
import elide.runtime.node.test.NodeTestModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `test` built-in module. */
@TestCase internal class NodeTestTest : NodeModuleConformanceTest<NodeTestModule>() {
  override val moduleName: String get() = "test"
  override fun provide(): NodeTestModule = NodeTestModule()
  @Inject lateinit var test: NodeTestModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("run")
    yield("suite")
    yield("test")
    yield("describe")
    yield("it")
    yield("before")
    yield("after")
    yield("beforeEach")
    yield("afterEach")
    yield("MockFunctionContext")
    yield("MockTracker")
    yield("MockTimers")
    yield("TestsStream")
    yield("TestContext")
    yield("SuiteContext")
  }

  @Test override fun testInjectable() {
    assertNotNull(test)
  }
}
