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
import elide.runtime.node.diagnostics.NodeDiagnosticsChannelModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `diagnostics_channel` built-in module. */
@TestCase internal class NodeDiagnosticsChannelTest : NodeModuleConformanceTest<NodeDiagnosticsChannelModule>() {
  override val moduleName: String get() = "diagnostics_channel"
  override fun provide(): NodeDiagnosticsChannelModule = NodeDiagnosticsChannelModule()
  @Inject lateinit var diag: NodeDiagnosticsChannelModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("hasSubscribers")
    yield("channel")
    yield("subscribe")
    yield("unsubscribe")
    yield("tracingChannel")
    yield("Channel")
    yield("TracingChannel")
  }

  @Test override fun testInjectable() {
    assertNotNull(diag)
  }
}
