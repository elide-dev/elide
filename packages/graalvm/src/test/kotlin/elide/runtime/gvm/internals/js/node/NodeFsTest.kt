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

/** Tests for Elide's implementation of the Node `fs` built-in module. */
@TestCase internal class NodeFsTest : NodeModuleConformanceTest<NodeFilesystemModule>() {
  override val moduleName: String get() = "fs"
  override fun provide(): NodeFilesystemModule = NodeFilesystemModule()
  @Inject lateinit var filesystem: NodeFilesystemModule

  // @TODO(sgammon): Not yet fully supported
  override fun expectCompliance(): Boolean = false

  override fun requiredMembers(): Sequence<String> = sequence {
    yield("constants")
    yield("FileHandle")
    yield("Dir")
    yield("Dirent")
    yield("ReadStream")
    yield("WriteStream")
    yield("Stats")
    yield("StatFs")
    yield("FSWatcher")
    yield("StatWatcher")
    yield("access")
    yield("accessSync")
    yield("appendFile")
    yield("appendFileSync")
    yield("chmod")
    yield("chmodSync")
    yield("chown")
    yield("chownSync")
    yield("close")
    yield("closeSync")
    yield("copyFile")
    yield("copyFileSync")
    yield("createReadStream")
    yield("createWriteStream")
    yield("exists")
    yield("existsSync")
    yield("fchmod")
    yield("fchmodSync")
    yield("fchown")
    yield("fchownSync")
    yield("fdatasync")
    yield("fdatasyncSync")
    yield("fstat")
    yield("fstatSync")
    yield("fsync")
    yield("fsyncSync")
    yield("ftruncate")
    yield("ftruncateSync")
    yield("futimes")
    yield("futimesSync")
    yield("glob")
    yield("globSync")
    yield("lchmod")
    yield("lchmodSync")
    yield("lchown")
    yield("lchownSync")
    yield("luntimes")
    yield("luntimesSync")
    yield("link")
    yield("linkSync")
    yield("lstat")
    yield("lstatSync")
    yield("mkdir")
    yield("mkdirSync")
    yield("mkdtemp")
    yield("mkdtempSync")
    yield("open")
    yield("openSync")
    yield("openAsBlob")
    yield("openAsBlobSync")
    yield("opendir")
    yield("opendirSync")
    yield("read")
    yield("readSync")
    yield("readdir")
    yield("readdirSync")
    yield("readFile")
    yield("readFileSync")
    yield("readlink")
    yield("readlinkSync")
    yield("realpath")
    yield("realpathSync")
    yield("rename")
    yield("renameSync")
    yield("rmdir")
    yield("rmdirSync")
    yield("rm")
    yield("rmSync")
    yield("stat")
    yield("statSync")
    yield("symlink")
    yield("symlinkSync")
    yield("truncate")
    yield("truncateSync")
    yield("unlink")
    yield("unlinkSync")
    yield("unwatchFile")
    yield("utimes")
    yield("utimesSync")
    yield("watch")
    yield("watchFile")
    yield("write")
    yield("writeSync")
    yield("writeFile")
    yield("writeFileSync")
    yield("writev")
    yield("writevSync")
  }

  @Test override fun testInjectable() {
    assertNotNull(filesystem, "should be able to inject host-side filesystem module")
  }
}
