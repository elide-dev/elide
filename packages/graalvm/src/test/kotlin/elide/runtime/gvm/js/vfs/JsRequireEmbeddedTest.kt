@file:Suppress("JSFileReferences", "JSUnresolvedFunction", "NpmUsedModulesInstalled")

package elide.runtime.gvm.js.vfs

import elide.runtime.gvm.internals.vfs.EmbeddedGuestVFSImpl
import elide.runtime.gvm.js.AbstractJsTest
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.testing.annotations.Test
import elide.testing.annotations.TestCase
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

/** Tests for CJS and NPM-style require calls that resolve via embedded VFS I/O. */
@TestCase internal class JsRequireEmbeddedTest : AbstractJsTest() {
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
