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
@file:OptIn(DelicateElideApi::class)

package elide.runtime.gvm.internals.js.node

import org.junit.jupiter.api.assertDoesNotThrow
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*
import elide.annotations.Inject
import elide.runtime.core.DelicateElideApi
import elide.runtime.gvm.internals.node.fs.NodeFilesystemModule
import elide.runtime.gvm.js.node.NodeModuleConformanceTest
import elide.runtime.intrinsics.js.node.WritableFilesystemAPI
import elide.runtime.intrinsics.js.node.fs.ReadFileOptions
import elide.runtime.intrinsics.js.node.fs.WriteFileOptions
import elide.testing.annotations.TestCase
import elide.runtime.intrinsics.js.node.path.Path as NodePath

/** Tests for Elide's implementation of the Node `fs` built-in module. */
@TestCase internal class NodeFsTest : NodeModuleConformanceTest<NodeFilesystemModule>() {
  @Inject lateinit var filesystem: NodeFilesystemModule

  override val moduleName: String get() = "fs"
  override fun provide(): NodeFilesystemModule = filesystem

  private fun withTemp(op: (Path) -> Unit) {
    val temp = Files.createTempDirectory(
      Path.of(System.getProperty("java.io.tmpdir")),
      "elide-test-"
    )
    val fileOf = temp.toFile()
    var didError = false
    try {
      op(temp)
    } catch (ioe: Throwable) {
      didError = true
      throw ioe
    } finally {
      if (!didError) fileOf.deleteRecursively()
    }
  }

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

