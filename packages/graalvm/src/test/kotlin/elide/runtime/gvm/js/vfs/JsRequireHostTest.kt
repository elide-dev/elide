@file:Suppress("JSFileReferences", "JSUnresolvedFunction", "NpmUsedModulesInstalled")

package elide.runtime.gvm.js.vfs

import elide.runtime.gvm.internals.vfs.HostVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.HostVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import elide.util.UUID
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.test.assertEquals

/** Tests for CJS and NPM-style require calls that resolve via host-backed I/O. */
@TestCase internal class JsRequireHostTest : AbstractJsTest() {
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
