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
@file:Suppress("JSUnresolvedReference", "JSDeprecatedSymbols", "JSCheckFunctionSignatures")

package elide.runtime.node

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import jakarta.inject.Provider
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.exec.GuestExecutorProvider
import elide.runtime.gvm.vfs.EmbeddedGuestVFS
import elide.runtime.intrinsics.js.asDeferred
import elide.runtime.intrinsics.js.node.WritableFilesystemPromiseAPI
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.path.Path
import elide.runtime.node.fs.NodeFilesystemModule
import elide.runtime.node.fs.VfsInitializerListener
import elide.runtime.node.path.NodePathsModule
import elide.testing.annotations.TestCase

/** Tests for Elide's implementation of the Node `fs/promises` built-in module. */
@TestCase internal class NodeFsPromisesTest : AbstractNodeFsTest<NodeFilesystemModule>() {
  @Inject private lateinit var path: NodePathsModule
  @Inject private lateinit var execProvider: GuestExecutorProvider

  private val filesystem: NodeFilesystemModule by lazy {
    provide()
  }

  override val moduleName: String get() = "fs/promises"
  override fun provide(): NodeFilesystemModule = NodeFilesystemModule(
    path,
    Provider {
      VfsInitializerListener().also {
        it.onVfsCreated(EmbeddedGuestVFS.empty())
      }
    },
    execProvider,
  )

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

  @Test fun `access() with text file`() = withTemp { tmp ->
    filesystem.providePromises().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertNotNull(fs.access(Path.from(samplePath)).asDeferred().await())
      }.guest {
        // language=javascript
        """
          const { access } = require("node:fs/promises");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath").then(() => {
            callbackDispatched = true;
          }, (err) => {
            callbackDispatched = true;
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `access() with text file and mode`() = withTemp { tmp ->
    filesystem.providePromises().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))
      assertNotNull(fs.access(Path.from(samplePath)).asDeferred().await())

      executeGuest {
        // language=javascript
        """
          const { access, constants } = require("node:fs/promises");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.R_OK).then(() => {
            callbackDispatched = true;
          }, (err) => {
            callbackDispatched = true;
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
        """
      }

      executeGuest {
        // language=javascript
        """
          const { access, constants } = require("node:fs/promises");
          test(access).isNotNull();
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", constants.R_OK).then(() => {
            callbackDispatched = true;
          }, (err) => {
            callbackDispatched = true;
            fileErr = err || null;
          });
          test(callbackDispatched).shouldBeTrue();
          test(fileErr).isNull();
        """
      }
    }
  }

  @Test fun `readFile() with text file`() = withTemp { tmp ->
    filesystem.providePromises().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        val data = assertNotNull(
          fs.readFile(Path.from(samplePath), ReadFileOptions(encoding = "utf8"))
            .asDeferred()
            .await(),
        )
        assertIs<String>(data)
        assertEquals("Hello, world!", data)
      }.guest {
        // language=javascript
        """
          const { readFile } = require("node:fs/promises");
          test(readFile).isNotNull();
          let fileData = null;
          const promise = readFile("$samplePath", { encoding: 'utf-8' }).then((data) => {
            fileData = data;
          });
          test(promise).isNotNull();
          test(fileData).isNotNull();
          test(typeof fileData === 'string').shouldBeTrue();
          test(fileData).isEqualTo("Hello, world!");
        """
      }
    }
  }

  @Test fun `copyFile() with valid file`() = withTemp { tmp ->
    filesystem.providePromises().let { fs ->
      val srcPath = tmp.resolve("some-file.txt").toAbsolutePath()
      val destPath = tmp.resolve("some-file-2.txt").toAbsolutePath()
      Files.writeString(srcPath, "Hello, world!", StandardCharsets.UTF_8)

      assertTrue(Files.exists(srcPath), "src file should exist before creation")
      assertFalse(Files.exists(destPath), "dest file should not exist before creation")
      assertIs<WritableFilesystemPromiseAPI>(fs)
      fs.copyFile(Path.from(srcPath), Path.from(destPath)).asDeferred().await()
      assertTrue(Files.exists(destPath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(destPath))

      Files.delete(destPath)
      assertFalse(Files.exists(destPath), "file should not exist before guest test")

      executeGuest {
        // language=javascript
        """
          const { copyFile } = require("node:fs/promises");
          test(copyFile).isNotNull("expected `copyFile` symbol");
          let copyErr = null;
          let didExecute = false;
          copyFile("$srcPath", "$destPath").then((err) => {
            copyErr = err;
            didExecute = true;
          });
          test(didExecute).shouldBeTrue();
          test(copyErr).isNull();
        """
      }.doesNotFail()

      assertTrue(Files.exists(destPath))
      assertTrue(Files.isRegularFile(destPath))
      assertEquals("Hello, world!", Files.readString(destPath))
    }
  }
}
