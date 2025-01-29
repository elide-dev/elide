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
import elide.runtime.node.console.NodeConsoleModule
import elide.runtime.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `console` built-in module. */
@TestCase internal class NodeConsoleTest : NodeModuleConformanceTest<NodeConsoleModule>() {
  override val moduleName: String get() = "console"
  override fun provide(): NodeConsoleModule = NodeConsoleModule()
  @Inject lateinit var console: NodeConsoleModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("Console")
    yield("assert")
    yield("clear")
    yield("count")
    yield("countReset")
    yield("debug")
    yield("dir")
    yield("dirxml")
    yield("error")
    yield("group")
    yield("groupCollapsed")
    yield("groupEnd")
    yield("info")
    yield("log")
    yield("table")
    yield("time")
    yield("timeEnd")
    yield("timeLog")
    yield("trace")
    yield("warn")
  }

  @Test override fun testInjectable() {
    assertNotNull(console)
  }
}
