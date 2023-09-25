/*
 * Copyright (c) 2023 Elide Ventures, LLC.
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

@file:Suppress("JSFileReferences", "JSUnresolvedFunction", "NpmUsedModulesInstalled")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.js.vfs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.vfs.HostVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.HostVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.util.UUID

/** Tests for CJS and NPM-style require calls that resolve via host-backed I/O. */
@TestCase @Ignore internal class JsRequireHostTest : AbstractJsTest() {
  /** @return Empty VFS instance for testing. */
  private fun tempHostFs() = HostVFS.acquire() as HostVFSImpl
  private val tempdir = Files.createTempDirectory("elide-vfs-${UUID.random()}").toAbsolutePath().toString()

  /** Test: JavaScript `require` call that loads a file from the host file-system. */
  @Test fun testRequireHostFs() {
    val fs = tempHostFs()
    val testPath = fs.getPath(tempdir, "test.js")
    fs.writeStream(testPath).use { stream ->
      stream.write("module.exports = {sample: 'Hello, CJS!'};".toByteArray())
    }

    // read the file back to make sure it's there
    val exampleCJSContents = fs.readStream(testPath).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("module.exports = {sample: 'Hello, CJS!'};", exampleCJSContents.trim())

    withVFS(fs) {
      // language=javascript
      """
        const testmod = require("./test.js");
        test(testmod).isNotNull();
        test(testmod.sample).isEqualTo("Hello, CJS!");
      """
    }.doesNotFail()
  }

  /** Test: JavaScript `require` call that loads a file from a nested directory within a host-backed file-system. */
  @Test fun testRequireHostFsNested() {
    val fs = tempHostFs()
    fs.createDirectory(fs.getPath(tempdir, "testing"))
    val testPath = fs.getPath(tempdir, "testing/test.js")
    fs.writeStream(testPath).use { stream ->
      stream.write("module.exports = {sample: 'Hello, CJS!'};".toByteArray())
    }

    // read the file back to make sure it's there
    val exampleCJSContents = fs.readStream(testPath).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("module.exports = {sample: 'Hello, CJS!'};", exampleCJSContents.trim())

    withVFS(fs) {
      // language=javascript
      """
        const testmod = require("./testing/test.js");
        test(testmod).isNotNull();
        test(testmod.sample).isEqualTo("Hello, CJS!");
      """
    }.doesNotFail()
  }

  /** Test: JavaScript `require` call that loads a file from the Node modules path. */
  @Test fun testRequireHostFsNpmModules() {
    val fs = tempHostFs()
    fs.createDirectory(fs.getPath(tempdir, "node_modules"))
    fs.createDirectory(fs.getPath(tempdir, "node_modules/testing"))

    val testPath = fs.getPath(tempdir, "node_modules/testing/test.js")
    val configPath = fs.getPath(tempdir, "node_modules/testing/package.json")

    fs.writeStream(testPath).use { stream ->
      stream.write("module.exports = {sample: 'Hello, CJS!'};".toByteArray())
    }
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "testing",
          "version": "1.0.0",
          "main": "./test.js"
        }
      """.trimIndent().toByteArray())
    }

    // read the file back to make sure it's there
    val exampleCJSContents = fs.readStream(testPath).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("module.exports = {sample: 'Hello, CJS!'};", exampleCJSContents.trim())

    withVFS(fs) {
      // language=javascript
      """
        const testmod = require("testing");
        test(testmod).isNotNull();
        test(testmod.sample).isEqualTo("Hello, CJS!");
      """
    }.doesNotFail()
  }
}
