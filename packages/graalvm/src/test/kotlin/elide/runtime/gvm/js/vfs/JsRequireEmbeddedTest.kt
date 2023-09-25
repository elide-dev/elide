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
import kotlin.test.Ignore
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase

/** Tests for CJS and NPM-style require calls that resolve via embedded VFS I/O. */
@TestCase @Ignore internal class JsRequireEmbeddedTest : AbstractJsTest() {
  /** @return Empty VFS instance for testing. */
  private fun emptyEmbeddedFs() = EmbeddedGuestVFS.writable() as EmbeddedGuestVFSImpl

  /** Test: JavaScript `require` call that loads a file from an embedded guest file-system. */
  @Test fun testRequireEmbeddedFs() {
    val fs = emptyEmbeddedFs()
    val testPath = fs.getPath("test.js")
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

  /** Test: JavaScript `require` call that loads a file from a nested directory within an embedded guest file-system. */
  @Test fun testRequireEmbeddedFsNested() {
    val fs = emptyEmbeddedFs()
    fs.createDirectory(fs.getPath("testing"))
    val testPath = fs.getPath("testing", "test.js")
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
  @Test fun testRequireEmbeddedFsNpmModules() {
    val fs = emptyEmbeddedFs()
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/testing"))

    val testPath = fs.getPath("node_modules/testing/test.js")
    val configPath = fs.getPath("node_modules/testing/package.json")

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
