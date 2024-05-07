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
import elide.runtime.gvm.internals.node.fs.NodeFilesystemModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `fs/promises` built-in module. */
@TestCase internal class NodeFsPromisesTest : NodeModuleConformanceTest<NodeFilesystemModule>() {
  override val moduleName: String get() = "fs/promises"
  override fun provide(): NodeFilesystemModule = NodeFilesystemModule()
  @Inject lateinit var filesystem: NodeFilesystemModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("access")
    yield("appendFile")
    yield("chmod")
    yield("chown")
    yield("copyFile")
    yield("lchmod")
    yield("lchown")
    yield("link")
    yield("lstat")
    yield("mkdir")
    yield("mkdtemp")
    yield("open")
    yield("readdir")
    yield("readFile")
    yield("readlink")
    yield("realpath")
    yield("rename")
    yield("rmdir")
    yield("stat")
    yield("symlink")
    yield("truncate")
    yield("unlink")
    yield("utimes")
    yield("writeFile")
  }

  @Test override fun testInjectable() {
    assertNotNull(filesystem)
  }
}