  @Test fun `exists() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val missingPath = tmp.resolve("some-missing-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.exists(NodePath.from(samplePath)) {
          assertTrue(it)
        }
        val existed = assertDoesNotThrow {
          val doesExist = AtomicReference<Boolean>(null)
          fs.exists(NodePath.from(missingPath)) {
            doesExist.set(it)
          }
          doesExist.get()
        }
        assertFalse(existed)
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { exists } = require("node:fs");
          ok(exists);
          let callbackDispatched = false;
          let doesExist = null;

          exists("$samplePath", (does) => {
            callbackDispatched = true;
            doesExist = does;
          });
          equal(callbackDispatched, true);
          equal(doesExist, true);
        """
      }
    }
  }

  @Test fun `existsSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val missingPath = tmp.resolve("some-missing-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertTrue(
          assertDoesNotThrow { fs.existsSync(NodePath.from(samplePath)) }
        )
        assertFalse(
          assertDoesNotThrow { fs.existsSync(NodePath.from(missingPath)) }
        )
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { existsSync } = require("node:fs");
          ok(existsSync);
          ok(existsSync("$samplePath"));
          equal(existsSync("$samplePath"), true);
          ok(!existsSync("$missingPath"));
          equal(existsSync("$missingPath"), false);
        """
      }
    }
  }

  @Test fun `exists() with directory`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      Files.createDirectory(samplePath)
      assertTrue(Files.exists(samplePath), "should have written directory")
      assertTrue(Files.isDirectory(samplePath))

      dual {
        fs.exists(NodePath.from(samplePath)) {
          assertTrue(it)
        }
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { exists } = require("node:fs");
          ok(exists);
          let callbackDispatched = false;
          let doesExist = null;

          exists("$samplePath", (does) => {
            callbackDispatched = true;
            doesExist = does;
          });
          equal(callbackDispatched, true);
          equal(doesExist, true);
        """
      }
    }
  }

  @Test fun `existsSync() with directory`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      Files.createDirectory(samplePath)
      assertTrue(Files.exists(samplePath), "should have written directory")
      assertTrue(Files.isDirectory(samplePath))

      dual {
        assertTrue(
          assertDoesNotThrow { fs.existsSync(NodePath.from(samplePath)) }
        )
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { existsSync } = require("node:fs");
          ok(existsSync);
          equal(existsSync("$samplePath"), true);
        """
      }
    }
  }

  @Test fun `access() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.access(NodePath.from(samplePath)) { err ->
          assertNull(err)
        }
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { access } = require("node:fs");
          ok(access);
          let callbackDispatched = false;
          let fileErr = null;
          access("$samplePath", (err) => {
            fileErr = err || null;
          });
          equal(callbackDispatched, true);
          equal(fileErr, null);
        """
      }
    }
  }

  @Test fun `accessSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertDoesNotThrow { fs.accessSync(NodePath.from(samplePath)) }
      }.guest {
        // language=javascript
        """
          const { ok, doesNotThrow } = require("node:assert");
          const { accessSync } = require("node:fs");
          ok(accessSync);
          doesNotThrow(() => accessSync("$samplePath"));
        """
      }
    }
  }

  @Test fun `readFile() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        fs.readFile(NodePath.from(samplePath), ReadFileOptions(encoding = "utf8")) { err, data ->
          assertNull(err)
          assertNotNull(data)
          assertIs<String>(data)
          assertEquals("Hello, world!", data)
        }
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { readFile } = require("node:fs");
          ok(readFile);
          let callbackDispatched = false;
          let fileErr = null;
          let fileData = null;

          readFile("$samplePath", { encoding: 'utf-8' }, (err, data) => {
            callbackDispatched = true;
            fileErr = null;
            fileData = data;
          });
          equal(callbackDispatched, true);
          equal(fileErr, null);
          ok(typeof fileData === 'string');
          equal(fileData, "Hello, world!");
        """
      }
    }
  }

  @Test fun `readFileSync() with text file`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      Files.write(samplePath, "Hello, world!".toByteArray())
      assertTrue(Files.exists(samplePath), "should have written file")
      assertEquals("Hello, world!", Files.readString(samplePath))

      dual {
        assertEquals(
          "Hello, world!",
          assertDoesNotThrow { fs.readFileSync(NodePath.from(samplePath), ReadFileOptions(encoding = "utf8")) }
        )
      }.guest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { readFileSync } = require("node:fs");
          ok(readFileSync);
          ok(readFileSync("$samplePath", { encoding: 'utf-8' }));
          equal(readFileSync("$samplePath", { encoding: 'utf-8' }), "Hello, world!");
        """
      }
    }
  }

  @Test fun `mkdir() with valid directory name`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-dir2").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "directory should not exist before creation")
      assertFalse(Files.exists(samplePath2), "directory should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.mkdir(NodePath.from(samplePath)) {
        assertNull(it)
        assertTrue(Files.exists(samplePath), "directory should exist after creation")
      }
      assertTrue(Files.exists(samplePath))
      assertTrue(Files.isDirectory(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { mkdir } = require("node:fs");
          ok(mkdir);
          let created = false;
          let mkdirError = null;
          mkdir("$samplePath2", (err) => {
            created = !err;
            mkdirError = err ? err : mkdirError;
          });
          equal(created, true);
          equal(mkdirError, null);
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isDirectory(samplePath2))
    }
  }

  @Test fun `mkdirSync() with valid directory name`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-dir").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-dir2").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "directory should not exist before creation")
      assertFalse(Files.exists(samplePath2), "directory should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      assertEquals(
        samplePath.toString(),
        assertNotNull(assertDoesNotThrow { fs.mkdirSync(NodePath.from(samplePath)) }),
      )
      assertTrue(Files.exists(samplePath))
      assertTrue(Files.isDirectory(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { mkdirSync } = require("node:fs");
          ok(mkdirSync);
          equal(mkdirSync("$samplePath2"), "$samplePath2");
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isDirectory(samplePath2))
    }
  }

  @Test fun `writeFile() with valid file and string as data`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!") { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { writeFile } = require("node:fs");
          ok(writeFile);
          let writeErr = null;
          writeFile("$samplePath2", "Hello, world!", (err) => {
            writeErr = err || null;
          });
          equal(writeErr, null);
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFile() with valid file and host bytearray`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!".toByteArray(StandardCharsets.UTF_8)) { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
    }
  }

  @Test fun `writeFile() with valid file and string as string`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFile(NodePath.from(samplePath), "Hello, world!", WriteFileOptions(encoding = "utf-8")) { err ->
        assertNull(err)
      }
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok, equal } = require("node:assert");
          const { writeFile } = require("node:fs");
          ok(writeFile);
          let writeErr = null;
          writeFile("$samplePath2", "Hello, world!", { encoding: 'utf-8' }, (err) => {
            writeErr = err || null;
          });
          equal(writeErr, null);
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFileSync() with valid file and string as data`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      assertDoesNotThrow {
        fs.writeFileSync(NodePath.from(samplePath), "Hello, world!")
      }

      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok } = require("node:assert");
          const { writeFileSync } = require("node:fs");
          ok(writeFileSync);
          writeFileSync("$samplePath2", "Hello, world!")
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }

  @Test fun `writeFileSync() with valid file and host bytearray`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)
      fs.writeFileSync(NodePath.from(samplePath), "Hello, world!".toByteArray(StandardCharsets.UTF_8))
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))
    }
  }

  @Test fun `writeFileSync() with valid file and string as string`() = withTemp { tmp ->
    filesystem.provideStd().let { fs ->
      // write a file
      val samplePath = tmp.resolve("some-file.txt").toAbsolutePath()
      val samplePath2 = tmp.resolve("some-file2.txt").toAbsolutePath()
      assertFalse(Files.exists(samplePath), "file should not exist before creation")
      assertFalse(Files.exists(samplePath2), "file should not exist before creation")
      assertIs<WritableFilesystemAPI>(fs)

      fs.writeFileSync(NodePath.from(samplePath), "Hello, world!", WriteFileOptions(encoding = "utf-8"))
      assertTrue(Files.exists(samplePath), "file should exist after creation")
      assertEquals("Hello, world!", Files.readString(samplePath))

      executeGuest {
        // language=javascript
        """
          const { ok } = require("node:assert");
          const { writeFileSync } = require("node:fs");
          ok(writeFileSync);
          writeFileSync("$samplePath2", "Hello, world!", { encoding: 'utf-8' });
        """
      }.doesNotFail()

      assertTrue(Files.exists(samplePath2))
      assertTrue(Files.isRegularFile(samplePath2))
      assertEquals("Hello, world!", Files.readString(samplePath2))
    }
  }
}
