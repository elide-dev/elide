/*
 * Copyright (c) 2024 Elide Technologies, Inc.
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
import elide.runtime.gvm.internals.node.NodeStdlib
import elide.runtime.gvm.internals.node.childProcess.NodeChildProcessModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.ChildProcessAPI
import elide.testing.annotations.TestCase

/** Testing for Node's built-in `child_process` module. */
@TestCase internal class NodeChildProcessTest : NodeModuleConformanceTest<NodeChildProcessModule>() {
  @Inject internal lateinit var childProcess: ChildProcessAPI
  @Inject internal lateinit var module: NodeChildProcessModule

  override fun provide(): NodeChildProcessModule = module
  override val moduleName: String get() = "child_process"

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("ChildProcess")
    yield("exec")
    yield("execSync")
    yield("execFile")
    yield("execFileSync")
    yield("fork")
    yield("spawn")
    yield("spawnSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(childProcess, "should be able to inject host-side `child_process` module")
    assertNotNull(NodeStdlib.childProcess, "should be able to obtain `child_process` via `NodeStdlib`")
  }
}
