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
@file:Suppress("JSFileReferences", "JSUnresolvedFunction", "NpmUsedModulesInstalled")
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.js.vfs

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.test.assertEquals
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.vfs.HostVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.HostVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.util.UUID

/** Tests for ESM-style import calls that resolve via host-backed I/O. */
@TestCase internal class JsImportHostTest : AbstractJsTest() {
  private fun tempHostFs() = HostVFS.scopedTo(
    Files.createTempDirectory("elide-vfs-${UUID.random()}").toAbsolutePath().toString(),
    writable = true,
  ) as HostVFSImpl

  /** Test: JavaScript `import` call that loads a file from the host file-system. */
  @Test fun testImportHostFs() {
    val fs = tempHostFs()
    val testPath = fs.getPath("/test.mjs")
    fs.writeStream(testPath).use { stream ->
      stream.write("export default {sample: \"Hello, ESM!\"};".toByteArray())
    }

    // read the file back to make sure it's there
    val exampleESMContents = fs.readStream(testPath).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("export default {sample: \"Hello, ESM!\"};", exampleESMContents.trim())

    withHostFs(fs) {
      // language=javascript
      """
        import testmod from "/test.mjs";
        test(testmod).isNotNull();
        test(testmod.sample).isEqualTo("Hello, ESM!");
      """
    }.doesNotFail()
  }

  /** Test: JavaScript `import` call that loads a file from the Node modules path. */
  @Test fun testImportHostFsNpmModules() {
    val fs = tempHostFs()
    fs.setCurrentWorkingDirectory(Path("/"))
    fs.createDirectory(fs.getPath("node_modules"))
    fs.createDirectory(fs.getPath("node_modules/testing"))

    val testPath = fs.getPath("node_modules/testing/test.mjs")
    val configPath = fs.getPath("node_modules/testing/package.json")
    val pkgPath = fs.getPath("package.json")

    fs.writeStream(testPath).use { stream ->
      stream.write("export default {sample: \"Hello, ESM direct!\"};".toByteArray())
    }
    fs.writeStream(configPath).use { stream ->
      stream.write("""
        {
          "name": "testing",
          "version": "1.0.0",
          "main": "test.mjs",
          "module": true
        }
      """.trimIndent().toByteArray())
    }
    fs.writeStream(pkgPath).use { stream ->
      stream.write("""
        {
          "name": "esmtest",
          "version": "1.0.0",
          "module": true,
          "dependencies": {
            "testing": "1.0.0"
          }
        }
      """.trimIndent().toByteArray())
    }

    // read the file back to make sure it's there
    val exampleESMContents = fs.readStream(testPath).bufferedReader(StandardCharsets.UTF_8).use {
      it.readText()
    }
    assertEquals("export default {sample: \"Hello, ESM direct!\"};", exampleESMContents.trim())

    withHostFs(fs) {
      // language=javascript
      """
        import testmod from "testing";
        test(testmod).isNotNull();
        test(testmod.sample).isEqualTo("Hello, ESM direct!");
      """
    }.doesNotFail()
  }
}
