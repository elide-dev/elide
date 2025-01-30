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
import elide.runtime.node.inspector.NodeInspectorPromisesModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for the built-in [NodeInspectorPromisesModule]. */
@TestCase internal class NodeInspectorPromisesTest : NodeModuleConformanceTest<NodeInspectorPromisesModule>() {
  override val moduleName: String get() = "inspector/promises"
  override fun provide(): elide.runtime.node.inspector.NodeInspectorPromisesModule =
      elide.runtime.node.inspector.NodeInspectorPromisesModule()
  @Inject lateinit var inspector: elide.runtime.node.inspector.NodeInspectorPromisesModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Session")
    yield("close")
    yield("console")
    yield("open")
    yield("url")
    yield("waitForDebugger")
  }

  @Test override fun testInjectable() {
    assertNotNull(inspector)
  }
}
